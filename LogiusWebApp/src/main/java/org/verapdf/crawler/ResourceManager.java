package org.verapdf.crawler;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.verapdf.crawler.configurations.BingConfiguration;
import org.verapdf.crawler.configurations.ReportsConfiguration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.core.email.SendEmail;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.services.*;
import org.verapdf.crawler.core.reports.ReportsGenerator;
import org.verapdf.crawler.core.validation.PDFValidator;
import org.verapdf.crawler.db.*;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.core.validation.VeraPDFValidator;
import org.verapdf.crawler.health.*;
import org.verapdf.crawler.resources.*;
import org.verapdf.crawler.tools.AbstractService;

import java.util.*;

public class ResourceManager {

    private static final String NAME_VALIDATION_SERVICE = "validationService";
    private static final String NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE = "monitorCrawlJobStatusService";
    private static final String NAME_HERITRIX_CLEANER_SERVICE = "heritrixCleanerService";
    private static final String NAME_ODS_CLEANER_SERVICE = "odsCleanerService";
    private static final String NAME_HEALTH_CHECK_MONITOR_SERVICE = "healthCheckMonitorService";
    private static final String NAME_BING_SERVICE = "bingService";

    private final List<Object> resources = new ArrayList<>();
    private final HashMap<String, AbstractService> services = new HashMap<>();
    private final Map<String, HealthCheck> healthChecks = new HashMap<>();

    private final HeritrixClient heritrixClient;

    private final CrawlRequestDAO crawlRequestDAO;
    private final CrawlJobDAO crawlJobDAO;
    private final DocumentDAO documentDAO;
    private final ValidationJobDAO validationJobDAO;
    private final ValidationErrorDAO validationErrorDAO;
    private final PdfPropertyDAO pdfPropertyDAO;
    private final NamespaceDAO namespaceDAO;

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrixClient, HibernateBundle<LogiusConfiguration> hibernate) {
        this.heritrixClient = heritrixClient;
        ReportsConfiguration reportsConfiguration = config.getReportsConfiguration();
        // Initializing static classes
        ReportsGenerator.initialize(reportsConfiguration);
        SendEmail.initialize(config.getEmailServerConfiguration(), reportsConfiguration);
        // Initializing all DAO objects
        crawlRequestDAO = new CrawlRequestDAO(hibernate.getSessionFactory());
        crawlJobDAO = new CrawlJobDAO(hibernate.getSessionFactory());
        documentDAO = new DocumentDAO(hibernate.getSessionFactory());
        validationJobDAO = new ValidationJobDAO(hibernate.getSessionFactory());
        validationErrorDAO = new ValidationErrorDAO(hibernate.getSessionFactory());
        pdfPropertyDAO = new PdfPropertyDAO(hibernate.getSessionFactory());
        namespaceDAO = new NamespaceDAO(hibernate.getSessionFactory());

        VeraPDFServiceConfiguration veraPDFServiceConfiguration = config.getVeraPDFServiceConfiguration();

        // Initializing validators and reporters
        VeraPDFValidator veraPDFValidator = new VeraPDFValidator(veraPDFServiceConfiguration);
        services.put(NAME_VALIDATION_SERVICE, new UnitOfWorkAwareProxyFactory(hibernate).create(ValidationService.class,
                new Class[]{ResourceManager.class, PDFValidator.class},
                new Object[]{this, veraPDFValidator}));
        services.put(NAME_BING_SERVICE, new UnitOfWorkAwareProxyFactory(hibernate).create(BingService.class,
                new Class[]{BingConfiguration.class, ResourceManager.class},
                new Object[]{config.getBingConfiguration(), this}));
        services.put(NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE, new UnitOfWorkAwareProxyFactory(hibernate).create(MonitorCrawlJobStatusService.class,
                new Class[]{ResourceManager.class},
                new Object[]{this}));
        services.put(NAME_HERITRIX_CLEANER_SERVICE, new HeritrixCleanerService(this));

        // Discover admin connector port
        DefaultServerFactory serverFactory = (DefaultServerFactory) config.getServerFactory();
        HttpConnectorFactory adminConnectorFactory = (HttpConnectorFactory) serverFactory.getAdminConnectors().get(0);
        int adminPort = adminConnectorFactory.getPort();

        // Initializing resources
        CrawlJobResource crawlJobResource = new CrawlJobResource(this);
        resources.add(crawlJobResource);
        resources.add(new CrawlRequestResource(this));
        resources.add(new DocumentResource(this));
        resources.add(new DocumentPropertyResource(this));
        resources.add(new ValidationServiceResource(this));
        resources.add(new ReportResource(this));
        HealthResource healthResource = new HealthResource(adminPort);
        resources.add(healthResource);
        resources.add(new HeritrixResource(this));
        resources.add(new AdminResource(this));

        // Initializing the rest of services
        services.put(NAME_ODS_CLEANER_SERVICE, new ODSCleanerService(reportsConfiguration));
        services.put(NAME_HEALTH_CHECK_MONITOR_SERVICE, new HealthCheckMonitorService(healthResource,
                Arrays.asList(
                        NAME_VALIDATION_SERVICE,
                        NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE,
                        NAME_HERITRIX_CLEANER_SERVICE,
                        NAME_ODS_CLEANER_SERVICE,
                        NAME_HEALTH_CHECK_MONITOR_SERVICE,
                        NAME_BING_SERVICE
                )));

        // Initializing health checks
        healthChecks.put("heritrix", new HeritrixHealthCheck(heritrixClient));
        healthChecks.put("verapdf",
                new VeraPDFServiceHealthCheck(veraPDFServiceConfiguration));
        for (Map.Entry<String, AbstractService> serviceEntry : this.services.entrySet()) {
            healthChecks.put(serviceEntry.getKey(), new ServiceHealthCheck(serviceEntry.getValue()));
        }

        // Launching services
        for (AbstractService service : this.services.values()) {
            service.start();
        }
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(this.healthChecks);
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }

    public CrawlRequestDAO getCrawlRequestDAO() {
        return crawlRequestDAO;
    }

    public CrawlJobDAO getCrawlJobDAO() {
        return crawlJobDAO;
    }

    public DocumentDAO getDocumentDAO() {
        return documentDAO;
    }

    public ValidationJobDAO getValidationJobDAO() {
        return validationJobDAO;
    }

    public ValidationErrorDAO getValidationErrorDAO() {
        return validationErrorDAO;
    }

    public PdfPropertyDAO getPdfPropertyDAO() {
        return pdfPropertyDAO;
    }

    public NamespaceDAO getNamespaceDAO() {
        return namespaceDAO;
    }

    public HeritrixClient getHeritrixClient() {
        return heritrixClient;
    }

    public AbstractService getService(String name) {
        return services.get(name);
    }

    public BingService getBingService() {
        return (BingService) services.get(NAME_BING_SERVICE);
    }

    public ValidationService getValidationService() {
        return (ValidationService) services.get(NAME_VALIDATION_SERVICE);
    }

    public HeritrixCleanerService getHeritrixCleanerService() {
        return (HeritrixCleanerService) services.get(NAME_HERITRIX_CLEANER_SERVICE);
    }
}

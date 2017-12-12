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

import java.util.*;

public class ResourceManager {

    private static final String HEALTH_CHECK_NAME_VALIDATION_SERVICE = "validationService";
    private static final String HEALTH_CHECK_NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE = "monitorCrawlJobStatusService";
    private static final String HEALTH_CHECK_NAME_HERITRIX_CLEANER_SERVICE = "heritrixCleanerService";
    private static final String HEALTH_CHECK_NAME_ODS_CLEANER_SERVICE = "odsCleanerService";
    private static final String HEALTH_CHECK_NAME_HEALTH_CHECK_MONITOR_SERVICE = "healthCheckMonitorService";
    private static final String HEALTH_CHECK_NAME_BING_SERVICE = "bingService";

    private final List<Object> resources = new ArrayList<>();
    private final Map<String, HealthCheck> healthChecks = new HashMap<>();

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrix, HibernateBundle<LogiusConfiguration> hibernate) {
        ReportsConfiguration reportsConfiguration = config.getReportsConfiguration();
        // Initializing static classes
        ReportsGenerator.initialize(reportsConfiguration);
        SendEmail.initialize(config.getEmailServerConfiguration(), reportsConfiguration);
        // Initializing all DAO objects
        CrawlRequestDAO crawlRequestDAO = new CrawlRequestDAO(hibernate.getSessionFactory());
        CrawlJobDAO crawlJobDAO = new CrawlJobDAO(hibernate.getSessionFactory());
        DocumentDAO documentDAO = new DocumentDAO(hibernate.getSessionFactory());
        ValidationJobDAO validationJobDAO = new ValidationJobDAO(hibernate.getSessionFactory());
        ValidationErrorDAO validationErrorDAO = new ValidationErrorDAO(hibernate.getSessionFactory());
        PdfPropertyDAO pdfPropertyDAO = new PdfPropertyDAO(hibernate.getSessionFactory());
        NamespaceDAO namespaceDAO = new NamespaceDAO(hibernate.getSessionFactory());

        VeraPDFServiceConfiguration veraPDFServiceConfiguration = config.getVeraPDFServiceConfiguration();

        // Initializing validators and reporters
        VeraPDFValidator veraPDFValidator = new VeraPDFValidator(veraPDFServiceConfiguration);
        ValidationService validationService = new UnitOfWorkAwareProxyFactory(hibernate).create(ValidationService.class,
                new Class[]{ValidationJobDAO.class, ValidationErrorDAO.class, DocumentDAO.class, PDFValidator.class},
                new Object[]{validationJobDAO, validationErrorDAO, documentDAO, veraPDFValidator});
        BingService bingService = new UnitOfWorkAwareProxyFactory(hibernate).create(BingService.class,
                new Class[]{BingConfiguration.class, CrawlJobDAO.class, DocumentDAO.class, ValidationJobDAO.class},
                new Object[]{config.getBingConfiguration(), crawlJobDAO, documentDAO, validationJobDAO});
        MonitorCrawlJobStatusService monitorCrawlJobStatusService = new UnitOfWorkAwareProxyFactory(hibernate).create(MonitorCrawlJobStatusService.class,
                new Class[]{BingService.class, CrawlJobDAO.class, CrawlRequestDAO.class, ValidationJobDAO.class, HeritrixClient.class},
                new Object[]{bingService, crawlJobDAO, crawlRequestDAO, validationJobDAO, heritrix});
        HeritrixCleanerService heritrixCleanerService = new HeritrixCleanerService(heritrix);

        // Discover admin connector port
        DefaultServerFactory serverFactory = (DefaultServerFactory) config.getServerFactory();
        HttpConnectorFactory adminConnectorFactory = (HttpConnectorFactory) serverFactory.getAdminConnectors().get(0);
        int adminPort = adminConnectorFactory.getPort();

        // Initializing resources
        CrawlJobResource crawlJobResource = new CrawlJobResource(crawlJobDAO, validationJobDAO, heritrix, validationService, heritrixCleanerService, bingService);
        resources.add(crawlJobResource);
        resources.add(new CrawlRequestResource(crawlRequestDAO, crawlJobDAO, heritrix, crawlJobResource));
        resources.add(new DocumentResource(crawlJobDAO, documentDAO, validationJobDAO));
        resources.add(new DocumentPropertyResource(documentDAO));
        resources.add(new ValidationServiceResource(pdfPropertyDAO, namespaceDAO, validationJobDAO));
        resources.add(new ReportResource(documentDAO));
        HealthResource healthResource = new HealthResource(adminPort);
        resources.add(healthResource);
        resources.add(new HeritrixResource(heritrix));

        // Initializing the rest of services
        ODSCleanerService odsCleanerService = new ODSCleanerService(reportsConfiguration);
        HealthCheckMonitorService healthCheckMonitorService = new HealthCheckMonitorService(healthResource,
                Arrays.asList(
                        HEALTH_CHECK_NAME_VALIDATION_SERVICE,
                        HEALTH_CHECK_NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE,
                        HEALTH_CHECK_NAME_HERITRIX_CLEANER_SERVICE,
                        HEALTH_CHECK_NAME_ODS_CLEANER_SERVICE,
                        HEALTH_CHECK_NAME_HEALTH_CHECK_MONITOR_SERVICE,
                        HEALTH_CHECK_NAME_BING_SERVICE
                ));

        // Initializing health checks
        healthChecks.put("heritrix", new HeritrixHealthCheck(heritrix));
        healthChecks.put("verapdf",
                new VeraPDFServiceHealthCheck(veraPDFServiceConfiguration));
        healthChecks.put(HEALTH_CHECK_NAME_VALIDATION_SERVICE, new ServiceHealthCheck(validationService));
        healthChecks.put(HEALTH_CHECK_NAME_MONITOR_CRAWL_JOB_STATUS_SERVICE,
                new ServiceHealthCheck(monitorCrawlJobStatusService));
        healthChecks.put(HEALTH_CHECK_NAME_HERITRIX_CLEANER_SERVICE, new ServiceHealthCheck(heritrixCleanerService));
        healthChecks.put(HEALTH_CHECK_NAME_ODS_CLEANER_SERVICE, new ServiceHealthCheck(odsCleanerService));
        healthChecks.put(HEALTH_CHECK_NAME_HEALTH_CHECK_MONITOR_SERVICE, new ServiceHealthCheck(healthCheckMonitorService));
        healthChecks.put(HEALTH_CHECK_NAME_BING_SERVICE, new ServiceHealthCheck(bingService));

        // Launching services
        validationService.start();
        monitorCrawlJobStatusService.start();
        heritrixCleanerService.start();
        odsCleanerService.start();
        healthCheckMonitorService.start();
        bingService.start();
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(this.healthChecks);
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }
}

package org.verapdf.crawler;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import org.verapdf.crawler.configurations.ReportsConfiguration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.core.email.SendEmail;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.services.HealthCheckMonitorService;
import org.verapdf.crawler.core.services.HeritrixCleanerService;
import org.verapdf.crawler.core.services.MonitorCrawlJobStatusService;
import org.verapdf.crawler.core.reports.ReportsGenerator;
import org.verapdf.crawler.core.services.ODSCleanerService;
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
        MonitorCrawlJobStatusService monitorCrawlJobStatusService = new UnitOfWorkAwareProxyFactory(hibernate).create(MonitorCrawlJobStatusService.class,
                new Class[]{CrawlJobDAO.class, CrawlRequestDAO.class, ValidationJobDAO.class, HeritrixClient.class},
                new Object[]{crawlJobDAO, crawlRequestDAO, validationJobDAO, heritrix});
        HeritrixCleanerService heritrixCleanerService = new HeritrixCleanerService(heritrix);

        // Discover admin connector port
        DefaultServerFactory serverFactory = (DefaultServerFactory) config.getServerFactory();
        HttpConnectorFactory adminConnectorFactory = (HttpConnectorFactory) serverFactory.getAdminConnectors().get(0);
        int adminPort = adminConnectorFactory.getPort();

        // Initializing resources
        resources.add(new CrawlJobResource(crawlJobDAO, validationJobDAO, heritrix, validationService, heritrixCleanerService));
        resources.add(new CrawlRequestResource(crawlRequestDAO, crawlJobDAO, heritrix));
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
                        HEALTH_CHECK_NAME_HEALTH_CHECK_MONITOR_SERVICE
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

        // Launching services
        validationService.start();
        monitorCrawlJobStatusService.start();
        heritrixCleanerService.start();
        odsCleanerService.start();
        healthCheckMonitorService.start();
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(this.healthChecks);
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }
}

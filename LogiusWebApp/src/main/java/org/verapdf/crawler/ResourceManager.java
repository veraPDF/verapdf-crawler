package org.verapdf.crawler;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.services.HeritrixCleanerService;
import org.verapdf.crawler.core.services.MonitorCrawlJobStatusService;
import org.verapdf.crawler.core.reports.ReportsGenerator;
import org.verapdf.crawler.core.validation.PDFValidator;
import org.verapdf.crawler.db.*;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.core.validation.VeraPDFValidator;
import org.verapdf.crawler.health.*;
import org.verapdf.crawler.resources.*;

import java.util.*;

public class ResourceManager {

    private final List<Object> resources = new ArrayList<>();
    private final Map<String, HealthCheck> healthChecks = new HashMap<>();

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrix, HibernateBundle<LogiusConfiguration> hibernate) {
        // Initializing report generator
        ReportsGenerator.initialize(config.getReportsConfiguration());
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
                new Class[]{CrawlJobDAO.class, CrawlRequestDAO.class, ValidationJobDAO.class, HeritrixClient.class, EmailServerConfiguration.class},
                new Object[]{crawlJobDAO, crawlRequestDAO, validationJobDAO, heritrix, config.getEmailServerConfiguration()});
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
        resources.add(new HealthResource(adminPort));
        resources.add(new HeritrixResource(heritrix));

        // Initializing health checks
        healthChecks.put("heritrix", new HeritrixHealthCheck(heritrix));
        healthChecks.put("verapdf",
                new VeraPDFServiceHealthCheck(veraPDFServiceConfiguration));
        healthChecks.put("validationService", new ServiceHealthCheck(validationService));
        healthChecks.put("monitorCrawlJobStatusService",
                new ServiceHealthCheck(monitorCrawlJobStatusService));
        healthChecks.put("heritrixCleanerService", new ServiceHealthCheck(heritrixCleanerService));

        // Launching services
        validationService.start();
        monitorCrawlJobStatusService.start();
        heritrixCleanerService.start();
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(this.healthChecks);
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }
}

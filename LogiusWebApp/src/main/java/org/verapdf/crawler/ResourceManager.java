package org.verapdf.crawler;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.jobs.MonitorCrawlJobStatusService;
import org.verapdf.crawler.core.validation.PDFValidator;
import org.verapdf.crawler.db.*;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.core.validation.VeraPDFValidator;
import org.verapdf.crawler.health.HeritrixHealthCheck;
import org.verapdf.crawler.health.MonitorCrawlJobStatusServiceHealthCheck;
import org.verapdf.crawler.health.ValidationServiceHealthCheck;
import org.verapdf.crawler.health.VeraPDFServiceHealthCheck;
import org.verapdf.crawler.resources.*;

import java.util.*;

public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    private final List<Object> resources = new ArrayList<>();
    private final Map<String, HealthCheck> healthChecks = new HashMap<>();

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrix, HibernateBundle<LogiusConfiguration> hibernate) {
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

        // Discover admin connector port
        DefaultServerFactory serverFactory = (DefaultServerFactory) config.getServerFactory();
        HttpConnectorFactory adminConnectorFactory = (HttpConnectorFactory) serverFactory.getAdminConnectors().get(0);
        int adminPort = adminConnectorFactory.getPort();

        // Initializing resources
        resources.add(new CrawlJobResource(crawlJobDAO, validationJobDAO, heritrix));
        resources.add(new CrawlRequestResource(crawlRequestDAO, crawlJobDAO, heritrix));
        resources.add(new DocumentResource(crawlJobDAO, documentDAO, validationJobDAO));
        resources.add(new DocumentPropertyResource(documentDAO));
        resources.add(new ValidationServiceResource(pdfPropertyDAO, namespaceDAO, validationJobDAO));
        resources.add(new ReportResource(documentDAO));
        resources.add(new HealthResource(adminPort));

        // Initializing health checks
        healthChecks.put("heritrix", new HeritrixHealthCheck(heritrix));
        healthChecks.put("verapdf",
                new VeraPDFServiceHealthCheck(veraPDFServiceConfiguration));
        healthChecks.put("validationService", new ValidationServiceHealthCheck(validationService));
        healthChecks.put("monitorCrawlJobStatusService",
                new MonitorCrawlJobStatusServiceHealthCheck(monitorCrawlJobStatusService));

        // Launching services
        validationService.start();
        monitorCrawlJobStatusService.start();
    }

    public Map<String, HealthCheck> getHealthChecks() {
        return Collections.unmodifiableMap(this.healthChecks);
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }
}

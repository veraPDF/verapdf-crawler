package org.verapdf.crawler;

import io.dropwizard.hibernate.HibernateBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.verapdf.crawler.configurations.MySqlConfiguration;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.heritrix.HeritrixReporter;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.CrawlRequestDAO;
import org.verapdf.crawler.db.document.InsertDocumentDao;
import org.verapdf.crawler.db.document.ReportDocumentDao;
import org.verapdf.crawler.db.document.ValidatedPDFDao;
import org.verapdf.crawler.db.jobs.CrawlRequestDao;
import org.verapdf.crawler.db.jobs.CrawlJobDao;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.core.validation.VeraPDFValidator;
import org.verapdf.crawler.resources.*;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourceManager {
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final List<Object> resources = new ArrayList<>();

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrix, HibernateBundle<LogiusConfiguration> hibernate) {
        // Initializing all DAO objects (will be deprecated)
        DataSource dataSource = createMySqlDataSource(config.getMySqlConfiguration());
        CrawlJobDao crawlJobDao = new CrawlJobDao(dataSource);
        CrawlRequestDao crawlRequestDao = new CrawlRequestDao(dataSource);
        ReportDocumentDao reportDocumentDao = new ReportDocumentDao(dataSource);
        ValidatedPDFDao validatedPDFDao = new ValidatedPDFDao(dataSource);
        InsertDocumentDao insertDocumentDao = new InsertDocumentDao(dataSource);

        // Initializing all DAO objects
        CrawlRequestDAO crawlRequestDAO = new CrawlRequestDAO(hibernate.getSessionFactory());
        CrawlJobDAO crawlJobDAO = new CrawlJobDAO(hibernate.getSessionFactory());

        // Initializing validators and reporters
        HeritrixReporter reporter = new HeritrixReporter(heritrix, reportDocumentDao, crawlJobDao);
        VeraPDFValidator veraPDFValidator = new VeraPDFValidator(config.getVeraPDFServiceConfiguration(), insertDocumentDao, validatedPDFDao, crawlJobDao);
        ValidationService validationService = new ValidationService(dataSource, veraPDFValidator);

        // Initializing resources
        resources.add(new CrawlJobReportResource(crawlJobDao, reporter, validatedPDFDao));
        resources.add(new CrawlJobResource(crawlJobDAO, heritrix));
        resources.add(new CrawlRequestResource(crawlRequestDAO, crawlJobDAO, heritrix));
        resources.add(new DocumentPropertyResource(reportDocumentDao));
        resources.add(new HeritrixDataResource(validationService, crawlJobDao, dataSource));
        resources.add(new VeraPDFServiceResource(validatedPDFDao));

        // Launching validation
        validationService.start();
        logger.info("Validation service started.");
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }

    private DataSource createMySqlDataSource(MySqlConfiguration credentials) {
        DataSource dataSource = new DriverManagerDataSource();
        ((DriverManagerDataSource)dataSource).setDriverClassName(JDBC_DRIVER);
        ((DriverManagerDataSource)dataSource).setUrl(credentials.getConnectionString());
        ((DriverManagerDataSource)dataSource).setUsername(credentials.getUser());
        ((DriverManagerDataSource)dataSource).setPassword(credentials.getPassword());
        return dataSource;
    }
}

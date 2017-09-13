package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.document.ReportDocumentDao;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.PDFValidator;
import org.verapdf.crawler.validation.ValidationService;
import org.verapdf.crawler.validation.VerapdfServiceValidator;

import javax.sql.DataSource;

public class ResourceManager {
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final HeritrixDataResource heritrixDataResource;
    private final CrawlJobReportResource crawlJobReportResource;
    private final CrawlJobResource crawlJobResource;
    private final CrawlRequestResource crawlRequestResource;
    private final DocumentPropertyResource documentPropertyResource;
    private final VerapdfServiceValidator validatorResource;

    private final ValidationService validationService;

    // TODO: refactor such that all resources will be stored in list
    public ResourceManager(HeritrixClient client, EmailServer emailServer, String verapdfUrl, MySqlCredentials credentials) {
        DataSource dataSource = createMySqlDatasource(credentials);
        CrawlJobDao crawlJobDao = new CrawlJobDao(dataSource);
        CrawlRequestDao crawlRequestDao = new CrawlRequestDao(dataSource);
        ReportDocumentDao reportDocumentDao = new ReportDocumentDao(dataSource);

        HeritrixReporter reporter = new HeritrixReporter(client, reportDocumentDao, crawlJobDao);
        ValidatedPDFDao validatedPDFDao = new ValidatedPDFDao(dataSource);
        PDFValidator validator = new VerapdfServiceValidator(verapdfUrl, new InsertDocumentDao(dataSource), validatedPDFDao, crawlJobDao);
        validatorResource = (VerapdfServiceValidator) validator;
        validationService = new ValidationService(dataSource, validator);
        heritrixDataResource = new HeritrixDataResource(validationService, crawlJobDao, dataSource);
        crawlJobReportResource = new CrawlJobReportResource(crawlJobDao, reporter, validatedPDFDao);
        crawlJobResource = new CrawlJobResource(crawlJobDao, client, crawlRequestDao, reporter, emailServer);
        crawlRequestResource = new CrawlRequestResource(client, crawlRequestDao, crawlJobDao);
        documentPropertyResource = new DocumentPropertyResource(reportDocumentDao);

        validationService.start();
        logger.info("Validation service started.");
    }

    public HeritrixDataResource getHeritrixDataResource() {
        return heritrixDataResource;
    }

    public CrawlJobReportResource getCrawlJobReportResource() {
        return crawlJobReportResource;
    }

    public CrawlJobResource getCrawlJobResource() {
        return crawlJobResource;
    }

    public CrawlRequestResource getCrawlRequestResource() {
        return crawlRequestResource;
    }

    public DocumentPropertyResource getDocumentPropertyResource() {
        return documentPropertyResource;
    }

    public VerapdfServiceValidator getValidatorResource() {
        return validatorResource;
    }

    private DataSource createMySqlDatasource(MySqlCredentials credentials) {
        DataSource dataSource = new DriverManagerDataSource();
        ((DriverManagerDataSource)dataSource).setDriverClassName(JDBC_DRIVER);
        ((DriverManagerDataSource)dataSource).setUrl(credentials.connectionString);
        ((DriverManagerDataSource)dataSource).setUsername(credentials.user);
        ((DriverManagerDataSource)dataSource).setPassword(credentials.password);
        return dataSource;
    }
}

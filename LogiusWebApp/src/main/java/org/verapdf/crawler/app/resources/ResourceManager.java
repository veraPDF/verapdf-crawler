package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.report.CrawlJobReport;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;
import org.verapdf.crawler.repository.jobs.BatchJobDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.PDFValidator;
import org.verapdf.crawler.validation.ValidationService;
import org.verapdf.crawler.validation.VerapdfServiceValidator;

import javax.sql.DataSource;

public class ResourceManager {
    private final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final InfoResourse infoResourse;
    private final ReportResource reportResource;
    private final ControlResource controlResource;
    private final CrawlJobDao crawlJobDao;
    private final VerapdfServiceValidator validatorResource;

    private String resourceUri;
    private final ValidationService validationService;
    private final EmailServer emailServer;

    public ResourceManager(HeritrixClient client, EmailServer emailServer, String verapdfUrl, MySqlCredentials credentials) {
        DataSource dataSource = createMySqlDatasource(credentials);
        crawlJobDao = new CrawlJobDao(dataSource);
        BatchJobDao batchJobDao = new BatchJobDao(dataSource);

        HeritrixReporter reporter = new HeritrixReporter(client, dataSource, crawlJobDao);
        this.emailServer = emailServer;
        PDFValidator validator = new VerapdfServiceValidator(verapdfUrl, new ValidatedPDFDao(dataSource));
        validatorResource = (VerapdfServiceValidator) validator;
        validationService = new ValidationService(dataSource, validator);
        infoResourse = new InfoResourse(validationService, batchJobDao);
        reportResource = new ReportResource(reporter, crawlJobDao, batchJobDao);
        controlResource = new ControlResource(client, reporter, emailServer,validationService,
                this, crawlJobDao, dataSource, batchJobDao);

        for(BatchJob batchJob: batchJobDao.getBatchJobs()) {
            for (String jobId: batchJob.getCrawlJobs()) {
                CurrentJob job = crawlJobDao.getCrawlJob(jobId);
                if (job.getFinishTime() == null) {
                    crawlJobDao.writeFinishTime(job.getId());
                }
            }
        }

        new Thread(new StatusMonitor(batchJobDao, controlResource)).start();
        validationService.start();
        new Thread(validationService).start();
        logger.info("Validation service started.");
    }

    public InfoResourse getInfoResourse() {
        return infoResourse;
    }

    public ReportResource getReportResource() {
        return reportResource;
    }

    public ControlResource getControlResource() {
        return controlResource;
    }

    String getResourceUri() { return resourceUri; }

    void setResourceUri(String resourceUri) { this.resourceUri = resourceUri; }

    public VerapdfServiceValidator getValidatorResource() {
        return validatorResource;
    }

    EmailServer getEmailServer() { return emailServer; }

    private DataSource createMySqlDatasource(MySqlCredentials credentials) {
        DataSource dataSource = new DriverManagerDataSource();
        ((DriverManagerDataSource)dataSource).setDriverClassName(JDBC_DRIVER);
        ((DriverManagerDataSource)dataSource).setUrl(credentials.connectionString);
        ((DriverManagerDataSource)dataSource).setUsername(credentials.user);
        ((DriverManagerDataSource)dataSource).setPassword(credentials.password);
        return dataSource;
    }
}

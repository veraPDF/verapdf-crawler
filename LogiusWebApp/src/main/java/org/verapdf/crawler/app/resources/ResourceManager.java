package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.report.SingleURLJobReport;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.ValidationService;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ResourceManager {
    private final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final ArrayList<BatchJob> batchJobs;
    private final InfoResourse infoResourse;
    private final ReportResource reportResource;
    private final ControlResource controlResource;
    private final CrawlJobDao crawlJobDao;

    private String resourceUri;
    private final ValidationService validationService;
    private final EmailServer emailServer;

    public ResourceManager(HeritrixClient client, EmailServer emailServer, String verapdfPath, MySqlCredentials credentials) {
        DataSource dataSource = createMySqlDatasource(credentials);
        crawlJobDao = new CrawlJobDao(dataSource);

        batchJobs = new ArrayList<>();
        HeritrixReporter reporter = new HeritrixReporter(client, dataSource);
        this.emailServer = emailServer;

        validationService = new ValidationService(verapdfPath, dataSource);
        infoResourse = new InfoResourse(validationService, client, crawlJobDao);
        reportResource = new ReportResource(reporter, this, crawlJobDao);
        controlResource = new ControlResource(client, reporter, emailServer, batchJobs, validationService, this, crawlJobDao, dataSource);

        for(CurrentJob job: crawlJobDao.getAllJobs()) {
            if(job.getFinishTime() == null) {
                crawlJobDao.writeFinishTime(job);
            }
        }

        new Thread(new StatusMonitor(this, crawlJobDao)).start();
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

    ArrayList<BatchJob> getBatchJobs() { return batchJobs; }

    SingleURLJobReport getJob(String job) { return controlResource.getJob(job); }

    String getResourceUri() { return resourceUri; }

    void setResourceUri(String resourceUri) { this.resourceUri = resourceUri; }

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

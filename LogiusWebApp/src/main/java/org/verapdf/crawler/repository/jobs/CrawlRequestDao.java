package org.verapdf.crawler.repository.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.repository.mappers.BatchJobMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CrawlRequestDao {
    private final JdbcTemplate template;
    static final String BATCH_JOB_TABLE_NAME = "crawl_job_requests";
    static final String BATCH_REFERENCE_TABLE_NAME = "crawl_job_requests_crawl_jobs";
    public static final String FIELD_ID = "id";
    public static final String FIELD_IS_FINISHED = "is_finished";
    public static final String FIELD_REPORT_EMAIL = "report_email";
    public static final String FIELD_CRAWL_SINCE = "crawl_since";
    static final String FIELD_BATCH_JOB_ID = "crawl_job_request_id";
    static final String FIELD_CRAWL_JOB_ID = "crawl_job_id";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public CrawlRequestDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addBatchJob(CrawlRequest crawlRequest) {
        logger.info("Batch job inserted into database: " + crawlRequest.getId());
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,false,?,?)"
                , BATCH_JOB_TABLE_NAME, FIELD_ID, FIELD_IS_FINISHED, FIELD_REPORT_EMAIL, FIELD_CRAWL_SINCE)
                , crawlRequest.getId(), crawlRequest.getEmailAddress(), crawlRequest.getCrawlSinceTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        for (String crawlJobId: crawlRequest.getCrawlJobs()) {
            template.update(String.format("insert into %s (%s, %s) values (?, ?)",
                    BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_ID), crawlRequest.getId(), crawlJobId);
        }
    }

    public List<CrawlRequest> getCrawlRequestsForCrawlJob(String jobId) {
        return template.query(String.format("select * from %s inner join %s on %s.%s=%s.%s where %s=?",
                BATCH_JOB_TABLE_NAME, BATCH_REFERENCE_TABLE_NAME, BATCH_JOB_TABLE_NAME,
                FIELD_ID, BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_ID),
                new Object[]{jobId}, new BatchJobMapper());
    }

    public CrawlRequest getBatchJob(String batchJobId) {
        CrawlRequest result = template.query(String.format("select * from %s where %s=?" ,BATCH_JOB_TABLE_NAME, FIELD_ID), new BatchJobMapper(), batchJobId).get(0);
        result.setCrawlJobs(getCrawlJobsForBatch(batchJobId));
        return result;
    }

    private List<String> getCrawlJobsForBatch(String batchJobId) {
        return template.queryForList(String.format("select %s from %s where %s=?", FIELD_CRAWL_JOB_ID, BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID), String.class, batchJobId);
    }

    public List<CrawlRequest> getBatchJobs() {
        List<CrawlRequest> result = template.query("select * from " + BATCH_JOB_TABLE_NAME, new BatchJobMapper());
        for(CrawlRequest crawlRequest : result) {
            crawlRequest.setCrawlJobs(getCrawlJobsForBatch(crawlRequest.getId()));
        }
        return result;
    }

    public void setJobFinished(String batchJob) {
        template.update(String.format("update %s set %s=? where %s=?", BATCH_JOB_TABLE_NAME, FIELD_IS_FINISHED, FIELD_ID), true, batchJob);
    }

    public void setReportEmail(String jobId, String emailAddress) {
        logger.info("Email address " + emailAddress + " was associated with job " + jobId + " in database");
        template.update(String.format("update %s set %s=? where %s=?", BATCH_JOB_TABLE_NAME, FIELD_REPORT_EMAIL, FIELD_ID), emailAddress, jobId);
    }

    public LocalDateTime getCrawlSince(String jobId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(template.queryForObject(String.format("select %s from %s where %s=?", FIELD_CRAWL_SINCE, BATCH_JOB_TABLE_NAME, FIELD_ID), new Object[] {jobId}, String.class), formatter);
    }

    public String getReportEmail(String jobId) {
        return template.queryForObject(String.format("select %s from %s where %s=?", FIELD_REPORT_EMAIL, BATCH_JOB_TABLE_NAME, FIELD_ID), new Object[] {jobId}, String.class);
    }
}

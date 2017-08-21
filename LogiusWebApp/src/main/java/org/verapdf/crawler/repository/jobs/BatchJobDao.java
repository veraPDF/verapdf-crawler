package org.verapdf.crawler.repository.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.repository.mappers.BatchJobMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BatchJobDao {
    private final JdbcTemplate template;
    private static final String BATCH_JOB_TABLE_NAME = "batch_crawl_jobs";
    private static final String BATCH_REFERENCE_TABLE_NAME = "crawl_jobs_in_batch";
    public static final String FIELD_ID = "id";
    public static final String FIELD_IS_FINISHED = "is_finished";
    public static final String FIELD_REPORT_EMAIL = "report_email";
    public static final String FIELD_CRAWL_SINCE = "crawl_since";
    private static final String FIELD_BATCH_JOB_ID = "batch_job_id";
    private static final String FIELD_CRAWL_JOB_ID = "crawl_job_id";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public BatchJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addBatchJob(BatchJob batchJob) {
        logger.info("Batch job inserted into database: " + batchJob.getId());
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,false,?,?)"
                , BATCH_JOB_TABLE_NAME, FIELD_ID, FIELD_IS_FINISHED, FIELD_REPORT_EMAIL, FIELD_CRAWL_SINCE)
                , batchJob.getId(), batchJob.getEmailAddress(), batchJob.getCrawlSinceTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        for (String crawlJobId: batchJob.getCrawlJobs()) {
            template.update(String.format("insert into %s (%s, %s) values (?, ?)",
                    BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_ID), batchJob.getId(), crawlJobId);
        }
    }

    public BatchJob getBatchJob(String batchJobId) {
        BatchJob result = template.query(String.format("select * from %s where %s=?" ,BATCH_JOB_TABLE_NAME, FIELD_ID), new BatchJobMapper(), batchJobId).get(0);
        result.setCrawlJobs(getCrawlJobsForBatch(batchJobId));
        return result;
    }

    private List<String> getCrawlJobsForBatch(String batchJobId) {
        return template.queryForList(String.format("select %s from %s where %s=?", FIELD_CRAWL_JOB_ID, BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID), String.class, batchJobId);
    }

    public List<BatchJob> getBatchJobs() {
        List<BatchJob> result = template.query("select * from " + BATCH_JOB_TABLE_NAME, new BatchJobMapper());
        for(BatchJob batchJob: result) {
            batchJob.setCrawlJobs(getCrawlJobsForBatch(batchJob.getId()));
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

package org.verapdf.crawler.repository.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.repository.mappers.BatchJobMapper;

import javax.sql.DataSource;
import java.util.List;

public class BatchJobDao {
    private final JdbcTemplate template;
    public static final String BATCH_JOB_TABLE_NAME = "batch_crawl_jobs";
    public static final String BATCH_REFERENCE_TABLE_NAME = "crawl_jobs_in_batch";
    public static final String FIELD_ID = "id";
    public static final String FIELD_IS_FINISHED = "is_finished";
    public static final String FIELD_REPORT_EMAIL = "report_email";
    public static final String FIELD_CRAWL_SINCE = "crawl_since";
    public static final String FIELD_BATCH_JOB_ID = "batch_job_id";
    public static final String FIELD_CRAWL_JOB_ID = "crawl_job_id";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public BatchJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
        template.execute(String.format("CREATE TABLE IF NOT EXISTS `%s` (\n" +
                "  `%s` varchar(36) DEFAULT NULL,\n" +
                "  `%s` tinyint(1) DEFAULT '0',\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` datetime DEFAULT NULL\n" +
                ")", BATCH_JOB_TABLE_NAME, FIELD_ID, FIELD_IS_FINISHED, FIELD_REPORT_EMAIL, FIELD_CRAWL_SINCE));
        template.execute(String.format("CREATE TABLE IF NOT EXISTS `%s` (\n" +
                "  `%s` varchar(36) DEFAULT NULL,\n" +
                "  `%s` varchar(36) DEFAULT NULL\n" +
                ")", BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_ID));
    }

    public void addBatchJob(BatchJob batchJob) {
        logger.info("Batch job inserted into database: " + batchJob.getId());
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,false,?,?)"
                , BATCH_JOB_TABLE_NAME, FIELD_ID, FIELD_IS_FINISHED, FIELD_REPORT_EMAIL, FIELD_CRAWL_SINCE)
                , batchJob.getId(), batchJob.getEmailAddress(), batchJob.getCrawlSinceTime());
        for (String crawlJobId: batchJob.getCrawlJobs()) {
            template.update(String.format("insert into %s (%s, %s) values (?, ?)",
                    BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_ID), batchJob.getId(), crawlJobId);
        }
    }

    public List<String> getCrawlJobsForBatch(String batchJobId) {
        return template.queryForList(String.format("selet * from %s where %s=?", BATCH_REFERENCE_TABLE_NAME, FIELD_BATCH_JOB_ID), String.class, batchJobId);
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
}

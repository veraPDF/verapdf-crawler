package org.verapdf.crawler.repository.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.repository.mappers.CrawlJobMapper;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class CrawlJobDao {

    public static final String CRAWL_JOB_TABLE_NAME = "crawl_jobs";
    public static final String FIELD_ID = "id";
    public static final String FIELD_CRAWL_URL = "crawl_url";
    public static final String FIELD_JOB_URL = "job_url";
    public static final String FIELD_CRAWL_SINCE = "crawl_since";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_REPORT_EMAIL = "report_email";
    public static final String FIELD_START_TIME = "start_time";
    public static final String FIELD_FINISH_TIME = "finish_time";
    public static final String FIELD_IS_FINISHED = "is_finished";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final JdbcTemplate template;

    public CrawlJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
        template.execute(String.format("CREATE TABLE IF NOT EXISTS `%s` (\n" +
                "  `%s` varchar(36) DEFAULT NULL,\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` datetime DEFAULT NULL,\n" +
                "  `%s` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "  `%s` datetime DEFAULT NULL,\n" +
                "  `%s` tinyint(1) DEFAULT '0',\n" +
                "  `%s` varchar(10) DEFAULT NULL,\n" +
                "  `%s` varchar(255) DEFAULT NULL\n" +
                ")", CRAWL_JOB_TABLE_NAME, FIELD_ID, FIELD_CRAWL_URL, FIELD_JOB_URL, FIELD_CRAWL_SINCE,
                FIELD_START_TIME, FIELD_FINISH_TIME, FIELD_IS_FINISHED, FIELD_STATUS, FIELD_REPORT_EMAIL));
    }

    public List<CurrentJob> getAllJobs() {
        return template.query("select * from " + CRAWL_JOB_TABLE_NAME, new CrawlJobMapper());
    }

    public void addJob(CurrentJob job) {
        logger.info("Job inserted into database: " + job);
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s, %s) values (?,?,?,?,'active', ?)"
                , CRAWL_JOB_TABLE_NAME, FIELD_ID, FIELD_CRAWL_URL, FIELD_JOB_URL, FIELD_CRAWL_SINCE, FIELD_STATUS, FIELD_REPORT_EMAIL)
                , job.getId(), job.getCrawlURL(), job.getJobURL(), job.getCrawlSinceTime(), job.getReportEmail());
    }

    public void removeJob(CurrentJob job) {
        logger.info("Job removed from database: " + job);
        template.update(String.format("delete from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_ID), job.getId());
    }

    public void writeFinishTime(CurrentJob job) {
        logger.info("Job marked as finished in database: " + job);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        template.update(String.format("update %s set %s=?, %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_FINISH_TIME, FIELD_IS_FINISHED, FIELD_ID),
                sdfDate.format(now), true, job.getId());
    }

    public CurrentJob getCrawlJob(String jobId) {
        return template.query(String.format("select * from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_ID), new CrawlJobMapper(), jobId).get(0);
    }

    public CurrentJob getCrawlJobByCrawlUrl(String url) {
        return template.query(String.format("select * from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_CRAWL_URL), new CrawlJobMapper(), url).get(0);
    }

    public String getCrawlUrl(String jobId) {
        return template.queryForObject(String.format("select %s from %s where %s=?", FIELD_CRAWL_URL, CRAWL_JOB_TABLE_NAME, FIELD_ID), new Object[] {jobId}, String.class);
    }

    public LocalDateTime getCrawlSince(String jobId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(template.queryForObject(String.format("select %s from %s where %s=?", FIELD_CRAWL_SINCE, CRAWL_JOB_TABLE_NAME, FIELD_ID), new Object[] {jobId}, String.class), formatter);
    }

    public boolean doesJobExist(String url) {
        Integer count = template.queryForObject(String.format("select count(*) from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_CRAWL_URL), Integer.class, url);
        return count != null && count != 0;
    }

    public void setJobFinished(String jobId, boolean isFinished) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_IS_FINISHED, FIELD_ID), isFinished, jobId);
    }

    public void setStatus(String jobId, String status) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_STATUS, FIELD_ID), status, jobId);
    }

    public void setReportEmail(String jobId, String emailAddress) {
        logger.info("Email address " + emailAddress + " was associated woth job " + jobId + " in database");
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_REPORT_EMAIL, FIELD_ID), emailAddress, jobId);
    }

    public void setJobUrl(String jobId, String jobUrl) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_JOB_URL, FIELD_ID), jobUrl, jobId);
    }
}

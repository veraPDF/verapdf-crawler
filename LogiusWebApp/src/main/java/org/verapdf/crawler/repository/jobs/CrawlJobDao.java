package org.verapdf.crawler.repository.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.mappers.CrawlJobMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class CrawlJobDao {

    private static final String CRAWL_JOB_TABLE_NAME = "crawl_jobs";
    public static final String FIELD_ID = "id";
    public static final String FIELD_CRAWL_URL = "domain";
    public static final String FIELD_JOB_URL = "job_url";
    public static final String FIELD_STATUS = "job_status";
    public static final String FIELD_START_TIME = "start_time";
    public static final String FIELD_FINISH_TIME = "finish_time";
    public static final String FIELD_IS_FINISHED = "is_finished";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final JdbcTemplate template;

    public CrawlJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public List<CrawlJob> getAllJobsWithFilter(String domainFilter) {
        if(domainFilter != null && !domainFilter.isEmpty()) {
            return template.query(String.format("select * from %s where %s like ? order by %s desc",
                    CRAWL_JOB_TABLE_NAME, FIELD_CRAWL_URL, FIELD_START_TIME), new CrawlJobMapper(),
                    "%" + domainFilter + "%");
        }
        return template.query(String.format("select * from %s order by %s desc",
                CRAWL_JOB_TABLE_NAME, FIELD_START_TIME), new CrawlJobMapper());
    }

    public Integer countJobsWithFilter(String domainFilter) {
        if(domainFilter != null && !domainFilter.isEmpty()) {
            return template.queryForObject(String.format("select count(*) from %s where %s like ?",
                    CRAWL_JOB_TABLE_NAME, FIELD_CRAWL_URL), new Object[]{"%" + domainFilter + "%"}, Integer.class);
        }
        return template.queryForObject(String.format("select count(*) from %s", CRAWL_JOB_TABLE_NAME), Integer.class);
    }

    public void addJob(CrawlJob job) {
        logger.info("Job inserted into database: " + job.getId());
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,?,?,'active')"
                , CRAWL_JOB_TABLE_NAME, FIELD_ID, FIELD_CRAWL_URL, FIELD_JOB_URL, FIELD_STATUS)
                , job.getId(), job.getCrawlURL(), job.getJobURL());
    }

    public void removeJob(String jobId) {
        logger.info("Job removed from database: " + jobId);
        template.update(String.format("delete from %s where %s=?", InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_JOB_ID), jobId);
        template.update(String.format("delete from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_ID), jobId);
    }

    public LocalDateTime writeFinishTime(String jobId) {
        logger.info("Job marked as finished in database: " + jobId);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        template.update(String.format("update %s set %s=?, %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_FINISH_TIME, FIELD_IS_FINISHED, FIELD_ID),
                sdfDate.format(now), true, jobId);
        return now;
    }

    public List<String> getreportEmailsForCrawlJob(String jobId) {
        return template.query(String.format("select %s from ((%s inner join %s on %s=%s) inner join %s on %s=%s.%s) where %s.%s=?",
                CrawlRequestDao.FIELD_REPORT_EMAIL, CRAWL_JOB_TABLE_NAME, CrawlRequestDao.BATCH_REFERENCE_TABLE_NAME,
                FIELD_ID, CrawlRequestDao.FIELD_CRAWL_JOB_ID, CrawlRequestDao.BATCH_JOB_TABLE_NAME,
                CrawlRequestDao.FIELD_BATCH_JOB_ID, CrawlRequestDao.BATCH_JOB_TABLE_NAME, CrawlRequestDao.FIELD_ID,
                CRAWL_JOB_TABLE_NAME, FIELD_ID),
                new Object[]{jobId},
                (resultSet, i) -> resultSet.getString(CrawlRequestDao.FIELD_REPORT_EMAIL));
    }

    public CrawlJob getCrawlJob(String jobId) {
        List<CrawlJob> resultList = template.query(String.format("select * from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_ID), new CrawlJobMapper(), jobId);
        if(resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    public CrawlJob getCrawlJobByCrawlUrl(String url) {
        return template.query(String.format("select * from %s where %s like ?", CRAWL_JOB_TABLE_NAME, FIELD_CRAWL_URL), new CrawlJobMapper(), "%" + url + "%").get(0);
    }

    public String getCrawlUrl(String jobId) {
        return template.queryForObject(String.format("select %s from %s where %s=?", FIELD_CRAWL_URL, CRAWL_JOB_TABLE_NAME, FIELD_ID), new Object[] {jobId}, String.class);
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

    public void setJobUrl(String jobId, String jobUrl) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_JOB_URL, FIELD_ID), jobUrl, jobId);
    }
}

package org.verapdf.crawler.db.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.db.mappers.CrawlJobMapper;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.List;

public class CrawlJobDao {

    private static final String CRAWL_JOB_TABLE_NAME = "crawl_jobs";
    public static final String FIELD_HERITRIX_JOB_ID = "heritrix_job_id";
    public static final String FIELD_DOMAIN = "domain";
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
                    CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN, FIELD_START_TIME), new CrawlJobMapper(),
                    "%" + domainFilter + "%");
        }
        return template.query(String.format("select * from %s order by %s desc",
                CRAWL_JOB_TABLE_NAME, FIELD_START_TIME), new CrawlJobMapper());
    }

    public Integer countJobsWithFilter(String domainFilter) {
        if(domainFilter != null && !domainFilter.isEmpty()) {
            return template.queryForObject(String.format("select count(*) from %s where %s like ?",
                    CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), new Object[]{"%" + domainFilter + "%"}, Integer.class);
        }
        return template.queryForObject(String.format("select count(*) from %s", CRAWL_JOB_TABLE_NAME), Integer.class);
    }

    public void addJob(CrawlJob job) {
        logger.info("Job inserted into database: " + job.getHeritrixJobId());
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,?,?,'active')"
                , CRAWL_JOB_TABLE_NAME, FIELD_HERITRIX_JOB_ID, FIELD_DOMAIN, FIELD_JOB_URL, FIELD_STATUS)
                , job.getHeritrixJobId(), job.getDomain(), job.getJobURL());
    }

    public void removeJob(String domain) {
        logger.info("Job removed from database: " + domain);
        template.update(String.format("delete from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), domain);
    }

    public LocalDateTime writeFinishTime(String domain) {
        logger.info("Job marked as finished in database: " + domain);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        template.update(String.format("update %s set %s=?, %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_FINISH_TIME, FIELD_IS_FINISHED, FIELD_DOMAIN),
                sdfDate.format(now), true, domain);
        return now;
    }

    public List<String> getReportEmailsForCrawlJob(String domain) {
        return template.query(String.format("select %s from ((%s inner join %s on %s=%s) inner join %s on %s=%s.%s) where %s.%s=?",
                CrawlRequestDao.FIELD_REPORT_EMAIL, CRAWL_JOB_TABLE_NAME, CrawlRequestDao.TABLE_CRAWL_JOB_REF,
                FIELD_HERITRIX_JOB_ID, CrawlRequestDao.FIELD_CRAWL_JOB_DOMAIN, CrawlRequestDao.TABLE_CRAWL_JOB_REQUESTS,
                CrawlRequestDao.FIELD_BATCH_JOB_ID, CrawlRequestDao.TABLE_CRAWL_JOB_REQUESTS, CrawlRequestDao.FIELD_ID,
                CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN),
                new Object[]{domain},
                (resultSet, i) -> resultSet.getString(CrawlRequestDao.FIELD_REPORT_EMAIL));
    }

    public CrawlJob getCrawlJob(String domain) {
        List<CrawlJob> resultList = template.query(String.format("select * from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), new CrawlJobMapper(), domain);
        if(resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    public CrawlJob getCrawlJobByCrawlUrl(String url) {
        List<CrawlJob> query = template.query(String.format("select * from %s where %s like ?", CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), new CrawlJobMapper(), "%" + url + "%");
        return query == null || query.isEmpty() ? null : query.get(0);
    }

    public String getIdByUrl(String url) {
        return template.queryForObject(String.format("select %s from %s where %s like ?", FIELD_HERITRIX_JOB_ID, CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), new Object[]{"%" + url + "%"}, String.class);
    }

    public String getCrawlUrl(String jobId) {
        return template.queryForObject(String.format("select %s from %s where %s=?", FIELD_DOMAIN, CRAWL_JOB_TABLE_NAME, FIELD_HERITRIX_JOB_ID), new Object[] {jobId}, String.class);
    }

    public boolean doesJobExist(String url) {
        Integer count = template.queryForObject(String.format("select count(*) from %s where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_DOMAIN), Integer.class, url);
        return count != null && count != 0;
    }

    public void setJobFinished(String domain, boolean isFinished) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_IS_FINISHED, FIELD_DOMAIN), isFinished, domain);
    }

    public void setStatus(String domain, String status) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_STATUS, FIELD_DOMAIN), status, domain);
    }

    public void setJobUrl(String domain, String jobUrl) {
        template.update(String.format("update %s set %s=? where %s=?", CRAWL_JOB_TABLE_NAME, FIELD_JOB_URL, FIELD_DOMAIN), jobUrl, domain);
    }

    public List<String> getActiveDomains() {
        return template.queryForList(String.format("select %s from %s where %s=?", FIELD_JOB_URL, CRAWL_JOB_TABLE_NAME, FIELD_IS_FINISHED), new Object[]{false}, String.class);
    }
}

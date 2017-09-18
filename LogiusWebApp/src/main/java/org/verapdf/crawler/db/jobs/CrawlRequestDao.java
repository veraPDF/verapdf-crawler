package org.verapdf.crawler.db.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.db.mappers.CrawlRequestMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CrawlRequestDao {
    private final JdbcTemplate template;
    static final String TABLE_CRAWL_JOB_REQUESTS = "crawl_job_requests";
    static final String TABLE_CRAWL_JOB_REF = "crawl_job_requests_crawl_jobs";
    public static final String FIELD_ID = "id";
    public static final String FIELD_IS_FINISHED = "is_finished";
    public static final String FIELD_REPORT_EMAIL = "report_email";
    public static final String FIELD_CRAWL_SINCE = "crawl_since";
    static final String FIELD_BATCH_JOB_ID = "crawl_job_request_id";
    static final String FIELD_CRAWL_JOB_DOMAIN = "crawl_job_domain";

    public CrawlRequestDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addCrawlRequest(CrawlRequest crawlRequest) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values (?,false,?,?)"
                , TABLE_CRAWL_JOB_REQUESTS, FIELD_ID, FIELD_IS_FINISHED, FIELD_REPORT_EMAIL, FIELD_CRAWL_SINCE)
                , crawlRequest.getId(), crawlRequest.getEmailAddress(), crawlRequest.getCrawlSinceTime());
        for (String domain: crawlRequest.getDomains()) {
            template.update(String.format("insert into %s (%s, %s) values (?, ?)",
                    TABLE_CRAWL_JOB_REF, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_DOMAIN), crawlRequest.getId(), domain);
        }
    }

    public List<CrawlRequest> getCrawlRequestsForCrawlJob(String domain) {
        return template.query(String.format("select * from %s inner join %s on %s.%s=%s.%s where %s=?",
                TABLE_CRAWL_JOB_REQUESTS, TABLE_CRAWL_JOB_REF, TABLE_CRAWL_JOB_REQUESTS,
                FIELD_ID, TABLE_CRAWL_JOB_REF, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_DOMAIN),
                new Object[]{domain}, new CrawlRequestMapper());
    }

    public CrawlRequest getBatchJob(String batchJobId) {
        CrawlRequest result = template.query(String.format("select * from %s where %s=?" , TABLE_CRAWL_JOB_REQUESTS, FIELD_ID), new CrawlRequestMapper(), batchJobId).get(0);
        result.setDomains(getCrawlJobsForBatch(batchJobId));
        return result;
    }

    private List<String> getCrawlJobsForBatch(String batchJobId) {
        return template.queryForList(String.format("select %s from %s where %s=?", FIELD_CRAWL_JOB_DOMAIN, TABLE_CRAWL_JOB_REF, FIELD_BATCH_JOB_ID), String.class, batchJobId);
    }

    public List<CrawlRequest> getBatchJobs() {
        List<CrawlRequest> result = template.query("select * from " + TABLE_CRAWL_JOB_REQUESTS, new CrawlRequestMapper());
        for(CrawlRequest crawlRequest : result) {
            crawlRequest.setDomains(getCrawlJobsForBatch(crawlRequest.getId()));
        }
        return result;
    }

    public List<String> getIdsByEmail(String emailAddress) {
        return template.queryForList(String.format("select %s from %s where %s=?", FIELD_ID, TABLE_CRAWL_JOB_REQUESTS, FIELD_REPORT_EMAIL), new Object[]{emailAddress}, String.class);
    }

    public void setJobFinished(String batchJob) {
        template.update(String.format("update %s set %s=? where %s=?", TABLE_CRAWL_JOB_REQUESTS, FIELD_IS_FINISHED, FIELD_ID), true, batchJob);
    }

    public LocalDateTime getCrawlSince(String jobId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(template.queryForObject(String.format("select %s from %s where %s=?", FIELD_CRAWL_SINCE, TABLE_CRAWL_JOB_REQUESTS, FIELD_ID), new Object[] {jobId}, String.class), formatter);
    }

    public String getReportEmail(String jobId) {
        return template.queryForObject(String.format("select %s from %s where %s=?", FIELD_REPORT_EMAIL, TABLE_CRAWL_JOB_REQUESTS, FIELD_ID), new Object[] {jobId}, String.class);
    }

    public void unlinkCrawlJob(String batchJobId, String domain) {
        template.update(String.format("delete from %s where %s=? and %s=?", TABLE_CRAWL_JOB_REF, FIELD_BATCH_JOB_ID, FIELD_CRAWL_JOB_DOMAIN), batchJobId, domain);
        Integer remainCrawlJobCount = template.queryForObject(String.format("select count(*) from %s where %s=?", TABLE_CRAWL_JOB_REQUESTS, FIELD_ID), new Object[]{batchJobId}, Integer.class);
        if(remainCrawlJobCount == null || remainCrawlJobCount == 0) {
            template.update(String.format("delete from %s where %s=?", TABLE_CRAWL_JOB_REQUESTS, FIELD_ID), batchJobId);
        }
    }
}

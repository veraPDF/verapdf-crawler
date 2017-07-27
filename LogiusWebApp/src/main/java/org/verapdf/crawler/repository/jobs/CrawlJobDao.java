package org.verapdf.crawler.repository.jobs;

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

    private final JdbcTemplate template;

    public CrawlJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public List<CurrentJob> getAllJobs() {
        return template.query("select * from crawl_jobs", new CrawlJobMapper());
    }

    public void addJob(CurrentJob job) {
        template.update("insert into crawl_jobs (id, crawl_url, job_url, crawl_since, status, report_email) values (?,?,?,?,'active', ?)"
                , job.getId(), job.getCrawlURL(), job.getJobURL(), job.getCrawlSinceTime(), job.getReportEmail());
    }

    public void removeJob(CurrentJob job) {
        template.update("delete from crawl_jobs where id=?", job.getId());
    }

    public void writeFinishTime(CurrentJob job) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        template.update("update crawl_jobs set finish_time=?, is_finished=? where id=?",
                sdfDate.format(now), true, job.getId());
    }

    public CurrentJob getCrawlJob(String jobId) {
        return template.query("select * from crawl_jobs where id=?", new CrawlJobMapper(), jobId).get(0);
    }

    public CurrentJob getCrawlJobByCrawlUrl(String url) {
        return template.query("select * from crawl_jobs where crawl_url=?", new CrawlJobMapper(), url).get(0);
    }

    public String getCrawlUrl(String jobId) {
        return template.queryForObject("select crawl_url from crawl_jobs where id=?", new Object[] {jobId}, String.class);
    }

    public LocalDateTime getCrawlSince(String jobId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(template.queryForObject("select crawl_since from crawl_jobs where id=?", new Object[] {jobId}, String.class), formatter);
    }

    public boolean doesJobExist(String url) {
        Integer count = template.queryForObject("select count(*) from crawl_jobs where crawl_url=?", Integer.class, url);
        return count != null && count != 0;
    }

    public void setJobFinished(String jobId, boolean isFinished) {
        template.update("update crawl_jobs set is_finished=? where id=?", isFinished, jobId);
    }

    public void setStatus(String jobId, String status) {
        template.update("update crawl_jobs set status=? where id=?", status, jobId);
    }

    public void setReportEmail(String jobId, String emailAddress) {
        template.update("update crawl_jobs set report_email=? where id=?", emailAddress, jobId);
    }

    public void setJobUrl(String jobId, String jobUrl) {
        template.update("update crawl_jobs set job_url=? where id=?", jobUrl, jobId);
    }
}

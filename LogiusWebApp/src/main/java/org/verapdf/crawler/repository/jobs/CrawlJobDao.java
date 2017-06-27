package org.verapdf.crawler.repository.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.repository.mappers.CrawlJobMapper;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CrawlJobDao {

    private final JdbcTemplate template;

    public CrawlJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public List<CurrentJob> getAllJobs() {
        String sql = "select * from crawl_jobs";
        return template.query(sql, new CrawlJobMapper());
    }

    public void addJob(CurrentJob job) {
        String sql = "insert into crawl_jobs (id, crawl_url, job_url, crawl_since) values (?,?,?,?)";
        template.update(sql, job.getId(), job.getCrawlURL(), job.getJobURL(), job.getCrawlSinceTime());
    }

    public void removeJob(CurrentJob job) {
        String sql = "delete from crawl_jobs where id=?";
        template.update(sql, job.getId());
    }

    public void writeFinishTime(CurrentJob job) {
        String sql = "update crawl_jobs set finish_time=? where id=?";
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        template.update(sql, sdfDate.format(now), job.getId());
    }
}

package org.verapdf.crawler.repository.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.DaoUtils;
import org.verapdf.crawler.repository.mappers.ValidationJobMapper;

import javax.sql.DataSource;
import java.util.List;

public class ValidationJobDao {
    private final JdbcTemplate template;

    public ValidationJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public List<ValidationJobData> getAllJobs() {
        String sql = "select * from validation_jobs";
        return template.query(sql, new ValidationJobMapper());
    }

    public void deleteJob(ValidationJobData job) {
        String sql = "delete from validation_jobs where filepath=?";
        template.update(sql, job.getFilepath());
    }

    public void addJob(ValidationJobData job) {
        String sql = "insert into validation_jobs (filepath, job_directory, file_url, time_last_modified) values (?,?,?,?)";
        template.update(sql, job.getFilepath(), job.getJobDirectory(), job.getUri(), DaoUtils.getSqlTimeFromLastmodified(job.getTime()));
    }
}

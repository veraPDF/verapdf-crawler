package org.verapdf.crawler.repository.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.DaoUtils;
import org.verapdf.crawler.repository.mappers.ValidationJobMapper;

import javax.sql.DataSource;
import java.util.List;

public class ValidationJobDao {
    private final JdbcTemplate template;
    public static final String VALIDATION_JOB_TABLE_NAME = "validation_jobs";
    public static final String FIELD_FILEPATH = "filepath";
    public static final String FIELD_JOB_DIRECTORY = "job_directory";
    public static final String FIELD_FILE_URL = "file_url";
    public static final String FIELD_LAST_MODIFIED= "time_last_modified";

    public ValidationJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
        template.execute(String.format("CREATE TABLEIF NOT EXISTS `%s` (\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` varchar(255) DEFAULT NULL,\n" +
                "  `%s` datetime DEFAULT NULL\n" +
                ")", VALIDATION_JOB_TABLE_NAME, FIELD_FILEPATH,
                FIELD_JOB_DIRECTORY, FIELD_FILE_URL, FIELD_LAST_MODIFIED));
    }

    public List<ValidationJobData> getAllJobs() {
        return template.query("select * from " + VALIDATION_JOB_TABLE_NAME, new ValidationJobMapper());
    }

    public void deleteJob(ValidationJobData job) {
        template.update(String.format("delete from %s where %s=?", VALIDATION_JOB_TABLE_NAME, FIELD_FILEPATH), job.getFilepath());
    }

    public void addJob(ValidationJobData job) {
        String sql = String.format("insert into %s (%s, %s, %s, %s) values (?,?,?,?)", VALIDATION_JOB_TABLE_NAME, FIELD_FILEPATH, FIELD_JOB_DIRECTORY, FIELD_FILE_URL,FIELD_LAST_MODIFIED);
        template.update(sql, job.getFilepath(), job.getJobDirectory(), job.getUri(), DaoUtils.getSqlTimeFromLastmodified(job.getTime()));
    }
}

package org.verapdf.crawler.db.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.db.DaoUtils;
import org.verapdf.crawler.db.mappers.ValidationJobMapper;

import javax.sql.DataSource;
import java.util.List;

public class ValidationJobDao {
    private static final String TABLE_PDF_VALIDATION_JOBS_QUEUE = "pdf_validation_jobs_queue";
    public static final String FIELD_FILEPATH = "filepath";
    public static final String FIELD_CRAWL_JOB_ID = "heritrix_job_id";
    public static final String FIELD_JOB_DIRECTORY = "job_directory";
    public static final String FIELD_FILE_URL = "document_url";
    public static final String FIELD_LAST_MODIFIED = "time_last_modified";
    private static final String FIELD_VALIDATION_STATUS = "validation_status";
    public static final String STATUS_NOT_STARTED = "not_started";
    private static final String STATUS_IN_PROGRESS = "in_progress";

    private final JdbcTemplate template;

    public ValidationJobDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public ValidationJobData getOneJob() {
        List<ValidationJobData> validationJobs = template.query("select * from " + TABLE_PDF_VALIDATION_JOBS_QUEUE, new ValidationJobMapper());
        ValidationJobData result = null;
        if(!validationJobs.isEmpty()) {
            result = validationJobs.get(0);
            template.update(String.format("update %s set %s=? where %s=?",
                    TABLE_PDF_VALIDATION_JOBS_QUEUE, FIELD_VALIDATION_STATUS, FIELD_FILE_URL),
                    STATUS_IN_PROGRESS, result.getUri());
        }
        return result;
    }

    public void deleteJob(ValidationJobData job) {
        template.update(String.format("delete from %s where %s=?", TABLE_PDF_VALIDATION_JOBS_QUEUE, FIELD_FILE_URL), job.getUri());
    }

    public void addJob(ValidationJobData job) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values (?,?,?,?,?)",
                TABLE_PDF_VALIDATION_JOBS_QUEUE, FIELD_FILEPATH, FIELD_CRAWL_JOB_ID, FIELD_JOB_DIRECTORY, FIELD_FILE_URL,FIELD_LAST_MODIFIED),
                job.getFilepath(), job.getJobID(), job.getJobDirectory(), job.getUri(), DaoUtils.getSqlTimeFromLastmodified(job.getTime()));
    }

    public Integer getQueueSize() {
        return template.queryForObject(String.format("select count(*) from %s", TABLE_PDF_VALIDATION_JOBS_QUEUE), Integer.class);
    }
}

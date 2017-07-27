package org.verapdf.crawler.repository.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.domain.validation.ValidationReportData;
import org.verapdf.crawler.repository.DaoUtils;

import javax.sql.DataSource;

public class InsertDocumentDao {
    private final JdbcTemplate template;

    public InsertDocumentDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addInvalidPdfFile(ValidationReportData data, String jobId) {
        String sql = "insert into invalid_pdf_files (file_url, last_modified, passed_rules, failed_rules, valid, crawl_job_id) values(?,?,?,?,?,?)";
        template.update(sql, data.getUrl(), DaoUtils.getSqlTimeFromLastmodified(data.getLastModified()), data.getPassedRules(), data.getFailedRules(), data.isValid(), jobId);
    }

    public void addValidPdfFile(ValidationJobData data, String jobId) {
        String sql = "insert into valid_pdf_files (file_url, last_modified, crawl_job_id) values(?,?,?)";
        template.update(sql, data.getUri(), DaoUtils.getSqlTimeFromLastmodified(data.getTime()), jobId);
    }

    public void addMicrosoftOfficeFile(String fileUrl, String jobId, String lastModified) {
        String sql = "insert into microsoft_office_files (file_url, last_modified, crawl_job_id) values(?,?,?)";
        template.update(sql, fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), jobId);
    }

    public void addOdfFile(String fileUrl, String jobId, String lastModified) {
        String sql = "insert into odf_files (file_url, last_modified, crawl_job_id) values(?,?,?)";
        template.update(sql, fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), jobId);
    }
}

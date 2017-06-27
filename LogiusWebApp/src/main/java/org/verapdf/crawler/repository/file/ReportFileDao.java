package org.verapdf.crawler.repository.file;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.report.PDFValidationStatistics;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.domain.validation.ValidationReportData;
import org.verapdf.crawler.repository.mappers.FileUrlMapper;
import org.verapdf.crawler.repository.mappers.ValidationReportDataMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportFileDao {
    private final JdbcTemplate template;

    public ReportFileDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public PDFValidationStatistics getValidationStatistics(String crawlJobId, LocalDateTime sinceTime) {
        return new PDFValidationStatistics(getNumberOfInvalidFilesForJob(crawlJobId, sinceTime), getNumberOfValidFilesForJob(crawlJobId, sinceTime));
    }

    //<editor-fold desc="Invalid pdf files">
    private Integer getNumberOfInvalidFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select count(*) from invalid_pdf_files where crawl_job_id=? and last_modified>?";
        return template.queryForObject(sql, new Object[]{crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<ValidationReportData> getInvalidPdfFiles(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select * from invalid_pdf_files where crawl_job_id=? and last_modified>?";
        return template.query(sql, new ValidationReportDataMapper(), crawlJobId, getSqlTimeString(sinceTime));
    }
    //</editor-fold>
    //<editor-fold desc="Valid pdf files">
    private Integer getNumberOfValidFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select count(*) from valid_pdf_files where crawl_job_id=? and last_modified>?";
        return template.queryForObject(sql, new Object[] {crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }
    //</editor-fold>
    //<editor-fold desc="ODF files">

    public Integer getNumberOfOdfFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select count(*) from odf_files where crawl_job_id=? and last_modified>?";
        return template.queryForObject(sql, new Object[] {crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getListOfOfficeFiles(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select file_url from microsoft_office_files where crawl_job_id=? and last_modified>?";
        return template.query(sql, new FileUrlMapper(), crawlJobId, sinceTime);
    }

    //</editor-fold>
    //<editor-fold desc="Microsoft office files">

    public Integer getNumberOfMicrosoftFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select count(*) from microsoft_office_files where crawl_job_id=? and last_modified>?";
        return template.queryForObject(sql, new Object[] {crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getMicrosoftOfficeFiles(String crawlJobId, LocalDateTime sinceTime) {
        String sql = "select file_url from microsoft_office_files where crawl_job_id=? and last_modified>?";
        return template.query(sql, new FileUrlMapper(), crawlJobId, getSqlTimeString(sinceTime));
    }

    //</editor-fold>

    private String getSqlTimeString(LocalDateTime time) {
        if(time == null) {
            return "0000-00-00 00:00:00";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }
}

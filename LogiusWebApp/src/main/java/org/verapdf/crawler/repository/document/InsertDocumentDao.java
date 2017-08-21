package org.verapdf.crawler.repository.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.DaoUtils;

import javax.sql.DataSource;

public class InsertDocumentDao {
    final static String DOCUMENTS_TABLE_NAME = "documents";
    final static String FIELD_JOB_ID = "crawl_job_id";
    final static String FIELD_DOCUMENT_URL = "document_url";
    final static String FIELD_LAST_MODIFIED = "last_modified";
    final static String FIELD_DOCUMENT_TYPE = "document_type";
    final static String TYPE_VALID_PDF = "valid_pdf";
    final static String TYPE_INVALID_PDF = "invalid_pdf";
    final static String TYPE_MICROSOFT = "microsoft";
    final static String TYPE_OOXML = "ooxml";
    final static String TYPE_ODF = "odf";

    private final JdbcTemplate template;

    public InsertDocumentDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addInvalidPdfFile(ValidationJobData data, String jobId) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values(?,?,?, '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, FIELD_JOB_ID, FIELD_DOCUMENT_TYPE, TYPE_INVALID_PDF),
                data.getUri(), data.getTime(), jobId);
    }

    public void addPdfFile(ValidationJobData data, String jobId) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values(?,?,?, '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, FIELD_JOB_ID, FIELD_DOCUMENT_TYPE, TYPE_VALID_PDF),
                data.getUri(), data.getTime(), jobId);
    }

    public void addMicrosoftOfficeFile(String fileUrl, String jobId, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values(?,?,?, '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, FIELD_JOB_ID, FIELD_DOCUMENT_TYPE, TYPE_MICROSOFT),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), jobId);
    }

    public void addOdfFile(String fileUrl, String jobId, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values(?,?,?, '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, FIELD_JOB_ID, FIELD_DOCUMENT_TYPE, TYPE_ODF),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), jobId);
    }

    public void addOpenOfficeXMLFile(String fileUrl, String jobId, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s) values(?,?,?, '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, FIELD_JOB_ID, FIELD_DOCUMENT_TYPE, TYPE_OOXML),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), jobId);
    }
}

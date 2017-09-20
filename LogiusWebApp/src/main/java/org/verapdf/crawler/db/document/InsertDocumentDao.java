package org.verapdf.crawler.db.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.db.DaoUtils;

import javax.sql.DataSource;

public class InsertDocumentDao {
    public static final String DOCUMENTS_TABLE_NAME = "documents";
    public static final String CRAWL_JOB_DOMAIN = "crawl_job_domain";
    static final String FIELD_DOCUMENT_URL = "document_url";
    static final String FIELD_LAST_MODIFIED = "last_modified";
    static final String FIELD_DOCUMENT_TYPE = "document_type";
    static final String FIELD_DOCUMENT_STATUS = "document_status";
    static final String TYPE_PDF = "pdf";
    static final String TYPE_MICROSOFT = "microsoft";
    static final String TYPE_OOXML = "ooxml";
    static final String TYPE_ODF = "odf";

    private final JdbcTemplate template;

    public InsertDocumentDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addPdfFile(ValidationJobData data, String domain, TestResultSummary testResultSummary) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', ?)", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_PDF),
                data.getUri(), data.getTime(), domain, testResultSummary.getDataBaseValue());
    }

    public void addMicrosoftOfficeFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_MICROSOFT, TestResultSummary.NOT_OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public void addOdfFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_ODF, TestResultSummary.OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public void addOpenOfficeXMLFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_OOXML, TestResultSummary.NOT_OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public enum TestResultSummary {
        OPEN("open"),
        NOT_OPEN("not_open");

        private String dataBaseValue;

        TestResultSummary(String dataBaseValue) {
            this.dataBaseValue = dataBaseValue;
        }

        public String getDataBaseValue() {
            return dataBaseValue;
        }
    }
}

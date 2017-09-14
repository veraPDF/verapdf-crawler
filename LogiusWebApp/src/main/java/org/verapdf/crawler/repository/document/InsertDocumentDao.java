package org.verapdf.crawler.repository.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.DaoUtils;

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

    public void addPdfFile(ValidationJobData data, String domain, Status status) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', ?)", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_PDF),
                data.getUri(), data.getTime(), domain, status.getDataBaseValue());
    }

    public void addMicrosoftOfficeFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_MICROSOFT, Status.NOT_OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public void addOdfFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_ODF, Status.OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public void addOpenOfficeXMLFile(String fileUrl, String domain, String lastModified) {
        template.update(String.format("insert into %s (%s, %s, %s, %s, %s) values(?,?,?, '%s', '%s')", DOCUMENTS_TABLE_NAME,
                FIELD_DOCUMENT_URL, FIELD_LAST_MODIFIED, CRAWL_JOB_DOMAIN, FIELD_DOCUMENT_TYPE, FIELD_DOCUMENT_STATUS, TYPE_OOXML, Status.NOT_OPEN.getDataBaseValue()),
                fileUrl, DaoUtils.getSqlTimeFromLastmodified(lastModified), domain);
    }

    public enum Status {
        OPEN("open"),
        NOT_OPEN("not_open");

        private String dataBaseValue;

        Status(String dataBaseValue) {
            this.dataBaseValue = dataBaseValue;
        }

        public String getDataBaseValue() {
            return dataBaseValue;
        }
    }
}

package org.verapdf.crawler.db.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.db.mappers.FileUrlMapper;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

public class ReportDocumentDao {
    private final JdbcTemplate template;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public ReportDocumentDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    /*public PDFValidationStatistics getValidationStatistics(String crawlJobId, LocalDateTime sinceTime) {
        try {
            String sql = String.format("select any_value(%s.%s), any_value(%s), count(*) as `number` from ((%s inner join %s on %s.%s=%s.%s) inner join %s on %s.%s=%s.%s) where %s=? and %s>? group by %s",
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.FIELD_PROPERTY_VALUE,
                    ValidatedPDFDao.FIELD_PDF_PROPERTY_NAME, ValidatedPDFDao.PDF_PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.PDF_PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.FIELD_PDF_PROPERTY_NAME, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.FIELD_PROPERTY_NAME, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.FIELD_PROPERTIES_DOCUMENT_URL,
                    InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_URL,
                    InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED, ValidatedPDFDao.FIELD_PROPERTY_VALUE);
            List<PdfPropertyStatistics> statistics = template.query(sql, new PdfPropertyStatisticsMapper(), crawlJobId, sinceTime);
            return new PDFValidationStatistics(statistics, getNumberOfInvalidFilesForJob(crawlJobId, sinceTime),
                    getNumberOfValidFilesForJob(crawlJobId, sinceTime));
        }
        catch (Throwable e) {
            logger.error("Error in validation statistics query",e);
            return new PDFValidationStatistics();
        }
    }*/

    public List<String> getMatchingPropertyValues(String domain, String name, String valueFilter) {
        return template.query(String.format("select %s from %s inner join %s on %s.%s=%s.%s where %s=? and %s=? and %s like ? group by %s",
                ValidatedPDFDao.FIELD_PROPERTY_VALUE, InsertDocumentDao.DOCUMENTS_TABLE_NAME, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_URL, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                ValidatedPDFDao.FIELD_PROPERTIES_DOCUMENT_URL, InsertDocumentDao.CRAWL_JOB_DOMAIN, ValidatedPDFDao.FIELD_PROPERTY_NAME,
                ValidatedPDFDao.FIELD_PROPERTY_VALUE, ValidatedPDFDao.FIELD_PROPERTY_VALUE),
                new Object[]{domain, name, "%" + valueFilter + "%"},
                (resultSet, i) -> resultSet.getString(ValidatedPDFDao.FIELD_PROPERTY_VALUE));
    }

//    public List<DomainDocument> getDomainDocuments(String jobId, String startDate,
//                                                   String type, Integer start, Integer limit, List<String> properties) {
//        /* Replace ? with property for all properties in list
//        SELECT docs.domain, docs.document_type, docs.document_status, ?.propery_name, ?.property_value, err.description FROM crawl_jobs AS jobs
//	        INNER JOIN documents AS docs ON jobs.id=docs.crawl_job_id
//	        INNER JOIN documents_validation_errors ON docs.domain=documents_validation_errors.document_url
//	        INNER JOIN validation_errors AS err ON documents_validation_errors.error_id=err.id
//	        INNER JOIN document_properties AS ? ON docs.domain=?.document_url AND ?.property_name=?
//        where jobs.id="" AND docs.last_modified>"2015-01-01" AND docs.document_type=""
//        LIMIT ?, ?
//         */
//        return null;
//    }

    //<editor-fold desc="Invalid pdf files">
    public Integer getNumberOfInvalidPdfFiles(String domain, Date sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_DOCUMENT_STATUS,
                InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_PDF, InsertDocumentDao.Status.NOT_OPEN.getDataBaseValue(), domain, sinceTime}, Integer.class);
    }

    public List<String> getInvalidPdfFiles(String domain, Date sinceTime) {
        return template.query(String.format("select %s from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.FIELD_DOCUMENT_STATUS, InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_PDF, InsertDocumentDao.Status.NOT_OPEN.getDataBaseValue(), domain, sinceTime);
    }
    //</editor-fold>
    //<editor-fold desc="Valid pdf files">
    public Integer getNumberOfValidFilesForJob(String domain, Date sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_DOCUMENT_STATUS,
                InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_PDF, InsertDocumentDao.Status.OPEN.getDataBaseValue(), domain, sinceTime}, Integer.class);
    }
    //</editor-fold>
    //<editor-fold desc="ODF files">

    public Integer getNumberOfOdfFilesForJob(String domain, Date sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_ODF, domain, sinceTime}, Integer.class);
    }

    public List<String> getListOfODFFiles(String domain, Date sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_ODF, domain, sinceTime);
    }

    //</editor-fold>
    //<editor-fold desc="Microsoft office files">

    public Integer getNumberOfMicrosoftFilesForJob(String domain, Date sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_MICROSOFT, domain, sinceTime}, Integer.class);
    }

    public List<String> getMicrosoftOfficeFiles(String domain, Date sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_MICROSOFT, domain, sinceTime);
    }

    public Integer getNumberOfOoxmlFilesForJob(String domain, Date sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_OOXML, domain, sinceTime}, Integer.class);
    }

    public List<String> getOoxmlFiles(String domain, Date sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.CRAWL_JOB_DOMAIN, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_OOXML, domain, sinceTime);
    }
}

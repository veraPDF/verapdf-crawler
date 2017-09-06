package org.verapdf.crawler.repository.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.report.PDFValidationStatistics;
import org.verapdf.crawler.domain.report.PdfPropertyStatistics;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.mappers.FileUrlMapper;
import org.verapdf.crawler.repository.mappers.PdfPropertyStatisticsMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportDocumentDao {
    private final JdbcTemplate template;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public ReportDocumentDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    //select any_value(document_properties.value), any_value(human_readable_name), count(*) as `num` from
    // ((pdf_properties inner join document_properties on pdf_properties.name=document_properties.name)
    // inner join documents on document_properties.document_url = documents.document_url) group by value;

    public PDFValidationStatistics getValidationStatistics(String crawlJobId, LocalDateTime sinceTime) {
        try {
            String sql = String.format("select any_value(%s.%s), any_value(%s), count(*) as `number` from ((%s inner join %s on %s.%s=%s.%s) inner join %s on %s.%s=%s.%s) where %s=? and %s>? group by %s",
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.FIELD_PROPERTY_VALUE,
                    ValidatedPDFDao.FIELD_PDF_PROPERTY_NAME, ValidatedPDFDao.PDF_PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.PDF_PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.FIELD_PDF_PROPERTY_NAME, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                    ValidatedPDFDao.FIELD_PROPERTY_NAME, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                    ValidatedPDFDao.PROPERTIES_TABLE_NAME, ValidatedPDFDao.FIELD_PROPERTIES_DOCUMENT_URL,
                    InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_URL,
                    InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED, ValidatedPDFDao.FIELD_PROPERTY_VALUE);
            List<PdfPropertyStatistics> statistics = template.query(sql, new PdfPropertyStatisticsMapper(), crawlJobId, sinceTime);
            return new PDFValidationStatistics(statistics, getNumberOfInvalidFilesForJob(crawlJobId, sinceTime),
                    getNumberOfValidFilesForJob(crawlJobId, sinceTime));
        }
        catch (Throwable e) {
            logger.error("Error in validation statistics query",e);
            return new PDFValidationStatistics();
        }
    }

    public List<String> getMatchingPropertyValues(String crawlJobId, String name, String valueFilter) {
        return template.query(String.format("select %s from %s inner join %s on %s.%s=%s.%s where %s=? and %s=? and %s like ? group by %s",
                ValidatedPDFDao.FIELD_PROPERTY_VALUE, InsertDocumentDao.DOCUMENTS_TABLE_NAME, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_URL, ValidatedPDFDao.PROPERTIES_TABLE_NAME,
                ValidatedPDFDao.FIELD_PROPERTIES_DOCUMENT_URL, InsertDocumentDao.FIELD_JOB_ID, ValidatedPDFDao.FIELD_PROPERTY_NAME,
                ValidatedPDFDao.FIELD_PROPERTY_VALUE, ValidatedPDFDao.FIELD_PROPERTY_VALUE),
                new Object[]{crawlJobId, name, "%" + valueFilter + "%"},
                (resultSet, i) -> resultSet.getString(ValidatedPDFDao.FIELD_PROPERTY_VALUE));
    }

    //<editor-fold desc="Invalid pdf files">
    public Integer getNumberOfInvalidFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_DOCUMENT_STATUS,
                InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_PDF, InsertDocumentDao.STATUS_NOT_OPEN, crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getInvalidPdfFiles(String crawlJobId, LocalDateTime sinceTime) {
        return template.query(String.format("select %s from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.FIELD_DOCUMENT_STATUS, InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_PDF, InsertDocumentDao.STATUS_NOT_OPEN, crawlJobId, getSqlTimeString(sinceTime));
    }
    //</editor-fold>
    //<editor-fold desc="Valid pdf files">
    public Integer getNumberOfValidFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_DOCUMENT_STATUS,
                InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_PDF, InsertDocumentDao.STATUS_OPEN, crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }
    //</editor-fold>
    //<editor-fold desc="ODF files">

    public Integer getNumberOfOdfFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_ODF, crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getListOfODFFiles(String crawlJobId, LocalDateTime sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_ODF, crawlJobId, getSqlTimeString(sinceTime));
    }

    //</editor-fold>
    //<editor-fold desc="Microsoft office files">

    public Integer getNumberOfMicrosoftFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_MICROSOFT, crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getMicrosoftOfficeFiles(String crawlJobId, LocalDateTime sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_MICROSOFT, crawlJobId, getSqlTimeString(sinceTime));
    }

    public Integer getNumberOfOoxmlFilesForJob(String crawlJobId, LocalDateTime sinceTime) {
        return template.queryForObject(String.format("select count(*) from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.DOCUMENTS_TABLE_NAME, InsertDocumentDao.FIELD_DOCUMENT_TYPE,
                InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new Object[] {InsertDocumentDao.TYPE_OOXML, crawlJobId, getSqlTimeString(sinceTime)}, Integer.class);
    }

    public List<String> getOoxmlFiles(String crawlJobId, LocalDateTime sinceTime) {
        return template.query( String.format("select %s from %s where %s=? and %s=? and %s>?",
                InsertDocumentDao.FIELD_DOCUMENT_URL, InsertDocumentDao.DOCUMENTS_TABLE_NAME,
                InsertDocumentDao.FIELD_DOCUMENT_TYPE, InsertDocumentDao.FIELD_JOB_ID, InsertDocumentDao.FIELD_LAST_MODIFIED),
                new FileUrlMapper(), InsertDocumentDao.TYPE_OOXML, crawlJobId, getSqlTimeString(sinceTime));
    }

    private String getSqlTimeString(LocalDateTime time) {
        if(time == null) {
            return "0000-00-00 00:00:00";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }
}

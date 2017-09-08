package org.verapdf.crawler.repository.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.report.ErrorStatistics;
import org.verapdf.crawler.domain.validation.ValidationError;

import javax.sql.DataSource;
import java.util.*;

public class ValidatedPDFDao {
    private static final String VALIDATION_ERRORS_TABLE_NAME = "validation_errors";
    private static final String VALIDATION_ERRORS_REFERENCE_TABLE_NAME = "documents_validation_errors";
    static final String PROPERTIES_TABLE_NAME = "document_properties";
    private static final String PDF_PROPERTIES_TABLE_NAME = "pdf_properties";
    private final String PDF_PROPERTIES_NAMESPACES_TABLE_NAME = "pdf_properties_namespaces";
    private static final String FIELD_SPECIFICATION = "specification";
    private static final String FIELD_CLAUSE = "clause";
    private static final String FIELD_TEST_NUMBER = "test_number";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_ID = "id";
    private static final String FIELD_ERRORS_DOCUMENT_URL = "document_url";
    private static final String FIELD_ERROR_ID = "error_id";
    static final String FIELD_PROPERTY_NAME = "property_name";
    static final String FIELD_PROPERTY_VALUE = "property_value";
    static final String FIELD_PROPERTIES_DOCUMENT_URL = "document_url";
    private static final String FIELD_PDF_PROPERTY_NAME = "property_name";
    private static final String FIELD_PDF_PROPERTY_XPATH_INDEX = "xpath_index";
    private static final String FIELD_PDF_PROPERTY_XPATH = "xpath";
    private static final String FIELD_NAMESPACE_PREFIX = "namespace_prefix";
    private static final String FIELD_NAMESPACE_URL = "namespace_url";

    private static final String PROPERTY_PRODUCER = "pdf_producer";
    private static final String PROPERTY_VERSION = "pdf_specification";
    private static final String PROPERTY_FLAVOR = "pdf_flavor";

    private final JdbcTemplate template;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public ValidatedPDFDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addErrorToDocument(ValidationError rule, String documentUrl) {
		if (rule.isRuleBasedError()) {
			addRuleError(rule, documentUrl);
		} else {
			addProcessingError(rule.getDescription(), documentUrl);
		}
    }

    public void insertPropertyForDocument(String propertyName, String propertyValue, String documentUrl) {
        if(propertyValue != null) {
            logger.info("Inserting property " + propertyName + ":" + propertyValue + " for " + documentUrl);
            template.update(String.format("insert into %s (%s, %s, %s) values (?, ?, ?)",
                    PROPERTIES_TABLE_NAME, FIELD_PROPERTY_NAME, FIELD_PROPERTY_VALUE, FIELD_PROPERTIES_DOCUMENT_URL),
                    propertyName, propertyValue, documentUrl);
        }
    }

    private String getPropertyForDocument(String propertyName, String documentUrl) {
        return template.queryForObject(String.format("select %s from %s where %s=? and %s=?",
                FIELD_PROPERTY_VALUE, PROPERTIES_TABLE_NAME, FIELD_PROPERTIES_DOCUMENT_URL, FIELD_PROPERTY_NAME),
                new Object[]{documentUrl, propertyName}, String.class);
    }

    private void addRuleError(ValidationError rule, String documentUrl) {
		List<String> errors = template.query(String.format("select %s from %s where %s=? and %s=? and %s=?",
				FIELD_ID, VALIDATION_ERRORS_TABLE_NAME, FIELD_SPECIFICATION, FIELD_CLAUSE, FIELD_TEST_NUMBER),
				(resultSet, i) -> resultSet.getString(FIELD_ID), rule.getSpecification(), rule.getClause(), rule.getTestNumber());
		if(errors.isEmpty()) { // There is no such error in database, need to insert and link to document
			template.update(String.format("insert into %s (%s, %s, %s, %s) values (?, ?, ?, ?)", VALIDATION_ERRORS_TABLE_NAME,
					FIELD_SPECIFICATION, FIELD_CLAUSE, FIELD_TEST_NUMBER, FIELD_DESCRIPTION),
					rule.getSpecification(), rule.getClause(), rule.getTestNumber(), rule.getDescription());

			String newRuleId = template.query(String.format("select %s from %s where %s=? and %s=? and %s=?",
					FIELD_ID, VALIDATION_ERRORS_TABLE_NAME, FIELD_SPECIFICATION, FIELD_CLAUSE, FIELD_TEST_NUMBER),
					(resultSet, i) -> resultSet.getString(FIELD_ID), rule.getSpecification(), rule.getClause(), rule.getTestNumber()).get(0);

			template.update(String.format("insert into %s (%s, %s) values (?, ?)", VALIDATION_ERRORS_REFERENCE_TABLE_NAME,
					FIELD_ERRORS_DOCUMENT_URL, FIELD_ERROR_ID), documentUrl, newRuleId);
		}
		else { // Error record already exists, only link it to document
			template.update(String.format("insert into %s (%s, %s) values (?, ?)", VALIDATION_ERRORS_REFERENCE_TABLE_NAME,
					FIELD_ERRORS_DOCUMENT_URL, FIELD_ERROR_ID), documentUrl, errors.get(0));
		}
	}

    private void addProcessingError(String processingError, String documentUrl) {
        List<String> errors = template.query(String.format("select %s from %s where %s is null and %s is null and %s is null and %s=?",
                FIELD_ID, VALIDATION_ERRORS_TABLE_NAME, FIELD_SPECIFICATION, FIELD_CLAUSE, FIELD_TEST_NUMBER, FIELD_DESCRIPTION),
                (resultSet, i) -> resultSet.getString(FIELD_ID), processingError);
        if(errors.isEmpty()) { // There is no such error in database, need to insert and link to document
            template.update(String.format("insert into %s (%s) values (?)", VALIDATION_ERRORS_TABLE_NAME, FIELD_DESCRIPTION), processingError);

            String newRuleId = template.query(String.format("select %s from %s where %s is null and %s is null and %s is null and %s=?",
                    FIELD_ID, VALIDATION_ERRORS_TABLE_NAME, FIELD_SPECIFICATION, FIELD_CLAUSE, FIELD_TEST_NUMBER, FIELD_DESCRIPTION),
                    (resultSet, i) -> resultSet.getString(FIELD_ID), processingError).get(0);

            template.update(String.format("insert into %s (%s, %s) values (?, ?)", VALIDATION_ERRORS_REFERENCE_TABLE_NAME,
                    FIELD_ERRORS_DOCUMENT_URL, FIELD_ERROR_ID), documentUrl, newRuleId);
        }
        else { // Error record already exists, only link it to document
            template.update(String.format("insert into %s (%s, %s) values (?, ?)", VALIDATION_ERRORS_REFERENCE_TABLE_NAME,
                    FIELD_ERRORS_DOCUMENT_URL, FIELD_ERROR_ID), documentUrl, errors.get(0));
        }
    }

    public ErrorStatistics getErrorStatistics(String jobId, String startDate, String flavor, String version, String producer) {
        String sql = "SELECT " + VALIDATION_ERRORS_TABLE_NAME + "." + FIELD_DESCRIPTION + ", COUNT(doc." + InsertDocumentDao.FIELD_DOCUMENT_URL +") AS `doc_count` \n" +
                "FROM " + VALIDATION_ERRORS_REFERENCE_TABLE_NAME + "AS err\n" +
                "     INNER JOIN " + InsertDocumentDao.DOCUMENTS_TABLE_NAME + "AS doc ON err." + FIELD_ERRORS_DOCUMENT_URL + "=doc." + InsertDocumentDao.FIELD_DOCUMENT_URL + " \n" +
                "     INNER JOIN " + VALIDATION_ERRORS_TABLE_NAME + " ON err." + FIELD_ERROR_ID + "=" + VALIDATION_ERRORS_TABLE_NAME + "." + FIELD_ID + " \n";
        if(flavor != null && !flavor.isEmpty()) {
            sql += "INNER JOIN " + PROPERTIES_TABLE_NAME + " AS flavor ON doc." + InsertDocumentDao.FIELD_DOCUMENT_URL + "=flavor." + FIELD_PROPERTIES_DOCUMENT_URL + " AND flavor." + FIELD_PROPERTY_NAME + "=?";
        }
        if(version != null && !version.isEmpty()) {
            sql += "INNER JOIN " + PROPERTIES_TABLE_NAME + " AS version ON doc." + InsertDocumentDao.FIELD_DOCUMENT_URL + "=version." + FIELD_PROPERTIES_DOCUMENT_URL + " AND version." + FIELD_PROPERTY_NAME + "=?";
        }
        if(producer != null && !producer.isEmpty()) {
            sql += "INNER JOIN " + PROPERTIES_TABLE_NAME + " AS producer ON doc." + InsertDocumentDao.FIELD_DOCUMENT_URL + "=producer." + FIELD_PROPERTIES_DOCUMENT_URL + " AND producer." + FIELD_PROPERTY_NAME + "=?";
        }
        sql += "WHERE doc." + InsertDocumentDao.FIELD_JOB_ID + "=? AND doc." + InsertDocumentDao.FIELD_LAST_MODIFIED + ">? ";
        if(flavor != null && !flavor.isEmpty()) {
            sql += "AND flavor." + FIELD_PROPERTY_VALUE + "=? ";
        }
        if(version != null && !version.isEmpty()) {
            sql += "AND version." + FIELD_PROPERTY_VALUE + "=? ";
        }
        if(producer != null && !producer.isEmpty()) {
            sql += "AND producer." + FIELD_PROPERTY_VALUE + " LIKE ?";
        }
        sql += "GROUP BY " + VALIDATION_ERRORS_TABLE_NAME + "." + FIELD_DESCRIPTION + "\n" +
                "ORDER BY doc_count DESC\n" +
                "LIMIT 10";
        return new ErrorStatistics(template.query(sql, (resultSet, i) -> new ErrorStatistics.ErrorCount(resultSet.getString(VALIDATION_ERRORS_TABLE_NAME + "." + FIELD_DESCRIPTION), resultSet.getInt("doc_count")),
                PROPERTY_FLAVOR, PROPERTY_VERSION, PROPERTY_PRODUCER, jobId, startDate, flavor, version, producer));
    }

    public Map<String, String> getNamespaceMap() {
        List<Map<String, Object>> results = template.queryForList(String.format("select %s, %s from %s",
                FIELD_NAMESPACE_PREFIX, FIELD_NAMESPACE_URL, PDF_PROPERTIES_NAMESPACES_TABLE_NAME));
        Map<String, String> result = new HashMap<>();
        for(Map entry: results) {
            result.put(entry.get(FIELD_NAMESPACE_PREFIX).toString(), entry.get(FIELD_NAMESPACE_URL).toString());
        }
        return result;
    }

	public Map<String, List<String>> getPdfPropertiesWithXpath() {
		List<PropertyRow> results = template.query(String.format("select %s, %s, %s from %s",
				FIELD_PDF_PROPERTY_NAME, FIELD_PDF_PROPERTY_XPATH_INDEX, FIELD_PDF_PROPERTY_XPATH, PDF_PROPERTIES_TABLE_NAME),
				(resultSet, i) -> new PropertyRow(resultSet.getString(FIELD_PDF_PROPERTY_NAME),
						resultSet.getInt(FIELD_PDF_PROPERTY_XPATH_INDEX),
						resultSet.getString(FIELD_PDF_PROPERTY_XPATH)));
		results.sort(Comparator.comparingInt(row -> row.xpath_index));
		Map<String, List<String>> result = new HashMap<>();
		for(PropertyRow row : results) {
			List<String> propertyList = result.computeIfAbsent(row.propertyName, k -> new ArrayList<>());
			propertyList.add(row.xpath);
		}
		return result;
	}

    private static class PropertyRow {
    	private String propertyName;
    	private int xpath_index;
    	private String xpath;

		private PropertyRow(String propertyName, int xpath_index, String xpath) {
			this.propertyName = propertyName;
			this.xpath_index = xpath_index;
			this.xpath = xpath;
		}
	}
}

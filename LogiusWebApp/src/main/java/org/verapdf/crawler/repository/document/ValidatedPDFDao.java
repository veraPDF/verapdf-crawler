package org.verapdf.crawler.repository.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.verapdf.crawler.domain.validation.ValidationError;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidatedPDFDao {
    private final static String VALIDATION_ERRORS_TABLE_NAME = "validation_errors";
    private final static String VALIDATION_ERRORS_REFERENCE_TABLE_NAME = "documents_validation_errors";
    public final static String PROPERTIES_TABLE_NAME = "document_properties";
    final static String PDF_PROPERTIES_TABLE_NAME = "pdf_properties";
    private final String PDF_PROPERTIES_NAMESPACES_TABLE_NAME = "pdf_properties_namespaces";
    private final static String FIELD_SPECIFICATION = "specification";
    private final static String FIELD_CLAUSE = "clause";
    private final static String FIELD_TEST_NUMBER = "test_number";
    private final static String FIELD_DESCRIPTION = "description";
    private final static String FIELD_ID = "id";
    private final static String FIELD_ERRORS_DOCUMENT_URL = "document_url";
    private final static String FIELD_ERROR_ID = "error_id";
    final static String FIELD_PROPERTY_NAME = "property_name";
    public final static String FIELD_PROPERTY_VALUE = "property_value";
    final static String FIELD_PROPERTIES_DOCUMENT_URL = "document_url";
    final static String FIELD_PDF_PROPERTY_NAME = "property_name";
    private final static String FIELD_PDF_PROPERTY_XPATH = "xpath";
    private final static String FIELD_NAMESPACE_PREFIX = "namespace_prefix";
    private final static String FIELD_NAMESPACE_URL = "namespace_url";

    private final JdbcTemplate template;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    public ValidatedPDFDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void addErrorToDocument(ValidationError rule, String documentUrl) {
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

    public void addProcessingError(String processingError, String documentUrl) {
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

    public Map<String, String> getPdfPropertiesWithXpath() {
        List<Map<String, Object>> results = template.queryForList(String.format("select %s, %s from %s",
                FIELD_PDF_PROPERTY_NAME, FIELD_PDF_PROPERTY_XPATH, PDF_PROPERTIES_TABLE_NAME));
        Map<String, String> result = new HashMap<>();
        for(Map entry: results) {
            result.put(entry.get(FIELD_PDF_PROPERTY_NAME).toString(), entry.get(FIELD_PDF_PROPERTY_XPATH).toString());
        }
        return result;
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
}

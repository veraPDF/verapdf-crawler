package org.verapdf.crawler.logius.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.validation.error.ValidationError;

import java.util.*;

public class VeraPDFValidationResult {
    private DomainDocument.BaseTestResult testResult = DomainDocument.BaseTestResult.NOT_OPEN;
    private List<ValidationError> validationErrors = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();

    public VeraPDFValidationResult() {
    }

    public VeraPDFValidationResult(String validationErrorMessage) {
        this.validationErrors.add(new ValidationError(validationErrorMessage));
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors == null ? Collections.emptyList() : new ArrayList<>(validationErrors);
    }

    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
    }

    public DomainDocument.BaseTestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(DomainDocument.BaseTestResult testResult) {
        this.testResult = testResult;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties == null ? Collections.emptyMap() : new HashMap<>(properties);
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }
}

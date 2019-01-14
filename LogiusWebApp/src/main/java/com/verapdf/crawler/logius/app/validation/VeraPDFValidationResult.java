package com.verapdf.crawler.logius.app.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.verapdf.crawler.logius.app.document.DomainDocument;
import com.verapdf.crawler.logius.app.validation.error.ValidationError;

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

    @JsonProperty
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    @JsonProperty
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors == null ? Collections.emptyList() : new ArrayList<>(validationErrors);
    }

    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
    }

    @JsonProperty
    public DomainDocument.BaseTestResult getTestResult() {
        return testResult;
    }

    @JsonProperty
    public void setTestResult(DomainDocument.BaseTestResult testResult) {
        this.testResult = testResult;
    }

    @JsonProperty
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonProperty
    public void setProperties(Map<String, String> properties) {
        this.properties = properties == null ? Collections.emptyMap() : new HashMap<>(properties);
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }
}

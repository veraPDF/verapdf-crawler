package org.verapdf.crawler.domain.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationSettings {
    private Map<String, List<String>> validationProperties = new HashMap<>();
    private Map<String, String> namespaces = new HashMap<>();

    public ValidationSettings(Map<String, List<String>> validationProperties, Map<String, String> namespaces) {
        this.validationProperties = validationProperties;
        this.namespaces = namespaces;
    }

    public ValidationSettings() {
    }

    @JsonProperty("properties")
    public Map<String, List<String>> getValidationProperties() {
        return validationProperties;
    }

    @JsonProperty("properties")
    public void setValidationProperties(Map<String, List<String>> validationProperties) {
        this.validationProperties = validationProperties;
    }

    @JsonProperty
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    @JsonProperty
    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }
}

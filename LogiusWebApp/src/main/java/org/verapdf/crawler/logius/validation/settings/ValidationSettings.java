package org.verapdf.crawler.logius.validation.settings;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationSettings {
    private Map<String, List<String>> properties = new HashMap<>();
    private Map<String, String> namespaces = new HashMap<>();

    public ValidationSettings() {
    }

    public ValidationSettings(Map<String, List<String>> properties, Map<String, String> namespaces) {
        this.properties = properties;
        this.namespaces = namespaces;
    }

    public Map<String, List<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, List<String>> properties) {
        this.properties = properties;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }
}

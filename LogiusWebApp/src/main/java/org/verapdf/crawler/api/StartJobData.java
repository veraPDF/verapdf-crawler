package org.verapdf.crawler.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartJobData {
    private String domain;
    private String date;

    public StartJobData() {
        // Jackson deserialization
    }

    public StartJobData(String domain, String date) {
        this.date = date;
        this.domain = domain;
    }

    @JsonProperty
    public String getDomain() {
        return domain;
    }

    @JsonProperty
    public void setDomain(String domain) { this.domain = domain; }

    @JsonProperty
    public String getDate() {
        return date;
    }

    @JsonProperty
    public void setDate(String date) { this.date = date; }
}

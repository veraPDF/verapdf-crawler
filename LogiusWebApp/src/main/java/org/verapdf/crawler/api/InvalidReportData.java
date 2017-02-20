package org.verapdf.crawler.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvalidReportData {
    private String url;
    private String lastModified;
    private Integer passedRules;
    private Integer failedRules;

    public InvalidReportData() {}

    @JsonProperty
    public String getUrl() {
        return url;
    }

    @JsonProperty
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty
    public String getLastModified() {
        return lastModified;
    }

    @JsonProperty
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @JsonProperty
    public Integer getPassedRules() {
        return passedRules;
    }

    @JsonProperty
    public void setPassedRules(Integer passedRules) {
        this.passedRules = passedRules;
    }

    @JsonProperty
    public Integer getFailedRules() {
        return failedRules;
    }

    @JsonProperty
    public void setFailedRules(Integer failedRules) {
        this.failedRules = failedRules;
    }
}

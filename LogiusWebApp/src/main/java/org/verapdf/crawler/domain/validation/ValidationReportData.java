package org.verapdf.crawler.domain.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.processor.reports.ValidationReport;

import java.util.Map;

public class ValidationReportData {
    private String url;
    private String lastModified;
    private Integer passedRules;
    private Integer failedRules;

    @JsonIgnore
    private boolean isValid;

    public ValidationReportData() {}

    public ValidationReportData(ValidationReport report) {
        failedRules = report.getDetails().getFailedRules();
        passedRules = report.getDetails().getPassedRules();
        isValid = (failedRules == 0);
    }

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

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }
}

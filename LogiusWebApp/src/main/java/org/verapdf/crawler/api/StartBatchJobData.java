package org.verapdf.crawler.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartBatchJobData {

    private String[] domains;
    private String date;
    private String reportEmail;

    @JsonProperty
    public String[] getDomains() {
        return domains;
    }

    @JsonProperty
    public void setDomains(String[] domains) {
        this.domains = domains;
    }

    @JsonProperty
    public String getDate() {
        return date;
    }

    @JsonProperty
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty
    public String getReportEmail() {
        return reportEmail;
    }

    @JsonProperty
    public void setReportEmail(String reportEmail) {
        this.reportEmail = reportEmail;
    }

    public StartBatchJobData() {

    }
}

package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartBatchJobData {

    private String[] domains;
    private String date;
    private String reportEmail;
    private boolean doOverwrite;

    @JsonProperty
    public boolean isDoOverwrite() {
        return doOverwrite;
    }

    @JsonProperty
    public void setDoOverwrite(boolean doOverwrite) {
        this.doOverwrite = doOverwrite;
    }

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

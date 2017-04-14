package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.domain.report.ValidationError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CurrentJob {
    private String id;

    private String jobURL;
    private String crawlURL;
    private String reportEmail;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String status;
    private Map<ValidationError, Integer> errorOccurances;

    private boolean isEmailSent;

    private LocalDateTime crawlSinceTime;

    public CurrentJob(String id, String jobURL, String crawlURL, LocalDateTime time,
                      String reportEmail, LocalDateTime startTime) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
        this.crawlSinceTime = time;
        this.reportEmail = reportEmail;
        this.isEmailSent = false;
        this.startTime = startTime;
        errorOccurances = new HashMap<>();
    }

    @JsonIgnore
    public Map<ValidationError, Integer> getErrorOccurances() {
        return errorOccurances;
    }

    @JsonIgnore
    public void setErrorOccurances(Map<ValidationError, Integer> errorOccurances) {
        this.errorOccurances = errorOccurances;
    }

    public boolean isActiveJob() {
        return jobURL.equals("");
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    public String getJobURL() {
        return jobURL;
    }

    public void setJobURL(String jobURL) {
        this.jobURL = jobURL;
    }

    @JsonProperty
    public String getCrawlURL() {
        return crawlURL;
    }

    public LocalDateTime getCrawlSinceTime() { return crawlSinceTime; }

    public void setCrawlSinceTime(LocalDateTime crawlSinceTime) { this.crawlSinceTime = crawlSinceTime; }

    public String getReportEmail() {
        return reportEmail;
    }

    public void setReportEmail(String reportEmail) {
        this.reportEmail = reportEmail;
    }

    public boolean isEmailSent() {
        return isEmailSent;
    }

    public void setEmailSent(boolean emailSent) {
        isEmailSent = emailSent;
    }

    @JsonProperty
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @JsonProperty
    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

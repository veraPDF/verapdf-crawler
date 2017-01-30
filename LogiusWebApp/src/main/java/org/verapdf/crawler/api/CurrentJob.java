package org.verapdf.crawler.api;

import java.time.LocalDateTime;

public class CurrentJob {
    private String id;
    private String jobURL;
    private String crawlURL;
    private String reportEmail;

    private boolean isEmailSent;

    private LocalDateTime crawlSinceTime;

    public CurrentJob(String id, String jobURL, String crawlURL, LocalDateTime time, String reportEmail) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
        this.crawlSinceTime = time;
        this.reportEmail = reportEmail;
        this.isEmailSent = false;
    }

    public String getId() {
        return id;
    }

    public String getJobURL() {
        return jobURL;
    }

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
}

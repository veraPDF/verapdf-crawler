package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Date;

public class CrawlJob {

    private String domain;
    private String id;
    private String jobURL;
    private Date startTime;
    private Date finishTime;
    private String status;

    private boolean isFinished = false;

    public CrawlJob() {
    }

    public CrawlJob(String id, String jobURL, String domain, Date startTime) {
        this.id = id;
        this.jobURL = jobURL;
        this.domain = domain;
        this.startTime = startTime;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    public String getJobURL() {
        return jobURL;
    }

    @JsonProperty
    public String getDomain() {
        return domain;
    }

    @JsonProperty
    public Date getStartTime() {
        return startTime;
    }

    @JsonProperty
    public Date getFinishTime() {
        return finishTime;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty
    public boolean isFinished() {
        return isFinished;
    }

    @JsonProperty
    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

    @JsonProperty
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public void setJobURL(String jobURL) {
        this.jobURL = jobURL;
    }

    @JsonProperty
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @JsonProperty
    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }
}
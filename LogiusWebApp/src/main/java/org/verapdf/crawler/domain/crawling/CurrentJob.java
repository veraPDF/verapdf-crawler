package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class CurrentJob {

    private final String id;
    private String jobURL;
    private final String crawlURL;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String status;

    private boolean isFinished = false;

    public CurrentJob(String id, String jobURL, String crawlURL, LocalDateTime startTime) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
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
    public String getCrawlURL() {
        return crawlURL;
    }

    @JsonProperty
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @JsonProperty
    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    @JsonProperty
    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }
}

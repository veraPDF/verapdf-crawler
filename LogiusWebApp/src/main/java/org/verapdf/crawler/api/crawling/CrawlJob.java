package org.verapdf.crawler.api.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Date;

public class CrawlJob {

    @NotEmpty
    private String domain;

    private String id;
    private String jobURL;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
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
    public String getDomain() {
        return domain;
    }

    @JsonProperty
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getJobURL() {
        return jobURL;
    }

    @JsonProperty
    public void setJobURL(String jobURL) {
        this.jobURL = jobURL;
    }

    @JsonProperty
    public Date getStartTime() {
        return startTime;
    }

    @JsonProperty
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @JsonProperty
    public Date getFinishTime() {
        return finishTime;
    }

    @JsonProperty
    public void setFinishTime(Date finishTime) {
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

    @JsonProperty
    public boolean isFinished() {
        return isFinished;
    }

    @JsonProperty
    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
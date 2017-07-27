package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.domain.report.ValidationError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CurrentJob {
    private final String id;

    private String jobURL;
    private final String crawlURL;
    private String reportEmail;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String status;
    //private Map<ValidationError, Integer> errorOccurances;

    private boolean isFinished = false;

    private LocalDateTime crawlSinceTime;

    public CurrentJob(String id, String jobURL, String crawlURL, LocalDateTime time,
                      String reportEmail, LocalDateTime startTime) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
        this.crawlSinceTime = time;
        this.reportEmail = reportEmail;
        this.startTime = startTime;
        //errorOccurances = new HashMap<>();
    }

    /*@JsonIgnore
    public Map<ValidationError, Integer> getErrorOccurances() {
        return errorOccurances;
    }

    @JsonIgnore
    public void setErrorOccurances(Map<ValidationError, Integer> errorOccurances) {
        this.errorOccurances = errorOccurances;
    }*/

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

    public LocalDateTime getCrawlSinceTime() { return crawlSinceTime; }

    public String getReportEmail() {
        return reportEmail;
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

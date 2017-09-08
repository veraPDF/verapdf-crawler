package org.verapdf.crawler.domain.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CrawlRequest {
    private List<String> crawlJobs;
    private String id;
    private boolean isFinished;
    private final String emailAddress;
    private final LocalDateTime crawlSinceTime;

    public CrawlRequest(String id, String emailAddress, LocalDateTime crawlSinceTime) {
        crawlJobs = new ArrayList<>();
        this.id = id;
        this.emailAddress = emailAddress;
        this.isFinished = false;
        this.crawlSinceTime = crawlSinceTime;
    }

    @JsonProperty
    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd HH:mm:ss")
    public LocalDateTime getCrawlSinceTime() {
        return crawlSinceTime;
    }

    @JsonProperty
    public String getId() { return  id; }

    @JsonProperty
    public void setId(String id) { this.id = id; }

    @JsonProperty
    public List<String> getCrawlJobs() { return crawlJobs; }

    @JsonProperty
    public boolean isFinished() {
        return isFinished;
    }

    @JsonProperty
    public void setFinished() {
        isFinished = true;
    }

    @JsonProperty
    public String getEmailAddress() { return emailAddress; }

    @JsonProperty
    public void setCrawlJobs(List<String> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }
}

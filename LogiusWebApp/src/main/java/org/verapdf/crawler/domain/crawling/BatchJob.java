package org.verapdf.crawler.domain.crawling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BatchJob {
    private List<String> crawlJobs;
    private final String id;
    private boolean isFinished;
    private final String emailAddress;
    private final LocalDateTime crawlSinceTime;

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public LocalDateTime getCrawlSinceTime() {
        return crawlSinceTime;
    }


    public BatchJob(String id, String emailAddress, String crawlSinceTime) {
        crawlJobs = new ArrayList<>();
        this.id = id;
        this.emailAddress = emailAddress;
        this.isFinished = false;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.crawlSinceTime = LocalDateTime.parse(crawlSinceTime, formatter);
    }

    public String getId() { return  id; }

    public List<String> getCrawlJobs() { return crawlJobs; }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished() {
        isFinished = true;
    }

    public String getEmailAddress() { return emailAddress; }

    public void setCrawlJobs(List<String> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }
}

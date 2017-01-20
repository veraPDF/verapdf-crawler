package org.verapdf.crawler.api;

import java.time.LocalDateTime;

public class CurrentJob {
    private String id;
    private String jobURL;
    private String crawlURL;

    private LocalDateTime crawlSinceTime;

    public CurrentJob(String id, String jobURL, String crawlURL, LocalDateTime time) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
        this.crawlSinceTime = time;
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
}

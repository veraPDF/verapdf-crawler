package org.verapdf.crawler.api;

public class CurrentJob {
    private String id;
    private String jobURL;
    private String crawlURL;
    private boolean isActive;

    public boolean isActive() {
        return isActive;
    }

    public CurrentJob(String id, String jobURL, String crawlURL, boolean isActive) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
        this.isActive = isActive;
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
}

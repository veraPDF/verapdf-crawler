package org.verapdf.crawler.api;

public class InactiveJob {
    private String id;
    private String jobURL;
    private String crawlURL;

    public InactiveJob(String id, String jobURL, String crawlURL) {
        this.id = id;
        this.jobURL = jobURL;
        this.crawlURL = crawlURL;
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

package org.verapdf.crawler.domain.crawling;

public class CrawlJobReference {
    private String id;
    private String status;
    private String jobUrl;

    public CrawlJobReference(String id, String status, String jobUrl) {
        this.id = id;
        this.status = status;
        this.jobUrl = jobUrl;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }
}

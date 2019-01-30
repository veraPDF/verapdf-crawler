package org.verapdf.crawler.extention;

import java.util.Date;

public class DomainDocument {

    public static class CrawlJob {
        private String heritrixJobId;

        public CrawlJob() {
        }

        public CrawlJob(String heritrixJobId) {
            this.heritrixJobId = heritrixJobId;
        }

        public String getHeritrixJobId() {
            return heritrixJobId;
        }

        public void setHeritrixJobId(String heritrixJobId) {
            this.heritrixJobId = heritrixJobId;
        }
    }

    private String url;

    private CrawlJob crawlJob;

    private Date lastModified;

    private String contentType;


    public DomainDocument() {
    }

    public DomainDocument(String heritrixJobId, String url) {
        this.crawlJob = new CrawlJob(heritrixJobId);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CrawlJob getCrawlJob() {
        return crawlJob;
    }

    public void setCrawlJob(CrawlJob crawlJob) {
        this.crawlJob = crawlJob;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

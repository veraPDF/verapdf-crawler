package org.verapdf.crawler.logius.model;

import org.verapdf.crawler.logius.crawling.CrawlJob;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
public class DocumentId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "document_id")
    private CrawlJob crawlJob;

    @Column(name = "document_url")
    private String documentUrl;

    public DocumentId() {
    }

    public DocumentId(CrawlJob crawlJob, String documentUrl) {
        this.crawlJob = crawlJob;
        this.documentUrl = documentUrl;
    }

    public CrawlJob getCrawlJob() {
        return crawlJob;
    }

    public void setCrawlJob(CrawlJob crawlJob) {
        this.crawlJob = crawlJob;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}
package org.verapdf.crawler.extention;

import java.util.Date;

public class DomainDocument {

    public DocumentId getDocumentId() {
        return documentId;
    }

    public void setDocumentId(DocumentId documentId) {
        this.documentId = documentId;
    }

    public static class DocumentId {
        private String documentUrl;
        private CrawlJob crawlJob;

        public DocumentId(String documentUrl, CrawlJob crawlJob) {
            this.documentUrl = documentUrl;
            this.crawlJob = crawlJob;
        }

        public String getDocumentUrl() {
            return documentUrl;
        }

        public void setDocumentUrl(String documentUrl) {
            this.documentUrl = documentUrl;
        }

        public CrawlJob getCrawlJob() {
            return crawlJob;
        }

        public void setCrawlJob(CrawlJob crawlJob) {
            this.crawlJob = crawlJob;
        }
    }

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

    private DocumentId documentId;


    private Date lastModified;

    private String contentType;

    public DomainDocument() {
    }

    public DomainDocument(String heritrixJobId, String url) {
        this.documentId = new DocumentId(url, new CrawlJob(heritrixJobId));
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

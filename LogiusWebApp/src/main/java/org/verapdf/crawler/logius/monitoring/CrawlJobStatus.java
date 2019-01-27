package org.verapdf.crawler.logius.monitoring;

import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class CrawlJobStatus {

    private CrawlJob crawlJob;
    private List<CrawlRequest> crawlRequests;
    private HeritrixCrawlJobStatus heritrixStatus;
    private ValidationQueueStatus validationQueueStatus;

    public CrawlJobStatus() {
    }

    public CrawlJobStatus(CrawlJob crawlJob, HeritrixCrawlJobStatus heritrixStatus, ValidationQueueStatus validationQueueStatus) {
        this.crawlJob = crawlJob;
        this.crawlRequests = crawlJob.getCrawlRequests();
        this.heritrixStatus = heritrixStatus;
        this.validationQueueStatus = validationQueueStatus;
    }

    public CrawlJob getCrawlJob() {
        return crawlJob;
    }

    public void setCrawlJob(CrawlJob crawlJob) {
        this.crawlJob = crawlJob;
    }

    public List<CrawlRequest> getCrawlRequests() {
        return crawlRequests;
    }

    public void setCrawlRequests(List<CrawlRequest> crawlRequests) {
        this.crawlRequests = crawlRequests;
    }

    public HeritrixCrawlJobStatus getHeritrixStatus() {
        return heritrixStatus;
    }

    public void setHeritrixStatus(HeritrixCrawlJobStatus heritrixStatus) {
        this.heritrixStatus = heritrixStatus;
    }

    public ValidationQueueStatus getValidationQueueStatus() {
        return validationQueueStatus;
    }

    public void setValidationQueueStatus(ValidationQueueStatus validationQueueStatus) {
        this.validationQueueStatus = validationQueueStatus;
    }
}

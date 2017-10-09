package org.verapdf.crawler.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.api.crawling.CrawlJob;

/**
 * @author Maksim Bezrukov
 */
public class CrawlJobStatus {

	@JsonProperty
	private CrawlJob crawlJob;
	@JsonProperty
	private HeritrixCrawlJobStatus heritrixStatus;
	@JsonProperty
	private ValidationQueueStatus validationQueueStatus;

	public CrawlJobStatus() {
	}

	public CrawlJobStatus(CrawlJob crawlJob, HeritrixCrawlJobStatus heritrixStatus, ValidationQueueStatus validationQueueStatus) {
		this.crawlJob = crawlJob;
		this.heritrixStatus = heritrixStatus;
		this.validationQueueStatus = validationQueueStatus;
	}

	public CrawlJob getCrawlJob() {
		return crawlJob;
	}

	public void setCrawlJob(CrawlJob crawlJob) {
		this.crawlJob = crawlJob;
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

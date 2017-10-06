package org.verapdf.crawler.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.api.crawling.CrawlJob;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class CrawlJobStatus {

	@JsonProperty
	private CrawlJob crawlJob;
	@JsonProperty
	private HeritrixCrawlJobStatus heritrixStatus;
	@JsonProperty
	private List<String> documentsQueue;

	public CrawlJobStatus() {
	}

	public CrawlJobStatus(CrawlJob crawlJob, HeritrixCrawlJobStatus heritrixStatus, List<String> documentsQueue) {
		this.crawlJob = crawlJob;
		this.heritrixStatus = heritrixStatus;
		this.documentsQueue = documentsQueue;
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

	public List<String> getDocumentsQueue() {
		return documentsQueue;
	}

	public void setDocumentsQueue(List<String> documentsQueue) {
		this.documentsQueue = documentsQueue;
	}
}

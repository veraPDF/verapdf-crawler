package org.verapdf.crawler.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class ValidationQueueStatus {

	@JsonProperty
	private Long count;
	@JsonProperty
	private List<String> topDocuments;

	public ValidationQueueStatus() {
	}

	public ValidationQueueStatus(Long count, List<String> topDocuments) {
		this.count = count;
		this.topDocuments = topDocuments;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public List<String> getTopDocuments() {
		return topDocuments;
	}

	public void setTopDocuments(List<String> documents) {
		this.topDocuments = documents;
	}
}

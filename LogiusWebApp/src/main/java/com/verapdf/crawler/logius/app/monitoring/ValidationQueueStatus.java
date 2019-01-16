package com.verapdf.crawler.logius.app.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.verapdf.crawler.logius.app.validation.ValidationJob;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class ValidationQueueStatus {

	@JsonProperty
	private Long count;
	@JsonProperty
	private List<ValidationJob> topDocuments;

	public ValidationQueueStatus() {
	}

	public ValidationQueueStatus(Long count, List<ValidationJob> topDocuments) {
		this.count = count;
		this.topDocuments = topDocuments;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public List<ValidationJob> getTopDocuments() {
		return topDocuments;
	}

	public void setTopDocuments(List<ValidationJob> documents) {
		this.topDocuments = documents;
	}
}

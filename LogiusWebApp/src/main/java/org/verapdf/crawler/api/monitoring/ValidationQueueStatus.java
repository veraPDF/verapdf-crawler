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
	private List<String> documents;

	public ValidationQueueStatus() {
	}

	public ValidationQueueStatus(Long count, List<String> documents) {
		this.count = count;
		this.documents = documents;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public List<String> getDocuments() {
		return documents;
	}

	public void setDocuments(List<String> documents) {
		this.documents = documents;
	}
}

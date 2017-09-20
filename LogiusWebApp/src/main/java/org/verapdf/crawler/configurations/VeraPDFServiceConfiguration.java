package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class VeraPDFServiceConfiguration {

	@NotEmpty
	private String url;

	public VeraPDFServiceConfiguration() {
	}

	@JsonProperty
	public String getUrl() {
		return url;
	}

	@JsonProperty
	public void setUrl(String url) {
		this.url = url;
	}
}

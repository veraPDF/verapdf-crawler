package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class BingConfiguration {

	@NotEmpty
	private String baseTempFolder;
	@NotEmpty
	private String apiKey;

	public BingConfiguration() {
	}

	@JsonProperty
	public String getBaseTempFolder() {
		return baseTempFolder;
	}

	@JsonProperty
	public void setBaseTempFolder(String baseTempFolder) {
		this.baseTempFolder = baseTempFolder;
	}

	@JsonProperty
	public String getApiKey() {
		return apiKey;
	}

	@JsonProperty
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}

package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class ReportsConfiguration {

	@NotEmpty
	private String odsTemplatePath;

	public ReportsConfiguration() {
	}

	@JsonProperty
	public String getOdsTemplatePath() {
		return odsTemplatePath;
	}

	@JsonProperty
	public void setOdsTemplatePath(String odsTemplatePath) {
		this.odsTemplatePath = odsTemplatePath;
	}
}

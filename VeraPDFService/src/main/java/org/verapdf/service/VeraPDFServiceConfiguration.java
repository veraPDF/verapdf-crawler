package org.verapdf.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class VeraPDFServiceConfiguration extends Configuration {
	private String verapdfPath;

	@JsonProperty
	public String getVerapdfPath() {
		return verapdfPath;
	}

	@JsonProperty
	public void setVerapdfPath(String verapdfPath) {
		this.verapdfPath = verapdfPath;
	}
}

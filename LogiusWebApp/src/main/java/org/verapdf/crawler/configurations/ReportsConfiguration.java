package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class ReportsConfiguration {

	@NotEmpty
	private String odsTemplatePath;
	@NotEmpty
	private String notificationEmails;
	@NotEmpty
	private String odsTempFolder;

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

	@JsonProperty
	public String getNotificationEmails() {
		return notificationEmails;
	}

	@JsonProperty
	public void setNotificationEmails(String notificationEmails) {
		this.notificationEmails = notificationEmails;
	}

	@JsonProperty
	public String getOdsTempFolder() {
		return odsTempFolder;
	}

	@JsonProperty
	public void setOdsTempFolder(String odsTempFolder) {
		this.odsTempFolder = odsTempFolder;
	}
}

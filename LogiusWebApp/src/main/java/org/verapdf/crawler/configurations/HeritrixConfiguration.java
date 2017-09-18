package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class HeritrixConfiguration {

	@NotEmpty
	private String url;
	@NotEmpty
	private String login;
	@NotEmpty
	private String password;
	@NotEmpty
	private String resourcePath;

	public HeritrixConfiguration() {
	}

	@JsonProperty
	public String getUrl() {
		return url;
	}

	@JsonProperty
	public void setUrl(String url) {
		this.url = url;
	}

	@JsonProperty
	public String getLogin() {
		return login;
	}

	@JsonProperty
	public void setLogin(String login) {
		this.login = login;
	}

	@JsonProperty
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

	@JsonProperty
	public String getResourcePath() {
		return resourcePath;
	}

	@JsonProperty
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}
}

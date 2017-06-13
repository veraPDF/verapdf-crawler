package org.verapdf.crawler.app.configuration;

import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.email.EmailServer;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class LogiusConfiguration extends Configuration {
    private EmailServer emailServer;
    private MySqlCredentials credentials;
    private String heritrixLogin;
    private String heritrixPassword;
    private String resourcePath;
    private String verapdfPath;

    @JsonProperty
    public MySqlCredentials getCredentials() {
        return credentials;
    }

    @JsonProperty
    public void setCredentials(MySqlCredentials credentials) {
        this.credentials = credentials;
    }

    @JsonProperty
    public String getVerapdfPath() {
        return verapdfPath;
    }

    @JsonProperty
    public void setVerapdfPath(String verapdfPath) {
        this.verapdfPath = verapdfPath;
    }

    @JsonProperty
    public String getResourcePath() {
        return resourcePath;
    }

    @JsonProperty
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @JsonProperty
    public String getHeritrixLogin() {
        return heritrixLogin;
    }

    @JsonProperty
    public void setHeritrixLogin(String heritrixLogin) {
        this.heritrixLogin = heritrixLogin;
    }

    @JsonProperty
    public String getHeritrixPassword() {
        return heritrixPassword;
    }

    @JsonProperty
    public void setHeritrixPassword(String heritrixPassword) {
        this.heritrixPassword = heritrixPassword;
    }

    @JsonProperty
    public EmailServer getEmailServer() {
        return emailServer;
    }

    @JsonProperty
    public void setEmailServer(EmailServer emailServer) {
        this.emailServer = emailServer;
    }
}

package org.verapdf.crawler;

import org.verapdf.crawler.configurations.HeritrixConfiguration;
import org.verapdf.crawler.configurations.MySqlConfiguration;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

public class LogiusConfiguration extends Configuration {
    private EmailServerConfiguration emailServerConfiguration;
    private MySqlConfiguration mySqlConfiguration;
    private HeritrixConfiguration heritrixConfiguration;
    private VeraPDFServiceConfiguration veraPDFServiceConfiguration;

    @JsonProperty("veraPDFService")
    public VeraPDFServiceConfiguration getVeraPDFServiceConfiguration() {
        return veraPDFServiceConfiguration;
    }

    @JsonProperty("veraPDFService")
    public void setVeraPDFServiceConfiguration(VeraPDFServiceConfiguration veraPDFServiceConfiguration) {
        this.veraPDFServiceConfiguration = veraPDFServiceConfiguration;
    }

    @JsonProperty("emailServer")
    public EmailServerConfiguration getEmailServerConfiguration() {
        return emailServerConfiguration;
    }

    @JsonProperty("emailServer")
    public void setEmailServerConfiguration(EmailServerConfiguration emailServerConfiguration) {
        this.emailServerConfiguration = emailServerConfiguration;
    }

    @JsonProperty("mysql")
    public MySqlConfiguration getMySqlConfiguration() {
        return mySqlConfiguration;
    }

    @JsonProperty("mysql")
    public void setMySqlConfiguration(MySqlConfiguration mySqlConfiguration) {
        this.mySqlConfiguration = mySqlConfiguration;
    }

    @JsonProperty("heritrix")
    public HeritrixConfiguration getHeritrixConfiguration() {
        return heritrixConfiguration;
    }

    @JsonProperty("heritrix")
    public void setHeritrixConfiguration(HeritrixConfiguration heritrixConfiguration) {
        this.heritrixConfiguration = heritrixConfiguration;
    }
}

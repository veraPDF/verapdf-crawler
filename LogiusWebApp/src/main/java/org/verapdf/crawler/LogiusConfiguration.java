package org.verapdf.crawler;

import io.dropwizard.db.DataSourceFactory;
import org.verapdf.crawler.configurations.HeritrixConfiguration;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.verapdf.crawler.configurations.ReportsConfiguration;
import org.verapdf.crawler.configurations.VeraPDFServiceConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class LogiusConfiguration extends Configuration {
    private EmailServerConfiguration emailServerConfiguration;
    private HeritrixConfiguration heritrixConfiguration;
    private VeraPDFServiceConfiguration veraPDFServiceConfiguration;
    private ReportsConfiguration reportsConfiguration;

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDatabase(DataSourceFactory database) {
        this.database = database;
    }

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

    @JsonProperty("heritrix")
    public HeritrixConfiguration getHeritrixConfiguration() {
        return heritrixConfiguration;
    }

    @JsonProperty("heritrix")
    public void setHeritrixConfiguration(HeritrixConfiguration heritrixConfiguration) {
        this.heritrixConfiguration = heritrixConfiguration;
    }

    @JsonProperty("reports")
    public ReportsConfiguration getReportsConfiguration() {
        return reportsConfiguration;
    }

    @JsonProperty("reports")
    public void setReportsConfiguration(ReportsConfiguration reportsConfiguration) {
        this.reportsConfiguration = reportsConfiguration;
    }
}

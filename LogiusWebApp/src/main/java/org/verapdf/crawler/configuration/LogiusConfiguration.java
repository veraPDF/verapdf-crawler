package org.verapdf.crawler.configuration;

import org.verapdf.crawler.api.EmailServer;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class LogiusConfiguration extends Configuration {
    EmailServer emailServer;

    @JsonProperty
    public EmailServer getEmailServer() {
        return emailServer;
    }

    @JsonProperty
    public void setEmailServer(EmailServer emailServer) {
        this.emailServer = emailServer;
    }
}

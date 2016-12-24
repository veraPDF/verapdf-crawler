package org.verapdf.crawler.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailAddress {
    private String emailAddress;

    public EmailAddress() {
        // Jackson deserialization
    }

    public EmailAddress(String address) {
        emailAddress = address;
    }

    @JsonProperty
    public String getEmailAddress() {
        return emailAddress;
    }

    @JsonProperty
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

}

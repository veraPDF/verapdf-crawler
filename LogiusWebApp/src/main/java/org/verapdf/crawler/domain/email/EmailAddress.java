package org.verapdf.crawler.domain.email;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailAddress {
    private String emailAddress;
    private String batchJobId;

    public EmailAddress() {}

    public EmailAddress(String address) {
        emailAddress = address;
    }

    @JsonProperty
    public String getEmailAddress() {
        return emailAddress;
    }

    @JsonProperty
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    @JsonProperty
    public String getBatchJobId() {
        return batchJobId;
    }

    @JsonProperty
    public void setBatchJobId(String batchJobId) {
        this.batchJobId = batchJobId;
    }

}

package org.verapdf.crawler.domain.email;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailServer {

    @JsonProperty
    public String host;

    @JsonProperty
    public String address;

    @JsonProperty
    public String user;

    @JsonProperty
    public String password;

    @JsonProperty
    public String port;

    public EmailServer() {}

}

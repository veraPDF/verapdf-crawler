package org.verapdf.crawler.domain.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MySqlCredentials {
    @JsonProperty
    public String connectionString;

    @JsonProperty
    public String user;

    @JsonProperty
    public String password;

    public MySqlCredentials() {}
}

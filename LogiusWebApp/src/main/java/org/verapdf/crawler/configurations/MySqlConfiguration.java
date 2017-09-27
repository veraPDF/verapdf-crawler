package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class MySqlConfiguration {
    @NotEmpty
    private String connectionString;
    @NotEmpty
    private String user;
    @NotEmpty
    private String password;

    public MySqlConfiguration() {
    }

    @JsonProperty
    public String getConnectionString() {
        return connectionString;
    }

    @JsonProperty
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    @JsonProperty
    public String getUser() {
        return user;
    }

    @JsonProperty
    public void setUser(String user) {
        this.user = user;
    }

    @JsonProperty
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }
}

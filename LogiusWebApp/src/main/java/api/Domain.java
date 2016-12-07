package api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Domain {
    private String domain;

    public Domain() {
        // Jackson deserialization
    }

    public Domain(String domain) {
        this.domain = domain;
    }

    @JsonProperty
    public String getDomain() {
        return domain;
    }

    @JsonProperty
    public void setDomain(String domain) { this.domain = domain; }
}

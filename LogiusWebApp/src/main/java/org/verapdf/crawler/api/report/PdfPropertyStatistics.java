package org.verapdf.crawler.api.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PdfPropertyStatistics {

    private String name;
    private String value;
    private Integer count;

    public PdfPropertyStatistics() { }

    public PdfPropertyStatistics(String name, String value, Integer count) {
        this.name = name;
        this.value = value;
        this.count = count;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getValue() {
        return value;
    }

    @JsonProperty
    public void setValue(String value) {
        this.value = value;
    }

    @JsonProperty
    public Integer getCount() {
        return count;
    }

    @JsonProperty
    public void setCount(Integer count) {
        this.count = count;
    }
}

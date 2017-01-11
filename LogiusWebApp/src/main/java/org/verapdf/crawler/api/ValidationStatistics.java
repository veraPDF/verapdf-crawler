package org.verapdf.crawler.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationStatistics {
    private int numberOfPDFs;
    private int numberOfValidPDFs;

    public ValidationStatistics() {}

    public ValidationStatistics(int numberOfPDFs, int numberOfValidPDFs) {
        this.numberOfPDFs = numberOfPDFs;
        this.numberOfValidPDFs = numberOfValidPDFs;
    }

    @JsonProperty
    public int getNumberOfPDFs() {
        return numberOfPDFs;
    }

    @JsonProperty
    public int getNumberOfValidPDFs() {
        return numberOfValidPDFs;
    }
}

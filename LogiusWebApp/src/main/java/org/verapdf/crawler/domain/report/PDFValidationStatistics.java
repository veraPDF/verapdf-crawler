package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PDFValidationStatistics {
    private int numberOfInvalidPDFs;
    private int numberOfValidPDFs;


    PDFValidationStatistics() {}

    public PDFValidationStatistics(int numberOfInvalidPDFs, int numberOfValidPDFs) {
        this.numberOfInvalidPDFs = numberOfInvalidPDFs;
        this.numberOfValidPDFs = numberOfValidPDFs;
    }

    @JsonProperty
    public int getNumberOfInvalidPDFs() {
        return numberOfInvalidPDFs;
    }

    @JsonProperty
    public int getNumberOfValidPDFs() {
        return numberOfValidPDFs;
    }
}

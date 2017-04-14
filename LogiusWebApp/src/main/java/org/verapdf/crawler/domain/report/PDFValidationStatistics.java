package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.domain.validation.ValidationReportData;

import java.util.ArrayList;

public class PDFValidationStatistics {
    private int numberOfInvalidPDFs;
    private int numberOfValidPDFs;
    private String invalidPDFReportURL;
    @JsonIgnore
    public ArrayList<ValidationReportData> invalidPDFReport;

    public PDFValidationStatistics() {}

    public PDFValidationStatistics(int numberOfInvalidPDFs, int numberOfValidPDFs, String invalidPDFReportURL) {
        this.numberOfInvalidPDFs = numberOfInvalidPDFs;
        this.numberOfValidPDFs = numberOfValidPDFs;
        this.invalidPDFReportURL = invalidPDFReportURL;
    }

    @JsonProperty
    public int getNumberOfInvalidPDFs() {
        return numberOfInvalidPDFs;
    }

    @JsonProperty
    public int getNumberOfValidPDFs() {
        return numberOfValidPDFs;
    }

    @JsonProperty
    public String getInvalidPDFReportURL() { return invalidPDFReportURL; }
}

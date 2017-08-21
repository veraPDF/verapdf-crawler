package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PDFValidationStatistics {

    private List<PdfPropertyStatistics> statistics;
    private Integer numberOfValidPdfDocuments;
    private Integer numberOfInvalidPdfDocuments;

    public PDFValidationStatistics(List<PdfPropertyStatistics> statistics, Integer numberOfInvalidPdfDocuments,
                                   Integer numberOfValidPdfDocuments) {
        this.statistics = statistics;
        this.numberOfInvalidPdfDocuments = numberOfInvalidPdfDocuments;
        this.numberOfValidPdfDocuments = numberOfValidPdfDocuments;
    }

    PDFValidationStatistics() {
        statistics = new ArrayList<>();
    }

    @JsonProperty
    public List<PdfPropertyStatistics> getStatistics() {
        return statistics;
    }

    @JsonProperty
    public void setProperties(List<PdfPropertyStatistics> statistics) {
        this.statistics = statistics;
    }

    @JsonProperty
    public Integer getNumberOfValidPdfDocuments() {
        return numberOfValidPdfDocuments;
    }

    @JsonProperty
    public void setNumberOfValidPdfDocuments(Integer numberOfValidPdfDocuments) {
        this.numberOfValidPdfDocuments = numberOfValidPdfDocuments;
    }

    @JsonProperty
    public Integer getNumberOfInvalidPdfDocuments() {
        return numberOfInvalidPdfDocuments;
    }

    @JsonProperty
    public void setNumberOfInvalidPdfDocuments(Integer numberOfInvalidPdfDocuments) {
        this.numberOfInvalidPdfDocuments = numberOfInvalidPdfDocuments;
    }
}

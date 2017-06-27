package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleURLJobReport {
    private String id;
    private String url;
    private String status;
    private int numberOfCrawledUrls;
    private PDFValidationStatistics pdfStatistics;

    private int numberOfODFDocuments;
    private int numberOfOfficeDocuments;

    @JsonProperty
    public String startTime;

    @JsonProperty
    public String finishTime;

    public SingleURLJobReport() {
        // Jackson deserialization
    }

    public SingleURLJobReport(String id, String url, String status, int numberOfCrawledUrls) {
        this.id = id;
        this.url = url;
        this.status = status;
        this.numberOfCrawledUrls = numberOfCrawledUrls;
        this.pdfStatistics = new PDFValidationStatistics();
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public String getUrl() {
        return url;
    }

    @JsonProperty
    public String getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(String status) { this.status = status; }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonProperty
    public int getNumberOfCrawledUrls() {return numberOfCrawledUrls;}

    @JsonProperty
    public PDFValidationStatistics getPdfStatistics() { return pdfStatistics; }

    @JsonProperty
    public void setPdfStatistics(PDFValidationStatistics pdfStatistics) { this.pdfStatistics = pdfStatistics; }

    @JsonProperty
    public int getNumberOfODFDocuments() { return  numberOfODFDocuments; }

    @JsonProperty
    public void setNumberOfODFDocuments(int numberOfODFDocuments) { this.numberOfODFDocuments = numberOfODFDocuments; }

    @JsonProperty
    public int getNumberOfOfficeDocuments() { return numberOfOfficeDocuments; }

    @JsonProperty
    public void setNumberOfOfficeDocuments(int numberOfOfficeDocuments) { this.numberOfOfficeDocuments = numberOfOfficeDocuments; }
}

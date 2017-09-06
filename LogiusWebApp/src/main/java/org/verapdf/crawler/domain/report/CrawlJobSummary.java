package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CrawlJobSummary {
    private String id;
    private String url;
    private String status;
    private int numberOfCrawledUrls;
    private String startTime;
    private String finishTime;

    private PDFValidationStatistics pdfStatistics;
    private int numberOfODFDocuments;
    private int numberOfOfficeDocuments;
    private int numberOfOoxmlDocuments;


    public CrawlJobSummary() {
    }

    public CrawlJobSummary(String id, String url, String status, int numberOfCrawledUrls) {
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

    @JsonProperty
    public int getNumberOfOoxmlDocuments() { return numberOfOoxmlDocuments; }

    @JsonProperty
    public void setNumberOfOoxmlDocuments(int numberOfOoxmlDocuments) { this.numberOfOoxmlDocuments = numberOfOoxmlDocuments; }

    @JsonProperty
    public String getStartTime() { return startTime; }

    @JsonProperty
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getFinishTime() { return finishTime; }

    public void setFinishTime(String finishTime) { this.finishTime = finishTime; }
}

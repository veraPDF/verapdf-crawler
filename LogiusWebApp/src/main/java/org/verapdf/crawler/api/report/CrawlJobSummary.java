package org.verapdf.crawler.api.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class CrawlJobSummary {
    private String id;
    private String domain;
    private String status;
    private int numberOfCrawledUrls;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date finishTime;

    private PDFValidationStatistics pdfStatistics;
    private int numberOfODFDocuments;
    private int numberOfOfficeDocuments;
    private int numberOfOoxmlDocuments;


    public CrawlJobSummary() {
    }

    public CrawlJobSummary(String id, String domain, String status, int numberOfCrawledUrls) {
        this.id = id;
        this.domain = domain;
        this.status = status;
        this.numberOfCrawledUrls = numberOfCrawledUrls;
        this.pdfStatistics = new PDFValidationStatistics();
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    @JsonProperty
    public String getDomain() {
        return domain;
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
    public Date getStartTime() { return startTime; }

    @JsonProperty
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    @JsonProperty
    public Date getFinishTime() { return finishTime; }

    @JsonProperty
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }
}

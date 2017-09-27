package org.verapdf.crawler.api.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.validation.error.ValidationError;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "documents")
public class DomainDocument {

    public enum BaseTestResult {
        OPEN("open"),
        NOT_OPEN("not_open");

        private String dataBaseValue;

        BaseTestResult(String dataBaseValue) {
            this.dataBaseValue = dataBaseValue;
        }

        public String getDataBaseValue() {
            return dataBaseValue;
        }
    }

    @Id
    @Column(name = "document_url")
    @JsonProperty
    private String url;

    @ManyToOne
    @JoinColumn(name = "crawl_job_domain")
    @JsonProperty
    private CrawlJob crawlJob;

    @Column(name = "last_modified")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty
    private Date lastModified;

    @Column(name = "document_type")
    @JsonProperty
    private String contentType;

    @Transient
    @JsonProperty
    private String filePath;

    @Column(name = "document_status")
    @JsonProperty
    private BaseTestResult baseTestResult;

    @ElementCollection
    @CollectionTable(
            name = "document_properties",
            joinColumns = @JoinColumn(name = "document_url")
    )
    @MapKeyColumn(name = "property_name")
    @Column(name = "property_value")
    @JsonProperty
    private Map<String, String> properties;

    @ManyToMany
    @JoinTable(
            name = "documents_validation_errors",
            joinColumns = @JoinColumn(name = "document_url"),
            inverseJoinColumns = @JoinColumn(name = "error_id")
    )
    @JsonProperty
    private List<ValidationError> validationErrors;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CrawlJob getCrawlJob() {
        return crawlJob;
    }

    public void setCrawlJob(CrawlJob crawlJob) {
        this.crawlJob = crawlJob;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public BaseTestResult getBaseTestResult() {
        return baseTestResult;
    }

    public void setBaseTestResult(BaseTestResult baseTestResult) {
        this.baseTestResult = baseTestResult;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }
}

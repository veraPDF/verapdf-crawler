package org.verapdf.crawler.api.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.validation.error.ValidationError;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "documents")
public class DomainDocument {

    public enum DocumentTypeGroup {
        PDF(Collections.singletonList("pdf")),
        OFFICE(Arrays.asList(
                "odt", "ods", "odp",
                "doc", "xls", "ppt",
                "docx", "xlsx", "pptx"
        ));

        private List<String> types;

        DocumentTypeGroup(List<String> types) {
            this.types = Collections.unmodifiableList(types);
        }

        public List<String> getTypes() {
            return types;
        }

        @JsonValue
        public String getValue() {
            return name().toLowerCase();
        }
    }

    public enum BaseTestResult {
        OPEN,
        NOT_OPEN
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

    @Enumerated(EnumType.STRING)
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

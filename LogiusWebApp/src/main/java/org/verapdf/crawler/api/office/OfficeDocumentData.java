package org.verapdf.crawler.api.office;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class OfficeDocumentData {

    @NotEmpty
    private String jobId;
    @NotEmpty
    private String fileUrl;
    private String lastModified;

    public OfficeDocumentData() {
    }

    @JsonProperty
    public String getJobId() {
        return jobId;
    }

    @JsonProperty
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @JsonProperty
    public String getFileUrl() {
        return fileUrl;
    }

    @JsonProperty
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @JsonProperty
    public String getLastModified() {
        return lastModified;
    }

    @JsonProperty
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}

package org.verapdf.crawler.logius.dto;

import org.verapdf.crawler.logius.validation.ValidationJob;

public class ValidationJobDto {
    private ValidationJob.Status status;
    private String id;

    public ValidationJobDto(ValidationJob job) {
        this.status = job.getStatus();
        this.id = job.getDocumentUrl();
    }

    public ValidationJob.Status getStatus() {
        return status;
    }

    public void setStatus(ValidationJob.Status status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

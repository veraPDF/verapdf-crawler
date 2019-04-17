package org.verapdf.crawler.logius.validation;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.model.DocumentId;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_validation_jobs_queue")
public class ValidationJob {
    @EmbeddedId
    @Column(insertable = false, updatable = false)
    private DocumentId documentId;

    @OneToOne
    @JoinColumns({
            @JoinColumn(name = "document_id", referencedColumnName = "document_id", insertable = false, updatable = false),
            @JoinColumn(name = "document_url", referencedColumnName = "document_url", insertable = false, updatable = false)
    })
    private DomainDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private Status status;

    @Column(name = "creation_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationDate;

    public ValidationJob() {
    }

    public ValidationJob(DomainDocument document) {
        this.document = document;
        this.documentId = document.getDocumentId();
        this.status = Status.NOT_STARTED;
    }

    public ValidationJob(DocumentId documentId, Status status) {
        this.documentId = documentId;
        this.status = status;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime now) {
        this.creationDate = now;
    }

    public DocumentId getDocumentId() {
        return documentId;
    }

    public void setDocumentId(DocumentId documentId) {
        this.documentId = documentId;
    }

    public DomainDocument getDocument() {
        return document;
    }

    public void setDocument(DomainDocument document) {
        this.document = document;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDocumentUrl() {
        return documentId.getDocumentUrl();
    }

    public enum Status {
        IN_PROGRESS,
        NOT_STARTED,
        PAUSED,
        ABORTED
    }
}

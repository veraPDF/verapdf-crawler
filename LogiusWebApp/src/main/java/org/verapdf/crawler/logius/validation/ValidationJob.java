package org.verapdf.crawler.logius.validation;

import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.model.DocumentId;

import javax.persistence.*;

@Entity
@Table(name = "pdf_validation_jobs_queue")
public class ValidationJob {
    //todo asf
    @EmbeddedId
    @Column(insertable = false, updatable = false)
    private DocumentId documentId;

    @OneToOne
    @JoinColumns({
            @JoinColumn(name="document_id", referencedColumnName = "document_id", insertable=false, updatable=false),
            @JoinColumn(name="document_url", referencedColumnName = "document_url", insertable=false, updatable=false)
    })
    private DomainDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private Status status;

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

    public enum Status {
        IN_PROGRESS,
        NOT_STARTED,
        PAUSED,
        ABORTED
    }
}

package org.verapdf.crawler.logius.validation;

import org.verapdf.crawler.logius.document.DomainDocument;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "pdf_validation_jobs_queue")
public class ValidationJob {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", name = "document_id", updatable = false)
    private UUID id;

    @MapsId("document_id")
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    @PrimaryKeyJoinColumn(name = "document_id", referencedColumnName = "id")
    private DomainDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private Status status;

    public ValidationJob() {
    }

    public ValidationJob(DomainDocument document) {
        this.document = document;
        this.id = document.getId();
        this.status = Status.NOT_STARTED;
    }

    public ValidationJob(UUID id, Status status) {
        this.id = id;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

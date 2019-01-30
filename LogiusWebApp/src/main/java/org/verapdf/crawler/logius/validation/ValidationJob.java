package org.verapdf.crawler.logius.validation;

import org.verapdf.crawler.logius.document.DomainDocument;

import javax.persistence.*;

@Entity
@Table(name = "pdf_validation_jobs_queue")
public class ValidationJob {

    @Id
    @Column(name = "document_url")
    private String id;
    @MapsId("document_url")
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    @PrimaryKeyJoinColumn(name = "document_url", referencedColumnName = "document_url")
    private DomainDocument document;
    @Column(name = "filepath")
    private String filePath;
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private Status status;

    public ValidationJob() {
    }

    public ValidationJob(DomainDocument document) {
        this.document = document;
        this.id = document.getUrl();
        this.filePath = document.getFilePath();
        this.status = Status.NOT_STARTED;
    }

    public ValidationJob(String id, Status status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DomainDocument getDocument() {
        return document;
    }

    public void setDocument(DomainDocument document) {
        this.document = document;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

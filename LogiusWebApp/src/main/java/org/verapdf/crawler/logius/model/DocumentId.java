package org.verapdf.crawler.logius.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class DocumentId implements Serializable {
    @Column(name = "document_id")
    private UUID id;

    @Column(name = "document_url")
    private String documentUrl;

    public DocumentId(UUID id, String documentUrl) {
        this.id = id;
        this.documentUrl = documentUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}
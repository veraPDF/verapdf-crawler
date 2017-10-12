package org.verapdf.crawler.api.report;

import org.verapdf.crawler.api.document.DomainDocument;

import java.util.HashMap;
import java.util.Map;

public class CrawlJobSummary {

    private Map<DomainDocument.DocumentTypeGroup, Long> openDocuments;

    private Map<DomainDocument.DocumentTypeGroup, Long> notOpenDocuments;

    public Map<DomainDocument.DocumentTypeGroup, Long> getOpenDocuments() {
        if (openDocuments == null) {
            openDocuments = new HashMap<>();
        }
        return openDocuments;
    }

    public void setOpenDocuments(Map<DomainDocument.DocumentTypeGroup, Long> openDocuments) {
        this.openDocuments = openDocuments;
    }

    public Map<DomainDocument.DocumentTypeGroup, Long> getNotOpenDocuments() {
        if (notOpenDocuments == null) {
            notOpenDocuments = new HashMap<>();
        }
        return notOpenDocuments;
    }

    public void setNotOpenDocuments(Map<DomainDocument.DocumentTypeGroup, Long> notOpenDocuments) {
        this.notOpenDocuments = notOpenDocuments;
    }
}

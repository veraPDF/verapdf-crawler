package org.verapdf.crawler.logius.report;

import org.verapdf.crawler.logius.document.DomainDocument;

import java.util.HashMap;
import java.util.Map;

public class CrawlJobSummary {
    private Map<DomainDocument.DocumentTypeGroup, Long> typeOfDocuments;

    public CrawlJobSummary() {
        this.typeOfDocuments =  new HashMap<>();
    }

    public Map<DomainDocument.DocumentTypeGroup, Long> getTypeOfDocuments() {
        return typeOfDocuments;
    }

    public void setTypeOfDocuments(Map<DomainDocument.DocumentTypeGroup, Long> typeOfDocuments) {
        this.typeOfDocuments = typeOfDocuments;
    }

    public void addTypeOfDocumentCount(DomainDocument.DocumentTypeGroup documentTypeGroup, long count){
        this.typeOfDocuments.put(documentTypeGroup, count);
    }
}

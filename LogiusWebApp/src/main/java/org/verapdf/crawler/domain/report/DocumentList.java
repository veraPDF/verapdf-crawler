package org.verapdf.crawler.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DocumentList {
    private String domain;
    private List<String> documentUrls;

    public DocumentList() {}

    public DocumentList(String domain, List<String> documentUrls) {
        this.domain = domain;
        this.documentUrls = documentUrls;
    }

    @JsonProperty
    public String getDomain() {
        return domain;
    }

    @JsonProperty
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JsonProperty
    public List<String> getDocumentUrls() {
        return documentUrls;
    }

    @JsonProperty
    public void setDocumentUrls(List<String> documentUrls) {
        this.documentUrls = documentUrls;
    }
}

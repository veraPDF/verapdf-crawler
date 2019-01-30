package org.verapdf.crawler.logius.validation.settings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "pdf_properties_namespaces")
public class Namespace {
    @Id
    @Column(name = "namespace_prefix")
    private String prefix;

    @Column(name = "namespace_url")
    private String url;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

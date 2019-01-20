package org.verapdf.crawler.logius.validation.settings;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "pdf_properties")
public class PdfProperty {

    @Id
    @Column(name = "property_name")
    private String name;

    @Column(name = "property_enabled")
    private boolean enabled;

    @ElementCollection
    @CollectionTable(
            name = "pdf_properties_xpath",
            joinColumns = @JoinColumn(name = "property_name")
    )
    @Column(name = "xpath")
    @OrderColumn(name = "xpath_index")
    private List<String> xpathList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getXpathList() {
        return xpathList;
    }

    public void setXpathList(List<String> xpathList) {
        this.xpathList = xpathList;
    }
}

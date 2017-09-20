package org.verapdf.crawler.api.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "crawl_job_requests")
public class CrawlRequest {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @JsonProperty
    private String id;

    @Transient
    @JsonProperty
    private List<String> domains;

    @Column(name = "is_finished")
    @JsonProperty
    private boolean finished;

    @Column(name = "report_email")
    @JsonProperty
    private String emailAddress;

    @Column(name = "crawl_since")
    @Temporal(TemporalType.DATE)
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date crawlSinceTime;

    public CrawlRequest() {
    }

    public CrawlRequest(String id, String emailAddress, Date crawlSinceTime) {
        this.id = id;
        this.domains = new ArrayList<>();
        this.emailAddress = emailAddress;
        this.finished = false;
        this.crawlSinceTime = crawlSinceTime;
    }

    public String getId() { return  id; }

    public void setId(String id) { this.id = id; }

    public List<String> getDomains() { return domains; }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getEmailAddress() { return emailAddress; }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Date getCrawlSinceTime() {
        return crawlSinceTime;
    }

    public void setCrawlSinceTime(Date crawlSinceTime) {
        this.crawlSinceTime = crawlSinceTime;
    }
}

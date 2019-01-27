package org.verapdf.crawler.logius.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "crawl_job_requests")
public class CrawlRequest {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String id;

    @NotNull
    @ManyToMany(mappedBy = "crawlRequests")
    private List<CrawlJob> crawlJobs;

    @Column(name = "is_finished")
    private boolean finished;

    @Column(name = "report_email")
    private String emailAddress;

    @Column(name = "crawl_since")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date crawlSinceTime;

    public CrawlRequest() {
    }

    public CrawlRequest(String id, String emailAddress, Date crawlSinceTime) {
        this.id = id;
        this.emailAddress = emailAddress;
        this.finished = false;
        this.crawlSinceTime = crawlSinceTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<CrawlJob> getCrawlJobs() {
        return crawlJobs;
    }

    public void setCrawlJobs(List<CrawlJob> crawlJobs) {
        this.crawlJobs = crawlJobs;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

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

package org.verapdf.crawler.logius.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
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
    private LocalDate crawlSinceTime;

    @Column(name = "creation_date")
    private Instant creationDate;

    public CrawlRequest() {
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

    public LocalDate getCrawlSinceTime() {
        return crawlSinceTime;
    }

    public void setCrawlSinceTime(LocalDate crawlSinceTime) {
        this.crawlSinceTime = crawlSinceTime;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

}

package org.verapdf.crawler.logius.crawling;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.ColumnDefault;
import org.verapdf.crawler.logius.model.User;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "crawl_jobs")
public class CrawlJob {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @NotEmpty
    private String domain;

    @Column(name = "heritrix_job_id")
    private String heritrixJobId;

    @Column(name = "job_url")
    private String jobURL;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Column(name = "finish_time")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status")
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_service")
    private CrawlService crawlService;

    @Column(name = "is_finished")
    private boolean finished;

    @Column(name = "is_validation_enabled")
    private boolean isValidationEnabled;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "crawl_job_requests_crawl_jobs",
            joinColumns = @JoinColumn(name = "crawl_job_id"),
            inverseJoinColumns = @JoinColumn(name = "crawl_job_request_id")
    )
    @JsonIgnore
    private List<CrawlRequest> crawlRequests;

    public CrawlJob() {
    }

    public CrawlJob(String domain) {
        this(domain, CrawlService.HERITRIX);
    }

    public CrawlJob(String domain, CrawlService service) {
        this.domain = domain;
        this.heritrixJobId = UUID.randomUUID().toString();
        this.startTime = new Date();
        this.status = Status.NEW;
        this.crawlService = service;
    }

    public CrawlJob(String heritrixJobId, String jobURL, String domain, Date startTime) {
        this.heritrixJobId = heritrixJobId;
        this.jobURL = jobURL;
        this.domain = domain;
        this.startTime = startTime;
        this.crawlService = CrawlService.HERITRIX;
    }

    public CrawlJob(String domain, CrawlService bing, boolean isValidationRequired) {
        this(domain, bing);
        this.isValidationEnabled = isValidationRequired;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isValidationEnabled() {
        return isValidationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        isValidationEnabled = validationEnabled;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHeritrixJobId() {
        return heritrixJobId;
    }

    public void setHeritrixJobId(String heritrixJobId) {
        this.heritrixJobId = heritrixJobId;
    }

    public String getJobURL() {
        return jobURL;
    }

    public void setJobURL(String jobURL) {
        this.jobURL = jobURL;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public CrawlService getCrawlService() {
        return crawlService;
    }

    public void setCrawlService(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isValidationDisabled() {
        return !this.isValidationEnabled;
    }

    public List<CrawlRequest> getCrawlRequests() {
        if (crawlRequests == null) {
            crawlRequests = new ArrayList<>();
        }
        return crawlRequests;
    }

    public void setCrawlRequests(List<CrawlRequest> crawlRequests) {
        this.crawlRequests = crawlRequests;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public enum Status {
        NEW,
        RUNNING,
        PAUSED,
        FINISHED,
        FAILED
    }

    public enum CrawlService {
        HERITRIX,
        BING
    }
}
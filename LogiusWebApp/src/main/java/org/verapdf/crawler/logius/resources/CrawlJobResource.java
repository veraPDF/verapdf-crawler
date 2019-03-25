package org.verapdf.crawler.logius.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.HeritrixCleanerTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.monitoring.CrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.HeritrixCrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.logius.service.BingService;
import org.verapdf.crawler.logius.service.ValidationJobService;
import org.verapdf.crawler.logius.service.ValidatorService;
import org.verapdf.crawler.logius.tools.DomainUtils;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "api/crawl-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlJobResource {

    private static final Logger logger = LoggerFactory.getLogger(CrawlJobResource.class);

    private static final int GET_STATUS_MAX_DOCUMENT_COUNT = 10;

    private final CrawlJobDAO crawlJobDAO;
    private final HeritrixClient heritrixClient;
    private final ValidationJobDAO validationJobDAO;
    private final ValidationJobService validationJobService;
    private final ValidatorService validatorService;
    private final HeritrixCleanerTask heritrixCleanerTask;
    private final BingService bingService;

    public CrawlJobResource(CrawlJobDAO crawlJobDAO, HeritrixClient heritrixClient, ValidationJobDAO validationJobDAO,
                            ValidationJobService validationJobService, ValidatorService validatorService,
                            HeritrixCleanerTask heritrixCleanerTask, BingService bingService) {
        this.crawlJobDAO = crawlJobDAO;
        this.heritrixClient = heritrixClient;
        this.validationJobDAO = validationJobDAO;
        this.validationJobService = validationJobService;
        this.validatorService = validatorService;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.bingService = bingService;
    }

    @GetMapping
    @Transactional
    public ResponseEntity getJobList(@RequestParam(value = "domainFilter", required = false) String domainFilter,
                                     @RequestParam(value = "finished", required = false) Boolean finished,
                                     @RequestParam("start") int startParam,
                                     @RequestParam("limit") int limitParam) {

        long totalCount = crawlJobDAO.count(domainFilter, finished);
        List<CrawlJob> crawlJobs = crawlJobDAO.find(domainFilter, finished, startParam, limitParam);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(totalCount)).body(crawlJobs);
    }

    @PostMapping("/{domain}")
    @Transactional
    public ResponseEntity restartCrawlJob(@PathVariable("domain") String domain) {
        domain = DomainUtils.trimUrl(domain);
        CrawlJob crawlJob = crawlJobDAO.getByDomain(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        return ResponseEntity.ok(restartCrawlJob(crawlJob, domain, service));
    }

    public CrawlJob restartCrawlJob(CrawlJob crawlJob, String domain, CrawlJob.CrawlService service) {
        List<CrawlRequest> crawlRequests;

        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService currentService = crawlJob.getCrawlService();
        // Keep requests list to link to new job
        crawlRequests = new ArrayList<>(crawlJob.getCrawlRequests());

        // Tear crawl service
        if (currentService == CrawlJob.CrawlService.HERITRIX) {
            heritrixCleanerTask.teardownAndClearHeritrixJob(heritrixJobId);
        } else if (currentService == CrawlJob.CrawlService.BING) {
            bingService.discardJob(crawlJob);
        }

        // Remove job from DB
        crawlJobDAO.remove(crawlJob);

        // Stop validation job if it's related to this crawl job
        synchronized (ValidationJobService.class) {
            ValidationJob currentJob = validationJobService.getCurrentJob();
            if (currentJob != null && currentJob.getDocument().getCrawlJob().getDomain().equals(domain)) {
                validatorService.abortCurrentJob();
            }
        }


        // Create and start new crawl job
        CrawlJob newJob = new CrawlJob(domain, service, crawlJob.isValidationEnabled());
        newJob.setCrawlRequests(crawlRequests);
        crawlJobDAO.save(newJob);
        if (service == CrawlJob.CrawlService.HERITRIX) {
            startCrawlJob(newJob);
        }
        return newJob;
    }


    private CrawlJob getCrawlJob(String domain) {
        domain = DomainUtils.trimUrl(domain);
        return crawlJobDAO.getByDomain(domain);
    }

    @GetMapping("/{domain}")
    @Transactional
    public ResponseEntity<CrawlJob> getCrawl(@PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(crawlJob);
    }

    @PutMapping("/{domain}")
    @Transactional
    public ResponseEntity updateCrawlJob(@PathVariable("domain") String domain, @RequestBody @NotNull CrawlJob update) throws IOException,
            XPathExpressionException, SAXException, ParserConfigurationException {
        CrawlJob crawlJob = getCrawlJob(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }

        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        if (crawlJob.getStatus() == CrawlJob.Status.RUNNING && update.getStatus() == CrawlJob.Status.PAUSED) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.pauseJob(heritrixJobId);
            }
            validationJobDAO.pause(domain);
            crawlJob.setStatus(CrawlJob.Status.PAUSED);
        }
        if (crawlJob.getStatus() == CrawlJob.Status.PAUSED && update.getStatus() == CrawlJob.Status.RUNNING) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.unpauseJob(heritrixJobId);
            }
            validationJobDAO.unpause(domain);
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        }
        return ResponseEntity.ok(crawlJob);
    }


    @GetMapping("/{domain}/requests")
    @Transactional
    public ResponseEntity getCrawlJobRequests(@PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        List<CrawlRequest> crawlRequests = crawlJob.getCrawlRequests();
        crawlRequests.forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());
        return ResponseEntity.ok(crawlRequests);
    }

    @GetMapping("/{domain}/status")
    @Transactional
    public ResponseEntity getFullJobStatus(@PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        HeritrixCrawlJobStatus heritrixStatus = null;
        CrawlJob.CrawlService crawlService = crawlJob.getCrawlService();
        if (crawlService == CrawlJob.CrawlService.HERITRIX) {
            try {
                heritrixStatus = heritrixClient.getHeritrixStatus(crawlJob.getHeritrixJobId());
            } catch (Throwable e) {
                logger.error("Error during obtaining heritrix status", e);
                heritrixStatus = new HeritrixCrawlJobStatus("Unavailable: " + e.getMessage(), null, null);
            }
        } else if (crawlService == CrawlJob.CrawlService.BING) {
            //TODO: fix this
            heritrixStatus = null;
        }

        String crawlJobDomain = crawlJob.getDomain();
        Long count = validationJobDAO.count(crawlJobDomain);
        List<ValidationJob> topDocuments = validationJobDAO.getDocuments(crawlJobDomain, GET_STATUS_MAX_DOCUMENT_COUNT);
        crawlJob.getCrawlRequests().forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());
        return ResponseEntity.ok(new CrawlJobStatus(crawlJob, heritrixStatus, new ValidationQueueStatus(count, topDocuments)));
    }


    @DeleteMapping("/{domain}/requests")
    @Transactional
    public ResponseEntity unlinkCrawlRequests(@PathVariable("domain") String domain, @RequestParam("email") @NotNull String email) {
        CrawlJob crawlJob = getCrawlJob(domain);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }

        crawlJob.getCrawlRequests().removeIf(request -> email.equals(request.getEmailAddress()));
        return ResponseEntity.ok(crawlJob.getCrawlRequests());
    }


    public void startCrawlJob(CrawlJob crawlJob) {
        try {
            String heritrixJobId = crawlJob.getHeritrixJobId();
            heritrixClient.createJob(heritrixJobId, crawlJob.getDomain());
            heritrixClient.buildJob(heritrixJobId);
            heritrixClient.launchJob(heritrixJobId);
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        } catch (Exception e) {
            logger.error("Failed to start crawling job for domain " + crawlJob.getDomain(), e);
            crawlJob.setFinished(true);
            crawlJob.setFinishTime(new Date());
            crawlJob.setStatus(CrawlJob.Status.FAILED);
        }
        // TODO: cleanup heritrix in finally
    }
}

package org.verapdf.crawler.logius.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
import org.verapdf.crawler.logius.monitoring.CrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.HeritrixCrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.logius.service.CrawlJobService;
import org.verapdf.crawler.logius.service.CrawlService;
import org.verapdf.crawler.logius.tools.DomainUtils;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "api/crawl-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlJobResource {

    private static final Logger logger = LoggerFactory.getLogger(CrawlJobResource.class);

    private static final int GET_STATUS_MAX_DOCUMENT_COUNT = 10;

    private final CrawlJobDAO crawlJobDAO;
    private final HeritrixClient heritrixClient;
    private final ValidationJobDAO validationJobDAO;
    private final CrawlJobService crawlJobService;
    private final CrawlService crawlService;

    public CrawlJobResource(CrawlJobDAO crawlJobDAO, HeritrixClient heritrixClient, ValidationJobDAO validationJobDAO,
                            CrawlJobService crawlJobService, CrawlService crawlService) {
        this.crawlJobDAO = crawlJobDAO;
        this.heritrixClient = heritrixClient;
        this.validationJobDAO = validationJobDAO;
        this.crawlJobService = crawlJobService;
        this.crawlService = crawlService;
    }

    @GetMapping
    @Transactional
    public ResponseEntity getJobList(@AuthenticationPrincipal TokenUserDetails principal,
                                     @RequestParam(value = "domainFilter", required = false) String domainFilter,
                                     @RequestParam(value = "finished", required = false) Boolean finished,
                                     @RequestParam("start") int startParam,
                                     @RequestParam("limit") int limitParam) {

        UUID id = principal == null ? null : principal.getUuid();
        long totalCount = crawlJobDAO.count(domainFilter, id, finished);
        List<CrawlJob> crawlJobs = crawlJobDAO.find(domainFilter, id, finished, startParam, limitParam);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(totalCount)).body(crawlJobs);
    }

    @PostMapping("/{domain}")
    @Transactional
    public ResponseEntity restartCrawlJob(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        CrawlJob.CrawlService service = crawlJob.getCrawlService();

        return ResponseEntity.ok(crawlService.restartCrawlJob(crawlJob, domain, service));
    }

    private CrawlJob getCrawlJob(String domain, TokenUserDetails principal) {
        domain = DomainUtils.trimUrl(domain);
        return crawlJobDAO.findByDomainAndUserId(domain, principal == null ? null : principal.getUuid());
    }

    @GetMapping("/{domain}")
    @Transactional
    public ResponseEntity<CrawlJob> getCrawl(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(crawlJob);
    }

    @PutMapping("/{domain}")
    @Transactional
    public ResponseEntity updateCrawlJob(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain, @RequestBody @NotNull CrawlJob update) throws IOException,
            XPathExpressionException, SAXException, ParserConfigurationException {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }

        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        if (crawlJob.getStatus() == CrawlJob.Status.RUNNING && update.getStatus() == CrawlJob.Status.PAUSED) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.pauseJob(heritrixJobId);
            }
            validationJobDAO.pause(crawlJob.getId());
            crawlJob.setStatus(CrawlJob.Status.PAUSED);
        }
        if (crawlJob.getStatus() == CrawlJob.Status.PAUSED && update.getStatus() == CrawlJob.Status.RUNNING) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.unpauseJob(heritrixJobId);
            }
            validationJobDAO.unpause(crawlJob.getId());
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        }
        return ResponseEntity.ok(crawlJob);
    }


    @GetMapping("/{domain}/requests")
    @Transactional
    public ResponseEntity getCrawlJobRequests(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }
        List<CrawlRequest> crawlRequests = crawlJob.getCrawlRequests();
        crawlRequests.forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());
        return ResponseEntity.ok(crawlRequests);
    }

    @GetMapping("/{domain}/status")
    @Transactional
    public ResponseEntity getFullJobStatus(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
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

        UUID crawlJobId = crawlJob.getId();
        Long count = validationJobDAO.count(crawlJobId);
        List<ValidationJob> topDocuments = validationJobDAO.getDocuments(crawlJobId, GET_STATUS_MAX_DOCUMENT_COUNT);
        crawlJob.getCrawlRequests().forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());
        return ResponseEntity.ok(new CrawlJobStatus(crawlJob, heritrixStatus, new ValidationQueueStatus(count, topDocuments)));
    }


    @DeleteMapping("/{domain}/requests")
    @Transactional
    public ResponseEntity unlinkCrawlRequests(@AuthenticationPrincipal TokenUserDetails principal, @PathVariable("domain") String domain, @RequestParam("email") @NotNull String email) {
        CrawlJob crawlJob = getCrawlJob(domain, principal);
        if (crawlJob == null) {
            return ResponseEntity.notFound().build();
        }

        crawlJob.getCrawlRequests().removeIf(request -> email.equals(request.getEmailAddress()));
        return ResponseEntity.ok(crawlJob.getCrawlRequests());
    }
}

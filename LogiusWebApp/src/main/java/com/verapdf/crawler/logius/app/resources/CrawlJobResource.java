package com.verapdf.crawler.logius.app.resources;


import com.verapdf.crawler.logius.app.validation.ValidationJob;
import com.verapdf.crawler.logius.app.core.heritrix.HeritrixClient;
import com.verapdf.crawler.logius.app.core.services.HeritrixCleanerService;
import com.verapdf.crawler.logius.app.core.validation.ValidationService;
import com.verapdf.crawler.logius.app.tools.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.verapdf.crawler.logius.app.crawling.CrawlRequest;
import com.verapdf.crawler.logius.app.monitoring.CrawlJobStatus;
import com.verapdf.crawler.logius.app.monitoring.HeritrixCrawlJobStatus;
import com.verapdf.crawler.logius.app.monitoring.ValidationQueueStatus;
import com.verapdf.crawler.logius.app.crawling.CrawlJob;
import com.verapdf.crawler.logius.app.core.services.BingService;
import com.verapdf.crawler.logius.app.db.CrawlJobDAO;
import com.verapdf.crawler.logius.app.db.ValidationJobDAO;
import org.xml.sax.SAXException;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping(value = "logius/crawl-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlJobResource {

    private static final Logger logger = LoggerFactory.getLogger(CrawlJobResource.class);

    private static final int GET_STATUS_MAX_DOCUMENT_COUNT = 10;

    private final CrawlJobDAO crawlJobDAO;
    private final HeritrixClient heritrixClient;
    private final ValidationJobDAO validationJobDAO;
    private final ValidationService validationService;
    private final HeritrixCleanerService heritrixCleanerService;
    private final BingService bingService;

    public CrawlJobResource(CrawlJobDAO crawlJobDAO, HeritrixClient heritrixClient, ValidationJobDAO validationJobDAO,
                            ValidationService validationService, HeritrixCleanerService heritrixCleanerService, BingService bingService) {
        this.crawlJobDAO = crawlJobDAO;
        this.heritrixClient = heritrixClient;
        this.validationJobDAO = validationJobDAO;
        this.validationService = validationService;
        this.heritrixCleanerService = heritrixCleanerService;
        this.bingService = bingService;
    }

    @GetMapping
    @Transactional
    public ResponseEntity getJobList(@RequestParam("domainFilter") String domainFilter,
                                     @RequestParam("finished") Boolean finished,
                                     @RequestParam("start") int startParam,
                                     @RequestParam("limit") int limitParam) {
//        Integer start = startParam != null ? startParam.get() : null;
//        Integer limit = limitParam != null ? limitParam.get() : null;

        long totalCount = crawlJobDAO.count(domainFilter, finished);
        List<CrawlJob> crawlJobs = crawlJobDAO.find(domainFilter, finished, startParam, limitParam);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", "" + totalCount);
        return new ResponseEntity<>(crawlJobs, headers, HttpStatus.OK);

    }

    @PostMapping("/{domain}")
    @Transactional
    public CrawlJob restartCrawlJob(@PathVariable("domain") String domain) {
        domain = DomainUtils.trimUrl(domain);

        CrawlJob crawlJob = crawlJobDAO.getByDomain(domain);
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        return restartCrawlJob(crawlJob, domain, service);
    }

    //todo M static?
    public CrawlJob restartCrawlJob(CrawlJob crawlJob, String domain, CrawlJob.CrawlService service) {
        List<CrawlRequest> crawlRequests = null;
        if (crawlJob != null) {
            String heritrixJobId = crawlJob.getHeritrixJobId();
            CrawlJob.CrawlService currentService = crawlJob.getCrawlService();
            // Keep requests list to link to new job
            crawlRequests = new ArrayList<>();
            crawlRequests.addAll(crawlJob.getCrawlRequests());

            // Tear crawl service
            if (currentService == CrawlJob.CrawlService.HERITRIX) {
                heritrixCleanerService.teardownAndClearHeritrixJob(heritrixJobId);
            } else if (currentService == CrawlJob.CrawlService.BING) {
                bingService.discardJob(crawlJob);
            }

            // Remove job from DB
            crawlJobDAO.remove(crawlJob);

            // Stop validation job if it's related to this crawl job
            synchronized (ValidationService.class) {
                ValidationJob currentJob = validationService.getCurrentJob();
                if (currentJob != null && currentJob.getDocument().getCrawlJob().getDomain().equals(domain)) {
                    validationService.abortCurrentJob();
                }
            }
        }

        // Create and start new crawl job
        CrawlJob newJob = new CrawlJob(domain, service);
        if (crawlRequests != null) {
            newJob.setCrawlRequests(crawlRequests);
        }
        crawlJobDAO.save(newJob);
        if (service == CrawlJob.CrawlService.HERITRIX) {
            startCrawlJob(newJob);
        }
        return newJob;
    }


    @GetMapping("/{domain}")
    @Transactional
    public CrawlJob getCrawlJob(@PathVariable("domain") String domain) {
        domain = DomainUtils.trimUrl(domain);

        CrawlJob crawlJob = crawlJobDAO.getByDomain(domain);
        if (crawlJob == null) {
            //todo M return 404
            return null;
            //throw new WebApplicationException("Domain " + domain + " not found", Response.Status.NOT_FOUND);
        }
        return crawlJob;
    }

    @PutMapping("/{domain}")
    @Transactional
    public CrawlJob updateCrawlJob(@PathVariable("domain") String domain, @NotNull CrawlJob update) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        CrawlJob crawlJob = this.getCrawlJob(domain);

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
        return crawlJob;
    }


    @GetMapping("/{domain}/requests")
    public List<CrawlRequest> getCrawlJobRequests(@PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        return crawlJob.getCrawlRequests();
    }

    @GetMapping("/{domain}/status")
    @Transactional
    public CrawlJobStatus getFullJobStatus(@PathVariable("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
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

        return new CrawlJobStatus(crawlJob, heritrixStatus, new ValidationQueueStatus(count, topDocuments));
    }


    @DeleteMapping("/{domain}/requests")
    @Transactional
    public List<CrawlRequest> unlinkCrawlRequests(@PathVariable("domain") String domain, @RequestParam("email") @NotNull String email) {
        CrawlJob crawlJob = getCrawlJob(domain);

        crawlJob.getCrawlRequests().removeIf(request -> email.equals(request.getEmailAddress()));

//        if (crawlJob.getCrawlRequests().size() == 0) {
//            // todo: clarify if possible/required to terminate CrawlJob if no associated CrawlRequests left
//        }

        return crawlJob.getCrawlRequests();
    }
//
//    @GET
//    @Path("/{domain}/documents")
//    public List<Object> getDomainDocuments(@PathParam("domain") String domain,
//                                           @QueryParam("startDate") DateParam startDate,
//                                           @QueryParam("type") String type,
//                                           @QueryParam("start") IntParam start,
//                                           @QueryParam("limit") IntParam limit,
//                                           @QueryParam("property") List<String> properties) {
//        /* todo: introduce new domain object DomainDocument with the following structure:
//            {
//                url: '',
//                contentType: '',
//                compliant: true,
//                properties: {
//                    requestedProperty1: '',
//                    requestedProperty2: '',
//                    ...
//                },
//                errors: [
//                    'Error description 1',
//                    'Error description 2'
//                ]
//            }
//         */
//        return null;
//    }


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

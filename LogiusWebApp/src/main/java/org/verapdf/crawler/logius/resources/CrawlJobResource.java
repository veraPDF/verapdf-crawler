package org.verapdf.crawler.logius.resources;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
import org.verapdf.crawler.logius.monitoring.CrawlJobStatus;
import org.verapdf.crawler.logius.resources.util.ControllerHelper;
import org.verapdf.crawler.logius.service.CrawlJobService;
import org.verapdf.crawler.logius.service.CrawlRequestService;
import org.xml.sax.SAXException;

import javax.validation.constraints.NotNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "api/crawl-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlJobResource {
    private final CrawlJobService crawlJobService;
    private final CrawlRequestService crawlRequestService;
    private final ControllerHelper controllerHelper;

    public CrawlJobResource(CrawlJobService crawlJobService, CrawlRequestService crawlRequestService,
                            ControllerHelper controllerHelper) {
        this.crawlJobService = crawlJobService;
        this.crawlRequestService = crawlRequestService;
        this.controllerHelper = controllerHelper;
    }

    @GetMapping
    public ResponseEntity<List<CrawlJob>> getJobList(@AuthenticationPrincipal TokenUserDetails principal,
                                                     @RequestParam(value = "domainFilter", required = false) String domainFilter,
                                                     @RequestParam(value = "finished", required = false) Boolean finished,
                                                     @RequestParam("start") int startParam,
                                                     @RequestParam("limit") int limitParam) {

        UUID id = controllerHelper.getUserUUID(principal);
        long totalCount = crawlJobService.count(domainFilter, id, finished);
        List<CrawlJob> crawlJobs = crawlJobService.find(domainFilter, id, finished, startParam, limitParam);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(totalCount)).body(crawlJobs);
    }

    @PreAuthorize("isFullyAuthenticated()")
    @DeleteMapping("/{domain}")
    @Transactional
    public ResponseEntity cancelCrawlJob(@AuthenticationPrincipal TokenUserDetails principal,
                                                    @PathVariable("domain") String domain) {
        UUID id = controllerHelper.getUserUUID(principal);
        crawlJobService.cancelCrawlJob(id, domain);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{domain}")
    @Transactional
    public ResponseEntity<CrawlJob> restartCrawlJob(@AuthenticationPrincipal TokenUserDetails principal,
                                                    @PathVariable("domain") String domain) {
        UUID id = controllerHelper.getUserUUID(principal);
        return ResponseEntity.ok(crawlJobService.restartCrawlJob(id, domain));
    }


    @GetMapping("/{domain}")
    public ResponseEntity<CrawlJob> getCrawl(@AuthenticationPrincipal TokenUserDetails principal,
                                             @PathVariable("domain") String domain) {
        UUID id = controllerHelper.getUserUUID(principal);
        CrawlJob crawlJob = crawlJobService.getCrawlJob(domain, id);
        return ResponseEntity.ok(crawlJob);
    }

    @PutMapping("/{domain}")
    public ResponseEntity<CrawlJob> updateCrawlJob(@AuthenticationPrincipal TokenUserDetails principal,
                                                   @PathVariable("domain") String domain,
                                                   @RequestBody @NotNull CrawlJob update) throws IOException,
            XPathExpressionException, SAXException, ParserConfigurationException {
        UUID id = controllerHelper.getUserUUID(principal);
        CrawlJob updatedCrawlJob = crawlJobService.update(update, id);
        return ResponseEntity.ok(updatedCrawlJob);
    }


    @GetMapping("/{domain}/requests")
    public ResponseEntity<List<CrawlRequest>> getCrawlJobRequests(@AuthenticationPrincipal TokenUserDetails principal,
                                                                  @PathVariable("domain") String domain) {
        UUID id = controllerHelper.getUserUUID(principal);
        return ResponseEntity.ok(crawlRequestService.getCrawlRequests(domain, id));
    }

    @GetMapping("/{domain}/status")
    public ResponseEntity<CrawlJobStatus> getFullJobStatus(@AuthenticationPrincipal TokenUserDetails principal,
                                                           @PathVariable("domain") String domain) {
        UUID id = controllerHelper.getUserUUID(principal);
        CrawlJobStatus crawlJobStatus = crawlJobService.getFullJobStatus(domain, id);
        return ResponseEntity.ok(crawlJobStatus);
    }


    @DeleteMapping("/{domain}/requests")
    public ResponseEntity<List<CrawlRequest>> unlinkCrawlRequests(@AuthenticationPrincipal TokenUserDetails principal,
                                                                  @PathVariable("domain") String domain,
                                                                  @RequestParam("email") @NotNull String email) {
        UUID id = controllerHelper.getUserUUID(principal);
        CrawlJob crawlJob = crawlJobService.unlinkCrawlRequests(domain, id, email);
        return ResponseEntity.ok(crawlJob.getCrawlRequests());
    }
}

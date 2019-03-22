package org.verapdf.crawler.logius.resources;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.dto.TokenUserDetails;
import org.verapdf.crawler.logius.service.CrawlRequestService;

import javax.transaction.Transactional;


@RestController
@RequestMapping(value = "api/crawl-requests")
public class CrawlRequestResource {
    private final CrawlRequestService crawlRequestService;

    public CrawlRequestResource(CrawlRequestService crawlRequestService) {
        this.crawlRequestService = crawlRequestService;
    }

    @PostMapping
    @Transactional
    public CrawlRequest createCrawlRequest(@AuthenticationPrincipal TokenUserDetails principal,
                                           @RequestBody CrawlRequest crawlRequest,
                                           @RequestParam(value = "isValidationRequired", defaultValue = "false") boolean isValidationRequired,
                                           @RequestParam(value = "crawlService", defaultValue = "BING") CrawlJob.CrawlService crawlService) {
        if (principal == null) {
            isValidationRequired = false;
            crawlService = CrawlJob.CrawlService.BING;
        }
        return crawlRequestService.createCrawlRequest(crawlRequest, crawlService, isValidationRequired);

    }
}

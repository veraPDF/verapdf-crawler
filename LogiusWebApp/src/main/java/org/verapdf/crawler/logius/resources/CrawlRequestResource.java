package org.verapdf.crawler.logius.resources;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.CrawlRequestDAO;
import org.verapdf.crawler.logius.tools.DomainUtils;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value = "api/crawl-requests", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlRequestResource {
    private final CrawlRequestDAO crawlRequestDAO;
    private final CrawlJobDAO crawlJobDAO;

    public CrawlRequestResource(CrawlRequestDAO crawlRequestDAO, CrawlJobDAO crawlJobDAO) {
        this.crawlRequestDAO = crawlRequestDAO;
        this.crawlJobDAO = crawlJobDAO;
    }

    @PostMapping
    @Transactional
    public CrawlRequest createBingCrawlRequest(@Valid @RequestBody CrawlRequest crawlRequest) {
        // Validate and pre-process input
        List<String> domains = crawlRequest.getCrawlJobs().stream()
                .map(CrawlJob::getDomain)
                .map(DomainUtils::trimUrl).collect(Collectors.toList());
        // Save request
        crawlRequest = crawlRequestDAO.save(crawlRequest);

        // Find jobs for domains requested earlier and link with this request
        List<CrawlJob> existingJobs = crawlJobDAO.findByDomain(domains);
        for (CrawlJob existingJob : existingJobs) {
            domains.remove(existingJob.getDomain());
            existingJob.getCrawlRequests().add(crawlRequest);
        }

        // For domains that are left start new crawl jobs
        for (String domain : domains) {
            CrawlJob newJob = crawlJobDAO.save(new CrawlJob(domain, CrawlJob.CrawlService.BING));
            newJob.getCrawlRequests().add(crawlRequest);
        }
        return crawlRequest;
    }
}

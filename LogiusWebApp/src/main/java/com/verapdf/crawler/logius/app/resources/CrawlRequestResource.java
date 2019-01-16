package com.verapdf.crawler.logius.app.resources;


import com.verapdf.crawler.logius.app.tools.DomainUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.verapdf.crawler.logius.app.crawling.CrawlJob;
import com.verapdf.crawler.logius.app.crawling.CrawlRequest;
import com.verapdf.crawler.logius.app.db.CrawlJobDAO;
import com.verapdf.crawler.logius.app.db.CrawlRequestDAO;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value = "/crawl-requests", produces = MediaType.APPLICATION_JSON_VALUE)
public class CrawlRequestResource {
    private final CrawlRequestDAO crawlRequestDAO;
    private final CrawlJobDAO crawlJobDAO;
    private final CrawlJobResource crawlJobResource;

    public CrawlRequestResource(CrawlRequestDAO crawlRequestDAO, CrawlJobDAO crawlJobDAO, CrawlJobResource crawlJobResource) {
        this.crawlRequestDAO = crawlRequestDAO;
        this.crawlJobDAO = crawlJobDAO;
        this.crawlJobResource = crawlJobResource;
    }

    @PostMapping
    @Transactional
    public CrawlRequest createCrawlRequest(CrawlRequest crawlRequest,
                                           @RequestParam("cralwService") CrawlJob.CrawlService requestedService) {
        // Validate and pre-process input


        crawlRequest = new CrawlRequest();
        crawlRequest.setCrawlJobs(new ArrayList<>());
        crawlRequest.getCrawlJobs().add(new CrawlJob());
       // crawlRequest.getCrawlJobs().get(0).setDomain("https://logius.test-duallab.com/");
        crawlRequest.getCrawlJobs().get(0).setDomain("https://pdfa.org");
        List<String> domains = crawlRequest.getCrawlJobs().stream()
                .map(CrawlJob::getDomain)
                .map(DomainUtils::trimUrl).collect(Collectors.toList());
        System.out.println(crawlRequest);
        // Save request
        crawlRequest = crawlRequestDAO.save(crawlRequest);

        CrawlJob.CrawlService service = CrawlJob.CrawlService.HERITRIX;
        // Find jobs for domains requested earlier and link with this request
        List<CrawlJob> existingJobs = crawlJobDAO.findByDomain(domains);
        for (CrawlJob existingJob : existingJobs) {
            if (service != existingJob.getCrawlService()) {
                existingJob = crawlJobResource.restartCrawlJob(existingJob, existingJob.getDomain(), service);
            }
            domains.remove(existingJob.getDomain());
            existingJob.getCrawlRequests().add(crawlRequest);
        }

        // For domains that are left start new crawl jobs
        for (String domain : domains) {
            CrawlJob newJob = crawlJobDAO.save(new CrawlJob(domain, service));
            newJob.getCrawlRequests().add(crawlRequest);
            if (service == CrawlJob.CrawlService.HERITRIX) {
                crawlJobResource.startCrawlJob(newJob);
            }
        }
        System.out.println(1123);
        return crawlRequest;
    }
}

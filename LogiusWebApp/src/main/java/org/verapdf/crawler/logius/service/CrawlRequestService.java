package org.verapdf.crawler.logius.service;


import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.CrawlRequestDAO;
import org.verapdf.crawler.logius.tools.DomainUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrawlRequestService {
    private final CrawlRequestDAO crawlRequestDAO;
    private final CrawlJobDAO crawlJobDAO;

    public CrawlRequestService(CrawlRequestDAO crawlRequestDAO, CrawlJobDAO crawlJobDAO) {
        this.crawlRequestDAO = crawlRequestDAO;
        this.crawlJobDAO = crawlJobDAO;
    }

    @Transactional
    public CrawlRequest createCrawlRequest(CrawlRequest crawlRequest, CrawlJob.CrawlService crawlService, boolean isValidationRequared) {
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
            CrawlJob newJob = crawlJobDAO.save(new CrawlJob(domain, crawlService, isValidationRequared));
            newJob.getCrawlRequests().add(crawlRequest);
        }
        return crawlRequest;
    }
}

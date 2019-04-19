package org.verapdf.crawler.logius.service;


import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.CrawlRequestDAO;
import org.verapdf.crawler.logius.model.User;
import org.verapdf.crawler.logius.tools.DomainUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CrawlRequestService {
    private final CrawlRequestDAO crawlRequestDAO;
    private final CrawlJobDAO crawlJobDAO;
    private final CrawlJobService crawlJobService;
    private final UserService userService;
    private final CrawlService crawlService;

    public CrawlRequestService(CrawlRequestDAO crawlRequestDAO, CrawlJobDAO crawlJobDAO, CrawlJobService crawlJobService,
                               UserService userService, CrawlService crawlService) {
        this.crawlRequestDAO = crawlRequestDAO;
        this.crawlJobDAO = crawlJobDAO;
        this.crawlJobService = crawlJobService;
        this.userService = userService;
        this.crawlService = crawlService;
    }

    @Transactional
    public List<CrawlRequest> getCrawlRequests(String domain, UUID id){
        CrawlJob crawlJob = crawlJobService.getCrawlJob(domain, id);
        List<CrawlRequest> crawlRequests = crawlJob.getCrawlRequests();
        crawlRequests.forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());
        return crawlRequests;
    }

    @Transactional
    public CrawlRequest createCrawlRequest(CrawlRequest crawlRequest, UUID userId, CrawlJob.CrawlService crawlService, boolean isValidationRequared) {
        List<String> domains = extractDomains(crawlRequest);
        crawlRequest = crawlRequestDAO.save(crawlRequest);

        for (CrawlJob existingJob : crawlJobDAO.findByDomainsAndUserId(domains, userId)) {
            restartIfHasChanges(existingJob, crawlService, isValidationRequared);
            domains.remove(existingJob.getDomain());
            existingJob.getCrawlRequests().add(crawlRequest);
        }
        User user = userId == null ? null : userService.findUserById(userId);
        for (String domain : domains) {
            CrawlJob newJob = crawlJobDAO.save(new CrawlJob(domain, crawlService, isValidationRequared));
            newJob.getCrawlRequests().add(crawlRequest);
            newJob.setUser(user);
            if (crawlService == CrawlJob.CrawlService.HERITRIX) {
                this.crawlService.startCrawlJob(newJob);
            }
        }
        return crawlRequest;
    }

    private List<String> extractDomains(CrawlRequest crawlRequest) {
        return crawlRequest.getCrawlJobs().stream()
                .map(crawlJob -> DomainUtils.trimUrl(crawlJob.getDomain())).collect(Collectors.toList());
    }

    public void restartIfHasChanges(CrawlJob existingJob, CrawlJob.CrawlService crawlService, boolean isValidationRequared) {
        if (crawlService != existingJob.getCrawlService() || existingJob.isValidationEnabled() != isValidationRequared) {
            this.crawlService.restartCrawlJob(existingJob, existingJob.getDomain(), crawlService, isValidationRequared);
        }
    }
}

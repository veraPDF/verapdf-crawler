package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.HeritrixCleanerTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CrawlService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlJobService.class);

    private final HeritrixCleanerTask heritrixCleanerTask;
    private final BingService bingService;
    private final HeritrixClient heritrixClient;
    private final CrawlJobDAO crawlJobDAO;
    private final QueueManager queueManager;

    public CrawlService(HeritrixCleanerTask heritrixCleanerTask, BingService bingService, HeritrixClient heritrixClient,
                        CrawlJobDAO crawlJobDAO, QueueManager queueManager) {
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.bingService = bingService;
        this.heritrixClient = heritrixClient;
        this.crawlJobDAO = crawlJobDAO;
        this.queueManager = queueManager;
    }

    public CrawlJob restartCrawlJob(UUID userId, String domain){
        CrawlJob crawlJob = crawlJobDAO.findByDomainAndUserId(domain, userId);
        if (crawlJob == null){
            throw new NotFoundException(String.format("crawl job with userId %s and domain %s not found", userId, domain));
        }
        return restartCrawlJob(crawlJob, crawlJob.getCrawlService(), crawlJob.isValidationEnabled());
    }

    public void discardJob(CrawlJob crawlJob, CrawlJob.CrawlService service,  String heritrixJobId){
        switch (service) {
            case HERITRIX:
                heritrixCleanerTask.teardownAndClearHeritrixJob(heritrixJobId);
                break;
            case BING:
                bingService.discardJob(crawlJob);
                break;
            default:
                throw new IllegalStateException("Unsupported CrawlJob service");
        }

        queueManager.abortTasks(crawlJob);
        crawlJobDAO.remove(crawlJob);
    }

    public CrawlJob restartCrawlJob(CrawlJob crawlJob, CrawlJob.CrawlService service, boolean isValidationRequired) {
        List<CrawlRequest> crawlRequests;
        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService currentService = crawlJob.getCrawlService();
        // Keep requests list to link to new job
        crawlRequests = new ArrayList<>(crawlJob.getCrawlRequests());
        discardJob(crawlJob, currentService, heritrixJobId);

        // Create and start new crawl job
        CrawlJob newJob = new CrawlJob(crawlJob.getDomain(), service, isValidationRequired);
        newJob.setCrawlRequests(crawlRequests);
        newJob.setUser(crawlJob.getUser());
        crawlJobDAO.save(newJob);
        if (service == CrawlJob.CrawlService.HERITRIX) {
            startCrawlJob(newJob);
        }
        return newJob;
    }

    public void startCrawlJob(CrawlJob crawlJob) {
        try {
            heritrixClient.createJob(crawlJob);
            heritrixClient.buildJob(crawlJob.getHeritrixJobId());
            heritrixClient.launchJob(crawlJob.getHeritrixJobId());
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

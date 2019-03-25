package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.HeritrixCleanerTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CrawlService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlJobService.class);

    private final ValidationJobService validationJobService;
    private final ValidatorService validatorService;
    private final HeritrixCleanerTask heritrixCleanerTask;
    private final BingService bingService;
    private final HeritrixClient heritrixClient;
    private final CrawlJobDAO crawlJobDAO;

    public CrawlService(ValidationJobService validationJobService, ValidatorService validatorService,
                        HeritrixCleanerTask heritrixCleanerTask, BingService bingService, HeritrixClient heritrixClient, CrawlJobDAO crawlJobDAO) {
        this.validationJobService = validationJobService;
        this.validatorService = validatorService;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.bingService = bingService;
        this.heritrixClient = heritrixClient;
        this.crawlJobDAO = crawlJobDAO;
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

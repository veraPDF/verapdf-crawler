package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.core.email.SendEmailService;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.BingTask;
import org.verapdf.crawler.logius.core.tasks.MonitorCrawlJobStatusTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.CrawlRequestDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.exception.HeritrixException;

import java.util.Date;
import java.util.List;


@Service
public class MonitoringCrawlJobStatusService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorCrawlJobStatusTask.class);

    private static final int BATCH_SIZE = 20;
    private final CrawlJobDAO crawlJobDAO;
    private final HeritrixClient heritrixClient;
    private final BingTask bingTask;
    private final ValidationJobDAO validationJobDAO;
    private final SendEmailService sendEmailService;
    private final CrawlRequestDAO crawlRequestDAO;

    public MonitoringCrawlJobStatusService(CrawlJobDAO crawlJobDAO, HeritrixClient heritrixClient,
                                           BingTask bingTask, ValidationJobDAO validationJobDAO,
                                           SendEmailService sendEmailService, CrawlRequestDAO crawlRequestDAO) {
        this.crawlJobDAO = crawlJobDAO;
        this.heritrixClient = heritrixClient;
        this.bingTask = bingTask;
        this.validationJobDAO = validationJobDAO;
        this.sendEmailService = sendEmailService;
        this.crawlRequestDAO = crawlRequestDAO;
    }

    @Transactional
    public String checkJobsBatch(String lastDomain) {
        List<CrawlJob> runningJobs = crawlJobDAO.findByStatus(CrawlJob.Status.RUNNING, null, lastDomain, BATCH_SIZE);
        boolean containsRunningJobs = runningJobs != null && !runningJobs.isEmpty();
        if (containsRunningJobs) {
            runningJobs.forEach(this::checkJob);
        }

        checkCrawlRequests();
        return containsRunningJobs ? runningJobs.get(runningJobs.size() - 1).getDomain() : null;
    }


    private boolean checkJob(CrawlJob job) {
        try {
            // Check if Heritrix finished crawling
            CrawlJob.CrawlService service = job.getCrawlService();
            if (service == CrawlJob.CrawlService.HERITRIX) {
                String heritrixJobId = job.getHeritrixJobId();
                if (!heritrixClient.isJobFinished(heritrixJobId)) {
                    return false;
                }
            } else if (service == CrawlJob.CrawlService.BING) {
                CrawlJob currentJob = bingTask.getCurrentJob();
                if (currentJob != null && currentJob.getDomain().equals(job.getDomain())) {
                    return false;
                }
            }

            // Check if we have pending validation jobs
            Long validationJobsCount = validationJobDAO.count(job.getId());
            if (validationJobsCount > 0) {
                return false;
            }

            // If none above mark job as finished
            job.setFinished(true);
            job.setStatus(CrawlJob.Status.FINISHED);
            job.setFinishTime(new Date());
            logger.info("Crawling complete for " + job.getDomain());
            return true;
        } catch (Exception e) {
            logger.error("Fail to check status of job for " + job.getDomain());
            throw new HeritrixException(e);
        }
    }

    private void checkCrawlRequests() {
        List<CrawlRequest> crawlRequests = crawlRequestDAO.findActiveRequestsWithoutActiveJobs();
        for (CrawlRequest request : crawlRequests) {
            request.setFinished(true);
            if (request.getEmailAddress() != null && !request.getCrawlJobs().isEmpty()) {
                sendEmailService.sendFinishNotification(request);
            }
        }
    }
}

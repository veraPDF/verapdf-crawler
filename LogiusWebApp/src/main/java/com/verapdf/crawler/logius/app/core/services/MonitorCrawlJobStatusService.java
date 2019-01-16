package com.verapdf.crawler.logius.app.core.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.verapdf.crawler.logius.app.crawling.CrawlJob;
import com.verapdf.crawler.logius.app.crawling.CrawlRequest;
import com.verapdf.crawler.logius.app.core.email.SendEmail;
import com.verapdf.crawler.logius.app.core.heritrix.HeritrixClient;
import com.verapdf.crawler.logius.app.core.validation.ValidationService;
import com.verapdf.crawler.logius.app.db.CrawlJobDAO;
import com.verapdf.crawler.logius.app.db.CrawlRequestDAO;
import com.verapdf.crawler.logius.app.db.ValidationJobDAO;
import com.verapdf.crawler.logius.app.tools.AbstractService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */

@Service
public class MonitorCrawlJobStatusService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorCrawlJobStatusService.class);

    private static final long SLEEP_DURATION = 60 * 1000;
    private static final int BATCH_SIZE = 20;

    private final CrawlJobDAO crawlJobDAO;
    private final HeritrixClient heritrixClient;
    private final BingService bingService;
    private final ValidationJobDAO validationJobDAO;
    private final SendEmail sendEmail;
    private final CrawlRequestDAO crawlRequestDAO;

    public MonitorCrawlJobStatusService(CrawlJobDAO crawlJobDAO, HeritrixClient heritrixClient,
                                        BingService bingService, ValidationJobDAO validationJobDAO, SendEmail sendEmail,
                                        CrawlRequestDAO crawlRequestDAO) {
        super("MonitorCrawlJobStatusService", SLEEP_DURATION);

        this.crawlJobDAO = crawlJobDAO;
        this.heritrixClient = heritrixClient;
        this.bingService = bingService;
        this.validationJobDAO = validationJobDAO;
        this.sendEmail = sendEmail;
        this.crawlRequestDAO = crawlRequestDAO;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onRepeat() {
        String lastDomain = null;
        while (isRunning()) {
            lastDomain = checkJobsBatch(lastDomain);
            if (lastDomain == null) {
                break;
            }
        }
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    //@UnitOfWork
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
                boolean isCrawlingFinished = heritrixClient.isJobFinished(heritrixJobId);
                if (!isCrawlingFinished) {
                    return false;
                }
            } else if (service == CrawlJob.CrawlService.BING) {
                CrawlJob currentJob = bingService.getCurrentJob();
                if (currentJob != null && currentJob.getDomain().equals(job.getDomain())) {
                    return false;
                }
            }

            // Check if we have pending validation jobs
            Long validationJobsCount = validationJobDAO.count(job.getDomain());
            if (validationJobsCount > 0) {
                return false;
            }

            // If none above mark job as finished
            job.setFinished(true);
            job.setStatus(CrawlJob.Status.FINISHED);
            job.setFinishTime(new Date());
            if (service == CrawlJob.CrawlService.BING) {
                bingService.deleteTempFolder(job);
            }
            logger.info("Crawling complete for " + job.getDomain());
            return true;
        } catch (Exception e) {
            logger.error("Fail to check status of job for " + job.getDomain(), e);
            return false;
        }
    }

    private void checkCrawlRequests() {
        List<CrawlRequest> crawlRequests = crawlRequestDAO.findActiveRequestsWithoutActiveJobs();
        for (CrawlRequest request : crawlRequests) {
            request.setFinished(true);
            if (request.getEmailAddress() != null) {
                sendEmail.sendFinishNotification(request);
            }
        }
    }
}

package org.verapdf.crawler.core.services;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.configurations.EmailServerConfiguration;
import org.verapdf.crawler.core.email.SendEmail;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.CrawlRequestDAO;
import org.verapdf.crawler.db.ValidationJobDAO;
import org.verapdf.crawler.tools.AbstractService;

import java.util.Date;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class MonitorCrawlJobStatusService extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(MonitorCrawlJobStatusService.class);

	private static final long SLEEP_DURATION = 60*1000;
    private static final int BATCH_SIZE = 20;

	private final CrawlJobDAO crawlJobDAO;
	private final CrawlRequestDAO crawlRequestDAO;
	private final ValidationJobDAO validationJobDAO;
	private final HeritrixClient heritrixClient;

	public MonitorCrawlJobStatusService(CrawlJobDAO crawlJobDAO, CrawlRequestDAO crawlRequestDAO, ValidationJobDAO validationJobDAO, HeritrixClient heritrixClient) {
		super("MonitorCrawlJobStatusService", SLEEP_DURATION);
		this.crawlJobDAO = crawlJobDAO;
		this.crawlRequestDAO = crawlRequestDAO;
		this.validationJobDAO = validationJobDAO;
		this.heritrixClient = heritrixClient;
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
	@UnitOfWork
    public String checkJobsBatch(String lastDomain) {
        List<CrawlJob> runningJobs = crawlJobDAO.findByStatus(CrawlJob.Status.RUNNING, lastDomain, BATCH_SIZE);
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
            String heritrixJobId = job.getHeritrixJobId();
            boolean isCrawlingFinished = heritrixClient.isJobFinished(heritrixJobId);
            if (!isCrawlingFinished) {
                return false;
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
                SendEmail.sendFinishNotification(request);
            }
		}
	}
}

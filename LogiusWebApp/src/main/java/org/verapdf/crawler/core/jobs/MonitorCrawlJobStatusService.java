package org.verapdf.crawler.core.jobs;

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

import java.util.Date;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class MonitorCrawlJobStatusService implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(MonitorCrawlJobStatusService.class);

    private static final int BATCH_SIZE = 20;

	private final CrawlJobDAO crawlJobDAO;
	private final CrawlRequestDAO crawlRequestDAO;
	private final ValidationJobDAO validationJobDAO;
	private final HeritrixClient heritrixClient;
	private final EmailServerConfiguration emailServerConfiguration;
	private boolean running;

	public MonitorCrawlJobStatusService(CrawlJobDAO crawlJobDAO, CrawlRequestDAO crawlRequestDAO, ValidationJobDAO validationJobDAO, HeritrixClient heritrixClient, EmailServerConfiguration emailServerConfiguration) {
		running = false;
		this.crawlJobDAO = crawlJobDAO;
		this.crawlRequestDAO = crawlRequestDAO;
		this.validationJobDAO = validationJobDAO;
		this.heritrixClient = heritrixClient;
		this.emailServerConfiguration = emailServerConfiguration;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		running = true;
		new Thread(this, "Thread-MonitorCrawlJobStatusService").start();
	}

	@Override
	public void run() {
		logger.info("Crawl Job service started");
		while (running) {
			try {
                String lastDomain = null;
                while (running) {
                    lastDomain = checkJobsBatch(lastDomain);
                    if (lastDomain == null) {
                        break;
                    }
                }
			} catch (Exception e) {
				logger.error("Domains check failed", e);
			}
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

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
                SendEmail.sendFinishNotification(request, emailServerConfiguration);
            }
		}
	}
}

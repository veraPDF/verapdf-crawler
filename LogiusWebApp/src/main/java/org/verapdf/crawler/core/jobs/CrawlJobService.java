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
import org.verapdf.crawler.db.DocumentDAO;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class CrawlJobService implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CrawlJobService.class);

	private final CrawlJobDAO crawlJobDAO;
	private final CrawlRequestDAO crawlRequestDAO;
	private final DocumentDAO documentDAO;
	private final HeritrixClient heritrixClient;
	private final EmailServerConfiguration emailServerConfiguration;
	private boolean running;

	public CrawlJobService(CrawlJobDAO crawlJobDAO, CrawlRequestDAO crawlRequestDAO, DocumentDAO documentDAO, HeritrixClient heritrixClient, EmailServerConfiguration emailServerConfiguration) {
		running = false;
		this.crawlJobDAO = crawlJobDAO;
		this.crawlRequestDAO = crawlRequestDAO;
		this.documentDAO = documentDAO;
		this.heritrixClient = heritrixClient;
		this.emailServerConfiguration = emailServerConfiguration;
	}

	public void start() {
		running = true;
		new Thread(this, "Thread-CrawlJobService").start();
	}

	@Override
	public void run() {
		logger.info("Crawl Job service started");
		while (running) {
			try {
				checkAllDomains();
			} catch (ParserConfigurationException | SAXException | IOException e) {
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
	private void checkAllDomains() throws ParserConfigurationException, SAXException, IOException {
		List<CrawlJob> runningJobs = crawlJobDAO.findByStatus(CrawlJob.Status.RUNNING);

		for (CrawlJob job : runningJobs) {
			String heritrixJobId = job.getHeritrixJobId();
			boolean isFinished = heritrixClient.isJobFinished(heritrixJobId);
			if (isFinished) {
				Boolean isAllFinished = documentDAO.isAllFinishedByDomain(job.getDomain());
				if (isAllFinished) {
					job.setFinished(true);
					job.setStatus(CrawlJob.Status.FINISHED);
					checkNotificationRequirements(job);
				}
			}
		}
	}

	private void checkNotificationRequirements(CrawlJob job) {
		List<CrawlRequest> crawlRequests = crawlRequestDAO.getUncheckedFinishedCrawlRequestContainingDomain(job.getDomain());
		for (CrawlRequest request : crawlRequests) {
			request.setFinished(true);
			SendEmail.sendFinishNotification(request, emailServerConfiguration);
		}
	}
}

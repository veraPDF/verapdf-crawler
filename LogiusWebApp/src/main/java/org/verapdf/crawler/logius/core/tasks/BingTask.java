package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.email.SendEmailService;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.service.BingService;
import org.verapdf.crawler.logius.service.CrawlJobService;

@Component
public class BingTask extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(BingTask.class);
	private static final long SLEEP_DURATION = 1000;
	private final CrawlJobService crawlJobService;
	private final BingService bingService;

	public BingTask(SendEmailService email, CrawlJobService crawlJobService, BingService bingService) {
		super(SLEEP_DURATION, email);
		this.crawlJobService = crawlJobService;
		this.bingService = bingService;
	}

	@Override
	protected void process() {
		CrawlJob crawlJob = crawlJobService.getNewBingJob();
		bingService.startNewJob(crawlJob);
	}
}

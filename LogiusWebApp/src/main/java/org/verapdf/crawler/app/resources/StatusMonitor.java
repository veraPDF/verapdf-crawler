package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;

public class StatusMonitor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final CrawlRequestDao crawlRequestDao;
    private final ControlResource controlResource;

    StatusMonitor(CrawlRequestDao crawlRequestDao, ControlResource controlResource) {
        this.crawlRequestDao = crawlRequestDao;
        this.controlResource = controlResource;
    }

    @Override
    public void run() {
        while(true) {
            try {
                for (CrawlRequest batch : crawlRequestDao.getBatchJobs()) {
                    controlResource.getBatchJob(batch.getId());
                }

            }
            catch (Exception e) {
                logger.error("Status monitor error", e);
            }
            try {
                Thread.sleep(5 * 60 * 1000);
            } catch (InterruptedException e) {
                logger.error("Status monitor error", e);
            }
        }
    }
}

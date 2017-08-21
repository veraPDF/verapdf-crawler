package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.repository.jobs.BatchJobDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

public class StatusMonitor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final BatchJobDao batchJobDao;
    private final ControlResource controlResource;

    StatusMonitor(BatchJobDao batchJobDao, ControlResource controlResource) {
        this.batchJobDao = batchJobDao;
        this.controlResource = controlResource;
    }

    @Override
    public void run() {
        while(true) {
            try {
                for (BatchJob batch : batchJobDao.getBatchJobs()) {
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

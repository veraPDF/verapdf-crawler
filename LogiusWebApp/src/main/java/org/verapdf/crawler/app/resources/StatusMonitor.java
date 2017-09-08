package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;

public class StatusMonitor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final CrawlJobDao crawlJobDao;
    private final CrawlJobResource crawlJobResource;

    StatusMonitor(CrawlJobDao crawlJobDao, CrawlJobResource crawlJobResource) {
        this.crawlJobDao = crawlJobDao;
        this.crawlJobResource = crawlJobResource;
    }

    @Override
    public void run() {
        while(true) {
            try {
                for (String domain: crawlJobDao.getActiveDomains()) {
                    crawlJobResource.getCrawlJob(domain);
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

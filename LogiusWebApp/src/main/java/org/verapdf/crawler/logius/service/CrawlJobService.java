package org.verapdf.crawler.logius.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.CrawlJobDAO;

import java.util.List;

@Service
public class CrawlJobService {
    private final CrawlJobDAO crawlJobDAO;

    public CrawlJobService(CrawlJobDAO crawlJobDAO) {
        this.crawlJobDAO = crawlJobDAO;
    }

    @Transactional
    public CrawlJob getNewBingJob() {
        List<CrawlJob> newJob = crawlJobDAO.findByStatus(CrawlJob.Status.NEW, CrawlJob.CrawlService.BING, null, 1);
        if (newJob != null && !newJob.isEmpty()) {
            CrawlJob crawlJob = newJob.get(0);
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
            return crawlJob;
        }
        return null;
    }
}

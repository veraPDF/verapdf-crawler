package org.verapdf.crawler.logius.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.CrawlJobDAO;

import java.util.List;
import java.util.UUID;

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

    @Transactional
    public List<CrawlJob> findNotFinishedJobs(String domainFilter, int start, int limit) {
        return crawlJobDAO.findNotFinishedJobs(domainFilter, start, limit);
    }

    @Transactional
    public long count(String domainFilter, boolean isFinished){
        return crawlJobDAO.count(domainFilter, isFinished);
    }

    @Transactional
    public boolean isCanRestartJob(UUID userId, CrawlJob crawlJob) {
        List<CrawlJob> startedCrawlJobs = crawlJobDAO.findByStatusAndUserId(CrawlJob.Status.RUNNING, userId);
        if (startedCrawlJobs.isEmpty()) {
            return true;
        } else {
            return startedCrawlJobs.stream()
                    .filter(crawl -> crawl.getId()
                            .equals(crawlJob.getId())).limit(1).findFirst().isPresent();
        }
    }

    @Transactional
    public boolean isCanStartJob(UUID userId) {
        List<CrawlJob> startedCrawlJobs = crawlJobDAO.findByStatusAndUserId(CrawlJob.Status.RUNNING, userId);
        return (userId == null && startedCrawlJobs.size() < 2) || (userId != null && startedCrawlJobs.isEmpty());
    }
}

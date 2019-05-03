package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.core.tasks.BingTask;
import org.verapdf.crawler.logius.core.tasks.HeritrixCleanerTask;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.crawling.CrawlRequest;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.exception.NotFoundException;
import org.verapdf.crawler.logius.monitoring.CrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.HeritrixCrawlJobStatus;
import org.verapdf.crawler.logius.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.logius.tools.DomainUtils;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class CrawlJobService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlJobService.class);
    private static final int GET_STATUS_MAX_DOCUMENT_COUNT = 10;
    private final CrawlJobDAO crawlJobDAO;
    private final ValidationJobDAO validationJobDAO;
    private final HeritrixClient heritrixClient;
    private final HeritrixCleanerTask heritrixCleanerTask;
    private final BingTask bingTask;
    private final QueueManager queueManager;

    public CrawlJobService(CrawlJobDAO crawlJobDAO, ValidationJobDAO validationJobDAO, HeritrixClient heritrixClient,
                           HeritrixCleanerTask heritrixCleanerTask, BingTask bingTask, QueueManager queueManager) {
        this.crawlJobDAO = crawlJobDAO;
        this.validationJobDAO = validationJobDAO;
        this.heritrixClient = heritrixClient;
        this.heritrixCleanerTask = heritrixCleanerTask;
        this.bingTask = bingTask;
        this.queueManager = queueManager;
    }


    @Transactional
    public List<CrawlJob> findNotFinishedJobs(String domainFilter, int start, int limit) {
        return crawlJobDAO.findNotFinishedJobs(domainFilter, start, limit);
    }

    @Transactional
    public long count(String domainFilter, boolean isFinished) {
        return crawlJobDAO.count(domainFilter, isFinished);
    }

    @Transactional
    public CrawlJob update(CrawlJob update) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        CrawlJob job = getCrawlJob(update.getId());
        return update(update, job);
    }

    @Transactional
    public CrawlJob update(CrawlJob update, UUID id) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        CrawlJob job = getCrawlJob(update.getDomain(), id);
        return update(update, job);
    }

    @Transactional
    public CrawlJob update(CrawlJob update, CrawlJob crawlJob) throws IOException, XPathExpressionException,
            SAXException, ParserConfigurationException {

        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        if (crawlJob.getStatus() == CrawlJob.Status.RUNNING && update.getStatus() == CrawlJob.Status.PAUSED) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.pauseJob(heritrixJobId);
            }
            validationJobDAO.pause(crawlJob.getId());
            crawlJob.setStatus(CrawlJob.Status.PAUSED);
        }
        if (crawlJob.getStatus() == CrawlJob.Status.PAUSED && update.getStatus() == CrawlJob.Status.RUNNING) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrixClient.isJobFinished(heritrixJobId)) {
                heritrixClient.unpauseJob(heritrixJobId);
            }
            validationJobDAO.unpause(crawlJob.getId());
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        }

        return crawlJob;
    }


    @Transactional
    public void cancelCrawlJob(UUID id) {
        CrawlJob crawlJob = getCrawlJob(id);
        discardJob(crawlJob, crawlJob.getCrawlService(), crawlJob.getHeritrixJobId());
    }

    @Transactional
    public void cancelCrawlJob(UUID id, String domain) {
        CrawlJob crawlJob = getCrawlJob(domain, id);
        discardJob(crawlJob, crawlJob.getCrawlService(), crawlJob.getHeritrixJobId());
    }

    @Transactional
    public CrawlJob getCrawlJob(String domain, UUID userId) {
        domain = DomainUtils.trimUrl(domain);
        CrawlJob job = crawlJobDAO.findByDomainAndUserId(domain, userId);
        if (job == null) {
            throw new NotFoundException(String.format("crawl job with domain %s not found", domain));
        }
        return job;
    }

    @Transactional
    public CrawlJob getCrawlJob(UUID id) {
        CrawlJob job = crawlJobDAO.findById(id);
        if (job == null) {
            throw new NotFoundException(String.format("crawl job with uuid %s not found", id));
        }
        return job;
    }

    @Transactional
    public long count(String domainFilter, UUID id, Boolean finished) {
        return crawlJobDAO.count(domainFilter, id, finished);
    }

    @Transactional
    public List<CrawlJob> find(String domainFilter, UUID id, Boolean finished, int startParam, int limitParam) {
        return crawlJobDAO.find(domainFilter, id, finished, startParam, limitParam);
    }

    @Transactional
    public CrawlJob unlinkCrawlRequests(String domain, UUID id, String email) {
        CrawlJob crawlJob = getCrawlJob(domain, id);
        crawlJob.getCrawlRequests().removeIf(request -> email.equals(request.getEmailAddress()));
        return crawlJob;
    }

    @Transactional
    public CrawlJobStatus getFullJobStatus(UUID id) {
        CrawlJob crawlJob = getCrawlJob(id);
        return getStatus(crawlJob);
    }

    @Transactional
    public CrawlJobStatus getFullJobStatus(String domain, UUID id) {
        CrawlJob crawlJob = getCrawlJob(domain, id);
        return getStatus(crawlJob);
    }


    private CrawlJobStatus getStatus(CrawlJob crawlJob){
        HeritrixCrawlJobStatus heritrixStatus = null;
        CrawlJob.CrawlService crawlService = crawlJob.getCrawlService();

        switch (crawlService) {
            case HERITRIX:
                try {
                    heritrixStatus = heritrixClient.getHeritrixStatus(crawlJob.getHeritrixJobId());
                } catch (Throwable e) {
                    logger.error("Error during obtaining heritrix status", e);
                    heritrixStatus = new HeritrixCrawlJobStatus("Unavailable: " + e.getMessage(), null, null);
                }
                break;
            case BING:
                //TODO: fix this
                heritrixStatus = null;
                break;
            default:
                throw new IllegalStateException("CrawlJob service can't be null");
        }

        UUID crawlJobId = crawlJob.getId();
        Long count = validationJobDAO.count(crawlJobId);
        List<ValidationJob> topDocuments = validationJobDAO.getDocuments(crawlJobId, GET_STATUS_MAX_DOCUMENT_COUNT);
        crawlJob.getCrawlRequests().forEach(crawlRequest -> crawlRequest.getCrawlJobs().size());

        return new CrawlJobStatus(crawlJob, heritrixStatus, new ValidationQueueStatus(count, topDocuments));
    }

    @Transactional
    public CrawlJob restartCrawlJob(UUID id) {
        CrawlJob crawlJob = getCrawlJob(id);
        return restartCrawlJob(crawlJob, crawlJob.getCrawlService(), crawlJob.isValidationEnabled());
    }

    public CrawlJob restartCrawlJob(UUID userId, String domain){
        CrawlJob crawlJob = crawlJobDAO.findByDomainAndUserId(domain, userId);
        if (crawlJob == null){
            throw new NotFoundException(String.format("crawl job with userId %s and domain %s not found", userId, domain));
        }
        return restartCrawlJob(crawlJob, crawlJob.getCrawlService(), crawlJob.isValidationEnabled());
    }

    private void discardJob(CrawlJob crawlJob, CrawlJob.CrawlService service,  String heritrixJobId){
        switch (service) {
            case HERITRIX:
                heritrixCleanerTask.teardownAndClearHeritrixJob(heritrixJobId);
                break;
            case BING:
                bingTask.discardJob(crawlJob);
                break;
            default:
                throw new IllegalStateException("Unsupported CrawlJob service");
        }

        queueManager.abortTasks(crawlJob);
        crawlJobDAO.remove(crawlJob);
    }

    public CrawlJob restartCrawlJob(CrawlJob crawlJob, CrawlJob.CrawlService service, boolean isValidationRequired) {
        List<CrawlRequest> crawlRequests;
        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService currentService = crawlJob.getCrawlService();
        // Keep requests list to link to new job
        crawlRequests = new ArrayList<>(crawlJob.getCrawlRequests());
        discardJob(crawlJob, currentService, heritrixJobId);

        // Create and start new crawl job
        CrawlJob newJob = new CrawlJob(crawlJob.getDomain(), service, isValidationRequired);
        newJob.setCrawlRequests(crawlRequests);
        newJob.setUser(crawlJob.getUser());
        crawlJobDAO.save(newJob);
        if (service == CrawlJob.CrawlService.HERITRIX) {
            startCrawlJob(newJob);
        }
        return newJob;
    }

    public void startCrawlJob(CrawlJob crawlJob) {
        try {
            heritrixClient.createJob(crawlJob);
            heritrixClient.buildJob(crawlJob.getHeritrixJobId());
            heritrixClient.launchJob(crawlJob.getHeritrixJobId());
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        } catch (Exception e) {
            logger.error("Failed to start crawling job for domain " + crawlJob.getDomain(), e);
            crawlJob.setFinished(true);
            crawlJob.setFinishTime(new Date());
            crawlJob.setStatus(CrawlJob.Status.FAILED);
        }
        // TODO: cleanup heritrix in finally
    }
}

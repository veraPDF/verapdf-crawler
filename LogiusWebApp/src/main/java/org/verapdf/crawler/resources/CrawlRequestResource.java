package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.CrawlRequestDAO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/crawl-requests")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlRequestResource {
    private static final Logger logger = LoggerFactory.getLogger(CrawlRequestResource.class);
    private final HeritrixClient heritrix;
    private final CrawlRequestDAO crawlRequestDao;
    private final CrawlJobDAO crawlJobDao;

    public CrawlRequestResource(HeritrixClient heritrix, CrawlRequestDAO crawlRequestDao, CrawlJobDAO crawlJobDao) {
        this.heritrix = heritrix;
        this.crawlRequestDao = crawlRequestDao;
        this.crawlJobDao = crawlJobDao;
    }

    @POST
    @UnitOfWork
    public CrawlRequest createCrawlRequest(@NotNull @Valid CrawlRequest crawlRequest) {
        // Validate and pre-process input
        List<String> domains = crawlRequest.getDomains().stream().map(this::trimUrl).collect(Collectors.toList());

        // Save request
        crawlRequest = crawlRequestDao.save(crawlRequest);

        // Find jobs for domains requested earlier and link with this request
        List<CrawlJob> existingJobs = crawlJobDao.findByDomain(domains);
        for (CrawlJob existingJob: existingJobs) {
            domains.remove(existingJob.getDomain());
            existingJob.getCrawlRequests().add(crawlRequest);
        }

        // For domains that are left start new crawl jobs
        for (String domain: domains) {
            CrawlJob newJob = crawlJobDao.save(new CrawlJob(domain));
            newJob.getCrawlRequests().add(crawlRequest);
            startCrawlJob(newJob);
        }
        return crawlRequest;
    }

    private void startCrawlJob(CrawlJob crawlJob) {
        try {
            heritrix.createJob(crawlJob.getHeritrixJobId(), crawlJob.getDomain());
            heritrix.buildJob(crawlJob.getHeritrixJobId());
            heritrix.launchJob(crawlJob.getHeritrixJobId());
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        } catch (Exception e) {
            logger.error("Failed to start crawling job for domain " + crawlJob.getDomain(), e);
            crawlJob.setFinished(true);
            crawlJob.setFinishTime(new Date());
            crawlJob.setStatus(CrawlJob.Status.FAILED);
        }
        // TODO: cleanup heritrix in finally
    }

//    private void startCrawlJob(String domain){
//        try {
//            if (!crawlJobDao.doesJobExist(domain)) {
//                String id = UUID.randomUUID().toString();
//                crawlJobDao.addJob(new CrawlJob(id, "", domain, new Date()));
//                heritrix.createJob(id, domain);
//                heritrix.buildJob(id);
//                heritrix.launchJob(id);
//            }
//        }
//        catch (Exception e) {
//            logger.error("Error on job creation", e);
//        }
//    }

    private String trimUrl(String url) {
        if(url.contains("://")) {
            url = url.substring(url.indexOf("://") + 3);
        }
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        if(url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        return url;
    }
}

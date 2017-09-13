package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Path("/crawl-requests")
public class CrawlRequestResource {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final HeritrixClient client;
    private final CrawlRequestDao crawlRequestDao;
    private final CrawlJobDao crawlJobDao;

    public CrawlRequestResource(HeritrixClient client, CrawlRequestDao crawlRequestDao, CrawlJobDao crawlJobDao) {
        this.client = client;
        this.crawlRequestDao = crawlRequestDao;
        this.crawlJobDao = crawlJobDao;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public CrawlRequest createCrawlRequest(CrawlRequest crawlRequest) {
        String id = UUID.randomUUID().toString();
        crawlRequest.setId(id);
        List<String> rawDomains = crawlRequest.getDomains();
        List<String> domains = new ArrayList<>();
        for(String rawDomain : rawDomains) {
            String domain = trimUrl(rawDomain);
            domains.add(domain);
            startCrawlJob(domain);
        }
        crawlRequest.setDomains(domains);
        crawlRequestDao.addCrawlRequest(crawlRequest);
        return crawlRequest;
    }

    private void startCrawlJob(String domain){
        try {
            if (!crawlJobDao.doesJobExist(domain)) {
                String id = UUID.randomUUID().toString();
                crawlJobDao.addJob(new CrawlJob(id, "", domain, new Date()));
                client.createJob(id, domain);
                client.buildJob(id);
                client.launchJob(id);
            }
        }
        catch (Exception e) {
            logger.error("Error on job creation", e);
        }
    }

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

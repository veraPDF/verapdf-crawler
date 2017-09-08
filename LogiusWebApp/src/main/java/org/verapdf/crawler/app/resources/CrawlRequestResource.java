package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public CrawlRequest createCrawlRequest(CrawlRequest jobData) {
        // todo: add list of links to created/linked CrawlJob
        String id = UUID.randomUUID().toString();
        jobData.setId(id);
        List<String> domains = jobData.getCrawlJobs();
        jobData.setCrawlJobs(new ArrayList<>());
        logger.info("Batch job creation on domains: " + String.join(", ", domains));
        for(String domain : domains) {
            jobData.getCrawlJobs().add(startCrawlJob(domain));
        }
        crawlRequestDao.addBatchJob(jobData);
        return jobData;
    }

    private String startCrawlJob(String domain){
        try {
            String url = trimUrl(domain);
            if (crawlJobDao.doesJobExist(url)) { // This URL has already been crawled
                return crawlJobDao.getIdByUrl(url);
            } else {
                // Brand new URL
                ArrayList<String> list = new ArrayList<>();
                list.add(url);
                if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                    list.add(list.get(0).replace("https://", "http://"));
                }

                String id = UUID.randomUUID().toString();
                crawlJobDao.addJob(new CrawlJob(id, "", url, LocalDateTime.now()));
                client.createJob(id, list);
                client.buildJob(id);
                client.launchJob(id);
                logger.info("Job creation on " + domain);
                return id;
            }
        }
        catch (Exception e) {
            logger.error("Error on job creation", e);
            return "";
        }
    }

    private String trimUrl(String url) {
        if(!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if(url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if(url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}

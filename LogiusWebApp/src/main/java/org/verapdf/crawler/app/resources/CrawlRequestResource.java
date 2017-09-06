package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.domain.crawling.CrawlRequestData;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    public CrawlRequest createCrawlRequest(CrawlRequestData jobData) {
        // todo: add list of links to created/linked CrawlJob
        String id = UUID.randomUUID().toString();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        CrawlRequest batch = new CrawlRequest(id, jobData.getReportEmail(),
                LocalDateTime.of(LocalDate.parse(jobData.getDate(), dateFormatter), LocalTime.MIN));
        logger.info("Batch job creation on domains: " + String.join(", ",jobData.getDomains()));
        for(String domain : jobData.getDomains()) {
            batch.getCrawlJobs().add(startCrawlJob(domain));
        }
        crawlRequestDao.addBatchJob(batch);
        return batch;
    }

    private String startCrawlJob(String domain){
        try {
            if (crawlJobDao.doesJobExist(trimUrl(domain))) { // This URL has already been crawled
                return crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(domain)).getId();
            } else {
                // Brand new URL
                ArrayList<String> list = new ArrayList<>();
                if (domain.startsWith("http://") || domain.startsWith("https://")) {
                    list.add(trimUrl(domain));
                } else {
                    list.add(trimUrl(domain));
                    list.add(list.get(0).replace("https://", "http://"));
                }

                String id = UUID.randomUUID().toString();
                client.createJob(id, list);
                client.buildJob(id);
                client.launchJob(id);
                crawlJobDao.addJob(new CrawlJob(id, "", trimUrl(domain), LocalDateTime.now()));
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

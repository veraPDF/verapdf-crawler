package org.verapdf.crawler.app.resources;

import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.domain.crawling.StartBatchJobData;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


@Path("/crawl-requests")
public class CrawlRequestResource {

    @POST
    public StartBatchJobData createCrawlRequest(StartBatchJobData jobData) {
        // todo: rename StartBatchJobData + BatchJob to CrawlRequest
        // todo: add list of links to created/linked CrawlJob
        return null;
    }
}

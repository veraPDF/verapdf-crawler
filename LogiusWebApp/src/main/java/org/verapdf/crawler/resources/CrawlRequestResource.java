package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.CrawlRequestDAO;
import org.verapdf.crawler.tools.DomainUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Path("/crawl-requests")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlRequestResource {
    private static final Logger logger = LoggerFactory.getLogger(CrawlRequestResource.class);
    private final HeritrixClient heritrix;
    private final CrawlRequestDAO crawlRequestDao;
    private final CrawlJobDAO crawlJobDao;

    public CrawlRequestResource(CrawlRequestDAO crawlRequestDao, CrawlJobDAO crawlJobDao, HeritrixClient heritrix) {
        this.heritrix = heritrix;
        this.crawlRequestDao = crawlRequestDao;
        this.crawlJobDao = crawlJobDao;
    }

    @POST
    @UnitOfWork
    public CrawlRequest createCrawlRequest(@NotNull @Valid CrawlRequest crawlRequest) {
        // Validate and pre-process input
        List<String> domains = crawlRequest.getCrawlJobs().stream()
                .map(CrawlJob::getDomain)
                .map(DomainUtils::trimUrl).collect(Collectors.toList());

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
            CrawlJobResource.startCrawlJob(newJob, heritrix);
        }
        return crawlRequest;
    }
}

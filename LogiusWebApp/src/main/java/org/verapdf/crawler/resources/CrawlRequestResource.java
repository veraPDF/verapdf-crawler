package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.ResourceManager;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Path("/crawl-requests")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlRequestResource {
    private final ResourceManager resourceManager;

    public CrawlRequestResource(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @POST
    @UnitOfWork
    public CrawlRequest createCrawlRequest(@NotNull @Valid CrawlRequest crawlRequest,
                                           @QueryParam("cralwService") CrawlJob.CrawlService requestedService) {
        // Validate and pre-process input
        List<String> domains = crawlRequest.getCrawlJobs().stream()
                .map(CrawlJob::getDomain)
                .map(DomainUtils::trimUrl).collect(Collectors.toList());

        // Save request
        crawlRequest = resourceManager.getCrawlRequestDAO().save(crawlRequest);

        CrawlJob.CrawlService service = requestedService == null ? CrawlJob.CrawlService.HERITRIX : requestedService;
        // Find jobs for domains requested earlier and link with this request
        List<CrawlJob> existingJobs = resourceManager.getCrawlJobDAO().findByDomain(domains);
        for (CrawlJob existingJob: existingJobs) {
            if (service != existingJob.getCrawlService()) {
                existingJob = CrawlJobResource.restartCrawlJob(existingJob, existingJob.getDomain(), service, resourceManager);
            }
            domains.remove(existingJob.getDomain());
            existingJob.getCrawlRequests().add(crawlRequest);
        }

        // For domains that are left start new crawl jobs
        for (String domain: domains) {
            CrawlJob newJob = resourceManager.getCrawlJobDAO().save(new CrawlJob(domain, service));
            newJob.getCrawlRequests().add(crawlRequest);
            if (service == CrawlJob.CrawlService.HERITRIX) {
                CrawlJobResource.startCrawlJob(newJob, resourceManager.getHeritrixClient());
            }
        }
        return crawlRequest;
    }
}

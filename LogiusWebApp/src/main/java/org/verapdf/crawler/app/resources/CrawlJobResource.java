package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Path("/crawl-jobs")
public class CrawlJobResource {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final CrawlJobDao crawlJobDao;
    private final HeritrixClient client;
    private final CrawlRequestDao crawlRequestDao;
    private final HeritrixReporter reporter;
    private final EmailServer emailServer;

    public CrawlJobResource(CrawlJobDao crawlJobDao, HeritrixClient client, CrawlRequestDao crawlRequestDao, HeritrixReporter reporter, EmailServer emailServer) {
        this.crawlJobDao = crawlJobDao;
        this.client = client;
        this.crawlRequestDao = crawlRequestDao;
        this.reporter = reporter;
        this.emailServer = emailServer;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobList(@QueryParam("domainFilter") String domainFilter,
                               @QueryParam("start") Integer start,
                               @QueryParam("limit") Integer limit) {
        List<CrawlJob> crawlJobs = crawlJobDao.getAllJobsWithFilter(domainFilter);
        if(start != null && limit != null) {
            return Response.ok(crawlJobs.subList(start, start + limit)).
                    header("X-Total-Count", crawlJobDao.countJobsWithFilter(domainFilter)).build();
        }
        return Response.ok(crawlJobs).header("X-Total-Count", crawlJobDao.countJobsWithFilter(domainFilter)).build();
    }

    @POST
    @Path("/{domain}")
    public CrawlJob restartCrawlJob(@PathParam("domain") String domain) {
        try {
            CrawlJob crawlJob = crawlJobDao.getCrawlJobByCrawlUrl(domain);
            List<String> list = new ArrayList<>();
            list.add(crawlJob.getCrawlURL());
            crawlJobDao.setJobFinished(domain, false);
            client.teardownJob(domain);
            client.createJob(domain, list);
            client.buildJob(domain);
            client.launchJob(domain);
            logger.info("Crawl job on "+ crawlJob.getCrawlURL() + " restarted");
            return getCrawlJob(domain);
        }
        catch (Exception e) {
            logger.error("Error restarting job", e);
            return null;
        }
    }

    @GET
    @Path("/{domain}")
    public CrawlJob getCrawlJob(@PathParam("domain") String domain) throws IOException, SAXException, ParserConfigurationException {
        CrawlJob crawlJob = crawlJobDao.getCrawlJobByCrawlUrl(domain);
        if(!crawlJob.isFinished()) {
            crawlJob.setStatus(reporter.getCrawlJobStatus(crawlJob.getId()));
            if(crawlJob.getStatus().startsWith("finished") || crawlJob.getStatus().startsWith("aborted")) {
                setJobFinished(crawlJob);
            }
        }
        return crawlJob;
    }

    @PUT
    @Path("/{domain}")
    public CrawlJob updateCrawlJob(@PathParam("domain") String domain, CrawlJob update) {
        // todo: apply updates to the job, e.g. change status
        // Currently only pause/unpause is available
        CrawlJob crawlJob = crawlJobDao.getCrawlJobByCrawlUrl(domain);
        if(crawlJob.getStatus().equals("running") && update.getStatus().equals("paused")) {
            pauseJob(crawlJob.getId());
            crawlJob.setStatus("paused");
        }
        if(crawlJob.getStatus().equals("paused") && update.getStatus().equals("running")) {
            pauseJob(crawlJob.getId());
            crawlJob.setStatus("running");
        }
        return crawlJob;
    }

    @GET
    @Path("/{domain}/requests")
    public List<CrawlRequest> getCrawlJobRequests(@PathParam("domain") String domain) {
        return crawlRequestDao.getCrawlRequestsForCrawlJob(crawlJobDao.getCrawlJobByCrawlUrl(domain).getId());
    }

    @DELETE
    @Path("/{domain}/requests")
    public List<CrawlRequest> unlinkCrawlRequests(@PathParam("domain") String domain, @QueryParam("email") String email) {
        // todo: unlink all CrawlRequests with specified email from CrawlJob
        // todo: clarify if possible/required to terminate CrawlJob if no associated CrawlRequests left
        return null;
    }

    @GET
    @Path("/{domain}/documents")
    public List<Object> getDomainDocuments(@PathParam("domain") String domain,
                                           @QueryParam("startDate") String startDate,
                                           @QueryParam("type") String type,
                                           @QueryParam("start") Integer start,
                                           @QueryParam("limit") Integer limit,
                                           @QueryParam("property") List<String> properties) {
        /* todo: introduce new domain object DomainDocument with the following structure:
            {
                url: '',
                contentType: '',
                compliant: true,
                properties: {
                    requestedProperty1: '',
                    requestedProperty2: '',
                    ...
                },
                errors: [
                    'Error description 1',
                    'Error description 2'
                ]
            }
         */
        return null;
    }

    private void setJobFinished(CrawlJob job) throws IOException {
        crawlJobDao.setJobFinished(job.getId(), true);
        crawlJobDao.setJobUrl(job.getId(), client.getValidPDFReportUri(job.getId()).replace("mirror/Valid_PDF_Report.txt", ""));
        logger.info("Writing finish time for job " + job);
        job.setFinishTime(crawlJobDao.writeFinishTime(job.getId()));
        job.setFinished(true);
        client.teardownJob(job.getId());
        for(String emailAddress: crawlJobDao.getreportEmailsForCrawlJob(job.getId())) {
            if (emailAddress != null && !emailAddress.equals("")) {
                String subject = "Crawl job";
                String text = "Crawl job was finished successfully on domain:\n" + job.getCrawlURL();
                SendEmail.send(emailAddress, subject, text, emailServer);
            }
        }
    }

    private void pauseJob(String job) {
        try {
            client.pauseJob(job);
            crawlJobDao.setStatus(job, "paused");
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " paused");
        }
        catch (Exception e) {
            logger.error("Error pausing job", e);
        }
    }

    private void unpauseJob(String job) {
        try {
            client.unpauseJob(job);
            crawlJobDao.setStatus(job, "running");
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " unpaused");
        }
        catch (Exception e) {
            logger.error("Error unpausing job", e);
        }
    }


}

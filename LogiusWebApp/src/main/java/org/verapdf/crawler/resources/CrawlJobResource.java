package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.IntParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.crawling.CrawlRequest;
import org.verapdf.crawler.api.monitoring.CrawlJobStatus;
import org.verapdf.crawler.api.monitoring.HeritrixCrawlJobStatus;
import org.verapdf.crawler.api.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.core.services.HeritrixCleanerService;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.ValidationJobDAO;
import org.verapdf.crawler.tools.DomainUtils;
import org.xml.sax.SAXException;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

@Path("/crawl-jobs")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlJobResource {

    private static final Logger logger = LoggerFactory.getLogger(CrawlJobResource.class);

    private static final int GET_STATUS_MAX_DOCUMENT_COUNT = 10;

    private final CrawlJobDAO crawlJobDao;
    private final ValidationJobDAO validationJobDAO;
    private final HeritrixClient heritrix;
    private final ValidationService validationService;
    private final HeritrixCleanerService heritrixCleanerService;

    public CrawlJobResource(CrawlJobDAO crawlJobDao, ValidationJobDAO validationJobDAO, HeritrixClient heritrix,
                            ValidationService validationService, HeritrixCleanerService heritrixCleanerService) {
        this.crawlJobDao = crawlJobDao;
        this.validationJobDAO = validationJobDAO;
        this.heritrix = heritrix;
        this.validationService = validationService;
        this.heritrixCleanerService = heritrixCleanerService;
    }

    @GET
    @UnitOfWork
    public Response getJobList(@QueryParam("domainFilter") String domainFilter,
                               @QueryParam("finished") Boolean finished,
                               @QueryParam("start") IntParam startParam,
                               @QueryParam("limit") IntParam limitParam) {
        Integer start = startParam != null ? startParam.get() : null;
        Integer limit = limitParam != null ? limitParam.get() : null;

        long totalCount = crawlJobDao.count(domainFilter, finished);
        List<CrawlJob> crawlJobs = crawlJobDao.find(domainFilter, finished, start, limit);
        return Response.ok(crawlJobs).header("X-Total-Count", totalCount).build();
    }

    @POST
    @Path("/{domain}")
    @UnitOfWork
    public CrawlJob restartCrawlJob(@PathParam("domain") String domain) {
		domain = DomainUtils.trimUrl(domain);

		CrawlJob crawlJob = crawlJobDao.getByDomain(domain);
		CrawlJob.CrawlService service = crawlJob.getCrawlService();
		List<CrawlRequest> crawlRequests = null;
		if (crawlJob != null) {
            String heritrixJobId = crawlJob.getHeritrixJobId();
			// Keep requests list to link to new job
			crawlRequests = new ArrayList<>();
			crawlRequests.addAll(crawlJob.getCrawlRequests());

			// Tear down heritrix
            if (service == CrawlJob.CrawlService.HERITRIX) {
                heritrixCleanerService.teardownAndClearHeritrixJob(heritrixJobId);
            }

			// Remove job from DB
			crawlJobDao.remove(crawlJob);

			// Stop validation job if it's related to this crawl job
			synchronized (ValidationService.class) {
				ValidationJob currentJob = validationService.getCurrentJob();
				if (currentJob != null && currentJob.getDocument().getCrawlJob().getDomain().equals(domain)) {
					validationService.abortCurrentJob();
				}
			}
		}

		// Create and start new crawl job
        CrawlJob newJob = new CrawlJob(domain, service);
		if (crawlRequests != null) {
			newJob.setCrawlRequests(crawlRequests);
		}
        crawlJobDao.save(newJob);
        if (service == CrawlJob.CrawlService.HERITRIX) {
            startCrawlJob(newJob, heritrix);
        }
        return newJob;
    }

    @GET
    @Path("/{domain}")
    @UnitOfWork
    public CrawlJob getCrawlJob(@PathParam("domain") String domain) {
        domain = DomainUtils.trimUrl(domain);

        CrawlJob crawlJob = crawlJobDao.getByDomain(domain);
        if (crawlJob == null) {
            throw new WebApplicationException("Domain " + domain + " not found", Response.Status.NOT_FOUND);
        }
        return crawlJob;
    }

    @PUT
    @Path("/{domain}")
    @UnitOfWork
    public CrawlJob updateCrawlJob(@PathParam("domain") String domain, @NotNull CrawlJob update) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        CrawlJob crawlJob = this.getCrawlJob(domain);

        String heritrixJobId = crawlJob.getHeritrixJobId();
        CrawlJob.CrawlService service = crawlJob.getCrawlService();
        if(crawlJob.getStatus() == CrawlJob.Status.RUNNING && update.getStatus() == CrawlJob.Status.PAUSED) {
            if (service == CrawlJob.CrawlService.HERITRIX && !heritrix.isJobFinished(heritrixJobId)) {
                heritrix.pauseJob(heritrixJobId);
            }
            validationJobDAO.pause(domain);
            crawlJob.setStatus(CrawlJob.Status.PAUSED);
        }
        if(crawlJob.getStatus() == CrawlJob.Status.PAUSED && update.getStatus() == CrawlJob.Status.RUNNING) {
            if (service == CrawlJob.CrawlService.HERITRIX  && !heritrix.isJobFinished(heritrixJobId)) {
                heritrix.unpauseJob(heritrixJobId);
            }
            validationJobDAO.unpause(domain);
            crawlJob.setStatus(CrawlJob.Status.RUNNING);
        }
        return crawlJob;
    }

    @GET
    @Path("/{domain}/requests")
    @UnitOfWork
    public List<CrawlRequest> getCrawlJobRequests(@PathParam("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        return crawlJob.getCrawlRequests();
    }

    @GET
    @Path("/{domain}/status")
    @UnitOfWork
    public CrawlJobStatus getFullJobStatus(@PathParam("domain") String domain) {
        CrawlJob crawlJob = getCrawlJob(domain);
        HeritrixCrawlJobStatus heritrixStatus = null;
        if (crawlJob.getCrawlService() == CrawlJob.CrawlService.HERITRIX) {
            try {
                heritrixStatus = heritrix.getHeritrixStatus(crawlJob.getHeritrixJobId());
            } catch (Throwable e) {
                logger.error("Error during obtaining heritrix status", e);
                heritrixStatus = new HeritrixCrawlJobStatus("Unavailable: " + e.getMessage(), null, null);
            }
        }

        String crawlJobDomain = crawlJob.getDomain();
        Long count = validationJobDAO.count(crawlJobDomain);
        List<ValidationJob> topDocuments = validationJobDAO.getDocuments(crawlJobDomain, GET_STATUS_MAX_DOCUMENT_COUNT);

        return new CrawlJobStatus(crawlJob, heritrixStatus, new ValidationQueueStatus(count, topDocuments));
    }

    @DELETE
    @Path("/{domain}/requests")
    @UnitOfWork
    public List<CrawlRequest> unlinkCrawlRequests(@PathParam("domain") String domain, @QueryParam("email") @NotNull String email) {
        CrawlJob crawlJob = getCrawlJob(domain);

        crawlJob.getCrawlRequests().removeIf(request -> email.equals(request.getEmailAddress()));

//        if (crawlJob.getCrawlRequests().size() == 0) {
//            // todo: clarify if possible/required to terminate CrawlJob if no associated CrawlRequests left
//        }

        return crawlJob.getCrawlRequests();
    }
//
//    @GET
//    @Path("/{domain}/documents")
//    public List<Object> getDomainDocuments(@PathParam("domain") String domain,
//                                           @QueryParam("startDate") DateParam startDate,
//                                           @QueryParam("type") String type,
//                                           @QueryParam("start") IntParam start,
//                                           @QueryParam("limit") IntParam limit,
//                                           @QueryParam("property") List<String> properties) {
//        /* todo: introduce new domain object DomainDocument with the following structure:
//            {
//                url: '',
//                contentType: '',
//                compliant: true,
//                properties: {
//                    requestedProperty1: '',
//                    requestedProperty2: '',
//                    ...
//                },
//                errors: [
//                    'Error description 1',
//                    'Error description 2'
//                ]
//            }
//         */
//        return null;
//    }

    static void startCrawlJob(CrawlJob crawlJob, HeritrixClient heritrix) {
        try {
            String heritrixJobId = crawlJob.getHeritrixJobId();
            heritrix.createJob(heritrixJobId, crawlJob.getDomain());
            heritrix.buildJob(heritrixJobId);
            heritrix.launchJob(heritrixJobId);
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

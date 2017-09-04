package org.verapdf.crawler.app.resources;

import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/crawl-jobs")
public class CrawlJobResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CurrentJob> getJobList(@QueryParam("domainFilter") String domainFilter,
                                       @QueryParam("start") Integer start,
                                       @QueryParam("limit") Integer limit) {
        // todo: rename CurrentJob to CrawlJob
        // todo: return list of CrawlJobs where domain contains domainFilter if specified
        // todo: if start/limit specified limit the number of records accordingly
        // todo: specify total count of matched jobs using X-Total-Count response header
        return null;
    }

    @POST
    @Path("/{domain}")
    public CurrentJob restartCrawlJob(@PathParam("domain") String domain) {
        return null;
    }

    @GET
    @Path("/{domain}")
    public CurrentJob getCrawlJob(@PathParam("domain") String domain) {
        return null;
    }

    @PUT
    @Path("/{domain}")
    public CurrentJob updateCrawlJob(@PathParam("domain") String domain, CurrentJob update) {
        // todo: apply updates to the job, e.g. change status
        return null;
    }

    @GET
    @Path("/{domain}/requests")
    public List<BatchJob> getCrawlJobRequests(@PathParam("domain") String domain) {
        // todo: return the list of CrawlRequest linked to the job
        return null;
    }

    @DELETE
    @Path("/{domain}/requests")
    public List<BatchJob> unlinkCrawlRequests(@PathParam("domain") String domain, @QueryParam("email") String email) {
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

}

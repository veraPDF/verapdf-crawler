package org.verapdf.crawler.app.resources;

import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.validation.ValidationService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/info")
public class InfoResourse {

    private final ValidationService validationService;
    private final CrawlRequestDao crawlRequestDao;

    InfoResourse(ValidationService validationService, CrawlRequestDao crawlRequestDao) {
        this.validationService = validationService;
        this.crawlRequestDao = crawlRequestDao;
    }

    @GET
    @Path("/{job}/email_address")
    public String getReportEmail(@PathParam("job") String job) { return crawlRequestDao.getReportEmail(job); }

    @GET
    @Path("/queue")
    @Produces(MediaType.TEXT_PLAIN)
    public String getQueueSize() {
        return validationService.getQueueSize().toString();
    }
}

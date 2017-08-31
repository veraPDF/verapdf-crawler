package org.verapdf.crawler.app.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.repository.jobs.BatchJobDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.ValidationService;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("/info")
public class InfoResourse {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final ValidationService validationService;
    private final BatchJobDao batchJobDao;

    InfoResourse(ValidationService validationService, BatchJobDao batchJobDao) {
        this.validationService = validationService;
        this.batchJobDao = batchJobDao;
    }

    @GET
    @Timed
    @Path("/{job}/email_address")
    public String getReportEmail(@PathParam("job") String job) { return batchJobDao.getReportEmail(job); }

    @GET
    @Timed
    @Path("/list")
    public List<BatchJob> getJobs() {
        return batchJobDao.getBatchJobs();
    }

    @GET
    @Timed
    @Path("/queue")
    @Produces(MediaType.TEXT_PLAIN)
    public String getQueueSize() {
        return validationService.getQueueSize().toString();
    }
}

package org.verapdf.crawler.app.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.validation.ValidationService;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;

@Produces(MediaType.APPLICATION_JSON)
@Path("/info")
public class InfoResourse {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final ArrayList<CurrentJob> currentJobs;
    private final ValidationService validationService;
    private final HeritrixClient client;
    private final ResourceManager resourceManager;

    public InfoResourse(ValidationService validationService, HeritrixClient client, ArrayList<CurrentJob> currentJobs, ResourceManager resourceManager) {
        this.validationService = validationService;
        this.client = client;
        this.currentJobs = currentJobs;
        this.resourceManager = resourceManager;
    }

    @GET
    @Timed
    @Path("/{job}/email_address")
    public String getReportEmail(@PathParam("job") String job) {
        return getJobById(job).getReportEmail();
    }

    @GET
    @Timed
    @Path("/list")
    public ArrayList<CurrentJob> getJobs() {
        try {
            refreshCurrentJobs();
        }
        catch (Exception e) {
            logger.error("Error on refreshing job status", e);
        }
        return currentJobs;
    }

    @GET
    @Timed
    @Path("/queue")
    @Produces(MediaType.TEXT_PLAIN)
    public String getQueueSize() {
        return validationService.getQueueSize().toString();
    }

    private CurrentJob getJobById(String job) {
        return resourceManager.getJobById(job);
    }

    private void refreshCurrentJobs() throws SAXException, ParserConfigurationException, IOException {
        for(CurrentJob job : currentJobs) {
            if(job.isActiveJob()) {
                job.setStatus(client.getCurrentJobStatus(job.getId()));
            }
            else {
                job.setStatus("finished");
            }
        }
    }
}

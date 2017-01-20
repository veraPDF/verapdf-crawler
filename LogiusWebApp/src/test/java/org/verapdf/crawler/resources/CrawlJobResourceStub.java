package org.verapdf.crawler.resources;

import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.api.CurrentJob;
import org.verapdf.crawler.api.StartJobData;
import org.verapdf.crawler.api.EmailServer;
import org.verapdf.crawler.api.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class CrawlJobResourceStub extends CrawlJobResource {
    private int numberOfCrawledUrls;

    public CrawlJobResourceStub(HeritrixClient client, EmailServer emailServer) throws IOException {
        super(client, emailServer);
        currentJobs.clear();
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public SingleURLJobReport startJob(StartJobData startJobData) {
        numberOfCrawledUrls = 0;
        ArrayList<String> list = new ArrayList<>();
        list.add(startJobData.getDomain());

        String job = UUID.randomUUID().toString();
        String jobStatus = "Active: PREPARING";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if(startJobData.getDate() != null)
            currentJobs.add(new CurrentJob(job, "", startJobData.getDomain(), LocalDateTime.parse(startJobData.getDate(), formatter)));
        else
            currentJobs.add(new CurrentJob(job, "", startJobData.getDomain(), null));

        return new SingleURLJobReport(job, startJobData.getDomain(), jobStatus, 0);
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) {
        String jobStatus = "";
        String domain = "";
        String reportUrl = null;
        if(!getCrawlUrlById(job).equals("") && numberOfCrawledUrls < 6)
        {
            jobStatus = "Active: RUNNING";
            domain = getCrawlUrlById(job);
            numberOfCrawledUrls++;
        }
        else if(numberOfCrawledUrls == 6) {
            reportUrl = "";
        }
        return new SingleURLJobReport(job, domain, jobStatus, numberOfCrawledUrls);
    }

    private String getCrawlUrlById(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData.getCrawlURL();
        }
        return "";
    }
}

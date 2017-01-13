package org.verapdf.crawler.resources;

import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.api.Domain;
import org.verapdf.crawler.api.EmailServer;
import org.verapdf.crawler.api.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.UUID;

public class CrawlJobResourceStub extends CrawlJobResource {
    private int numberOfCrawledUrls;

    public CrawlJobResourceStub(HeritrixClient client, EmailServer emailServer) {
        super(client, emailServer);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Override
    public SingleURLJobReport startJob(Domain domain) {
        numberOfCrawledUrls = 0;
        ArrayList<String> list = new ArrayList<>();
        list.add(domain.getDomain());

        String job = UUID.randomUUID().toString();
        String jobStatus = "Active: PREPARING";
        currentJobs.put(job, domain.getDomain());

        return new SingleURLJobReport(job, domain.getDomain(), jobStatus, 0);
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) {
        String jobStatus = "";
        String domain = "";
        String reportUrl = null;
        if(currentJobs.containsKey(job) && numberOfCrawledUrls < 6)
        {
            jobStatus = "Active: RUNNING";
            domain = currentJobs.get(job);
            numberOfCrawledUrls++;
        }
        else if(numberOfCrawledUrls == 6) {
            reportUrl = "";
        }
        return new SingleURLJobReport(job, domain, jobStatus, numberOfCrawledUrls);
    }
}

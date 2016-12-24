package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.Domain;
import org.verapdf.crawler.api.EmailAddress;
import org.verapdf.crawler.api.EmailServer;
import org.verapdf.crawler.api.JobSingleUrl;
import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.helpers.emailUtils.SendEmail;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    protected HeritrixClient client;
    protected HashMap<String, String> currentJobs;
    protected String reportToEmail;
    protected EmailServer emailServer;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer)
    {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new HashMap<>();
    }

    public String getreportEmail() {
        return reportToEmail;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public JobSingleUrl startJob(Domain domain) {
        ArrayList<String> list = new ArrayList<>();
        list.add(domain.getDomain());
        String jobStatus = "";

        String job = UUID.randomUUID().toString();
        try {
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            jobStatus = client.getCurrentJobStatus(job);
            currentJobs.put(job, domain.getDomain());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return new JobSingleUrl(job, domain.getDomain(), jobStatus, 0, null);
    }

    @POST
    @Timed
    @Path("/target_email")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setReportEmail(EmailAddress email) {
        reportToEmail = email.getEmailAddress();
    }

    @GET
    @Timed
    @Path("/get_target_email")
    public EmailAddress setReportEmail() {
        return new EmailAddress(reportToEmail);
    }

    @GET
    @Timed
    @Path("/list")
    public HashMap<String, String> getJobs() {
        return currentJobs;
    }

    @GET
    @Timed
    @Path("/{job}")
    public JobSingleUrl getJob(@PathParam("job") String job) {
        String jobStatus = "";
        String domain = "";
        String reportUrl = null;
        int numberOfCrawledUrls = 0;
        try {
            jobStatus = client.getCurrentJobStatus(job);
            domain = client.getListOfCrawlUrls(job).get(0);
            numberOfCrawledUrls = client.getDownloadedCount(job);
            if(client.isJobFinished(job)) {
                if(reportToEmail != null) {
                    SendEmail.send(reportToEmail,
                            "Crawl report",
                            "The crawl job on " + domain + " is finished.",
                            emailServer);
                }
                reportUrl = client.getCrawlLogUri(job);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return new JobSingleUrl(job, domain, jobStatus, numberOfCrawledUrls
                , reportUrl);
    }
}

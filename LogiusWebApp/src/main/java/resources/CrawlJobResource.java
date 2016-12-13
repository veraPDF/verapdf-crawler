package resources;

import api.Domain;
import api.JobSingleUrl;
import com.codahale.metrics.annotation.Timed;
import engine.HeritrixClient;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static javax.swing.text.html.FormSubmitEvent.MethodType.POST;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    private HeritrixClient client;
    private HashMap<String, String> currentJobs;

    public CrawlJobResource(HeritrixClient client)
    {
        this.client = client;
        currentJobs = new HashMap<>();
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

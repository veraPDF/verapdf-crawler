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
import java.util.UUID;

import static javax.swing.text.html.FormSubmitEvent.MethodType.POST;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    private HeritrixClient client;

    public CrawlJobResource(HeritrixClient client) {
        this.client = client;
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

        return new JobSingleUrl(job, domain.getDomain(), jobStatus, null);
    }

    @GET
    @Timed
    @Path("/{job}")
    public JobSingleUrl getJob(@PathParam("job") String job) {
        String jobStatus = "";
        String domain = "";
        String reportUrl = null;
        try {
            jobStatus = client.getCurrentJobStatus(job);
            domain = client.getListOfCrawlUrls(job).get(0);
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

        return new JobSingleUrl(job, domain, jobStatus, reportUrl);
    }
}

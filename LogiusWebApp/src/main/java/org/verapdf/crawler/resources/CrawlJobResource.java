package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.*;
import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    private HeritrixClient client;
    protected HashMap<String, String> currentJobs;
    private ArrayList<InactiveJob> inactiveJobs;
    private String reportToEmail;
    private EmailServer emailServer;
    private HeritrixReporter reporter;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer)
    {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new HashMap<>();
        inactiveJobs = new ArrayList<>();
        reporter = new HeritrixReporter(client);
    }

    public String getreportEmail() {
        return reportToEmail;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(Domain domain) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        String jobURL = getExistingJobURLbyCrawlURL(domain.getDomain());
        String job = UUID.randomUUID().toString();
        if(jobURL.equals("")) { // Brand new URL
            ArrayList<String> list = new ArrayList<>();
            list.add(domain.getDomain());

            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            String jobStatus = client.getCurrentJobStatus(job);
            currentJobs.put(job, domain.getDomain());

            jobURL = client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt","");
            FileWriter writer = new FileWriter("src/main/resources/crawled_urls.txt", true);
            writer.write(domain.getDomain() + " " + jobURL);
            writer.write(System.lineSeparator());
            writer.close();

            return new SingleURLJobReport(job, domain.getDomain(), jobStatus, 0);
        }
        else { // This URL has already been crawled
            inactiveJobs.add(new InactiveJob(job, jobURL, domain.getDomain()));
            return new SingleURLJobReport(job, domain.getDomain(), "Finished", 0);
        }
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
        HashMap<String, String> result = new HashMap<>();
        result.putAll(currentJobs);
        for(InactiveJob job : inactiveJobs) {
            result.put(job.getId(), job.getCrawlURL());
        }
        return result;
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(!jobURL.equals("")){
            return reporter.getReport(job, jobURL);
        }
        else {
            return reporter.getReport(job);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        File file;
        if(!jobURL.equals("")){
            file = reporter.buildODSReport(job, jobURL);
        }
        else {
            file = reporter.buildODSReport(job);
        }
        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" )
                .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/html_report/{job}")
    public String getHtmlReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(!jobURL.equals("")){
            return reporter.buildHtmlReport(job, jobURL);
        }
        else {
            return reporter.buildHtmlReport(job);
        }
    }

    // Return empty string if not found
    private String getExistingJobURLbyCrawlURL(String URL) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("src/main/resources/crawled_urls.txt"));
        while(scanner.hasNext()) {
            if(URL.equals(scanner.next())) {
                return scanner.next();
            }
            scanner.next();
        }
        return "";
    }

    // Return empty string if not found
    private String getExistingJobURLbyJobId(String jobId) {
        for(InactiveJob job : inactiveJobs) {
            if(job.getId().equals(jobId)) {
                return job.getJobURL();
            }
        }
        return "";
    }
}

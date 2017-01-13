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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    private HeritrixClient client;
    protected HashMap<String, String> currentJobs;
    private String reportToEmail;
    private EmailServer emailServer;
    private HeritrixReporter reporter;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer)
    {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new HashMap<>();
        reporter = new HeritrixReporter(client);
    }

    public String getreportEmail() {
        return reportToEmail;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(Domain domain) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        ArrayList<String> list = new ArrayList<>();
        list.add(domain.getDomain());

        String job = UUID.randomUUID().toString();
        client.createJob(job, list);
        client.buildJob(job);
        client.launchJob(job);
        String  jobStatus = client.getCurrentJobStatus(job);
        currentJobs.put(job, domain.getDomain());

        return new SingleURLJobReport(job, domain.getDomain(), jobStatus, 0);
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
    public SingleURLJobReport getJob(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        return reporter.getReport(job);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        File file;
        file = reporter.buildODSReport(job);
        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" ) //optional
                .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/html_report/{job}")
    public String getHtmlReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        return reporter.buildHtmlReport(job);
    }
}

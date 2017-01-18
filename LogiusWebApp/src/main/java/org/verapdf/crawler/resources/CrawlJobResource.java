package org.verapdf.crawler.resources;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.verapdf.crawler.api.*;
import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    @Context
    private UriInfo uriInfo;

    private HeritrixClient client;
    protected ArrayList<CurrentJob> currentJobs;
    private String reportToEmail;
    private EmailServer emailServer;
    private HeritrixReporter reporter;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer) throws IOException {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new ArrayList<>();
        reporter = new HeritrixReporter(client);
        loadJobs();
    }

    public String getReportToEmail() {
        return reportToEmail;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(Domain domain) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {

        if(!isCurrentJob(domain.getDomain())) { // Brand new URL
            ArrayList<String> list = new ArrayList<>();
            list.add(domain.getDomain());

            String job = UUID.randomUUID().toString();
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            String jobStatus = client.getCurrentJobStatus(job);
            currentJobs.add(new CurrentJob(job, "", domain.getDomain(), true));

            String jobURL = client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt","");
            FileWriter writer = new FileWriter(client.baseDirectory + "/src/main/resources/crawled_urls.txt", true);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            printer.printRecord(new String[] {job, domain.getDomain(), jobURL});
            writer.close();

            return new SingleURLJobReport(job, domain.getDomain(), jobStatus, 0);
        }
        else { // This URL has already been crawled
            System.out.println("Was crawled");
            return new SingleURLJobReport("", "", "", 0);
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
        for(CurrentJob job : currentJobs) {
            result.put(job.getId(), job.getCrawlURL());
        }
        return result;
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(jobURL.equals("")){
            return reporter.getReport(job);
        }
        else {
            return reporter.getReport(job, jobURL);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        File file;
        if(jobURL.equals("")){
            file = reporter.buildODSReport(job);
        }
        else {
            file = reporter.buildODSReport(job, jobURL);
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
        String htmlReport;
        if(jobURL.equals("")){
            htmlReport = reporter.buildHtmlReport(job);
        }
        else {
            htmlReport = reporter.buildHtmlReport(job, jobURL);
        }
        htmlReport = htmlReport.replace("INVALID_PDF_REPORT", uriInfo.getBaseUri().toString() + "invalid_pdf_list/" + job);
        htmlReport = htmlReport.replace("OFFICE_REPORT", uriInfo.getBaseUri().toString() + "office_list/" + job);
        return htmlReport;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("office_list/{job}")
    public String getOfficeReport(@PathParam("job") String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(jobURL.equals("")) {
            return reporter.getOfficeReport(job);
        }
        else{
            return reporter.getOfficeReport(job, jobURL);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("invalid_pdf_list/{job}")
    public String getInvalidPdfReport(@PathParam("job") String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(jobURL.equals("")) {
            return reporter.getInvalidPDFReport(job);
        }
        else{
            return reporter.getInvalidPDFReport(job, jobURL);
        }
    }

    private String getExistingJobURLbyJobId(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData.getJobURL();
        }
        return "";
    }

    private boolean isCurrentJob(String crawlUrl) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getCrawlURL().equals(crawlUrl))
                return true;
        }
        return false;
    }

    private void loadJobs() throws IOException {
        FileReader reader = new FileReader(client.baseDirectory + "/src/main/resources/crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        for(CSVRecord record : records) {
            currentJobs.add(new CurrentJob(record.get("id"),
                    record.get("jobURL"),
                    record.get("crawlURL"),
                    false));
        }
    }
}

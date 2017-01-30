package org.verapdf.crawler.resources;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.verapdf.crawler.api.*;
import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.helpers.emailUtils.SendEmail;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    @Context
    private UriInfo uriInfo;

    private HeritrixClient client;
    protected ArrayList<CurrentJob> currentJobs;
    private EmailServer emailServer;
    private HeritrixReporter reporter;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer) throws IOException {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new ArrayList<>();
        reporter = new HeritrixReporter(client);
        loadJobs();
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(StartJobData startJobData) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        if(!isCurrentJob(startJobData.getDomain())) { // Brand new URL
            ArrayList<String> list = new ArrayList<>();
            list.add(startJobData.getDomain());

            String job = UUID.randomUUID().toString();
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            String jobStatus = client.getCurrentJobStatus(job);
            if(startJobData.getDate() == null || startJobData.getDate().isEmpty()) {
                currentJobs.add(new CurrentJob(job, "", startJobData.getDomain(),
                        null, startJobData.getReportEmail()));
            }
            else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                currentJobs.add(new CurrentJob(job, "", startJobData.getDomain(),
                        LocalDateTime.of(LocalDate.parse(startJobData.getDate(), formatter), LocalTime.MIN),
                        startJobData.getReportEmail()));
            }

            String jobURL = client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt","");
            FileWriter writer = new FileWriter(client.baseDirectory + "crawled_urls.txt", true);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            printer.printRecord(new String[] {job, startJobData.getDomain(), jobURL});
            writer.close();

            return new SingleURLJobReport(job, startJobData.getDomain(), jobStatus, 0);
        }
        else { // This URL has already been crawled
            if( startJobData.getDate() != null && !startJobData.getDate().isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                getJobByCrawlUrl(startJobData.getDomain()).setCrawlSinceTime(
                        LocalDateTime.of(LocalDate.parse(startJobData.getDate(), formatter), LocalTime.MIN));
            }
            return new SingleURLJobReport("", "", "", 0);
        }
    }

    @POST
    @Timed
    @Path("/email")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setReportEmail(EmailAddress address) {
        getJobById(address.getJob()).setReportEmail(address.getEmailAddress());
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
        SingleURLJobReport result;
        if(jobURL.equals("")){
            result = reporter.getReport(job, getTimeByJobId(job));
        }
        else {
            result = reporter.getReport(job, jobURL, getTimeByJobId(job));
        }
        if(result.getStatus().startsWith("Finished")) {
            CurrentJob jobData = getJobById(job);
            if(!jobData.getReportEmail().equals("") && jobData.isEmailSent() != true) {
                String subject = "Crawl job";
                String text = "Crawl job on " + jobData.getCrawlURL() + " was finished with status " + result.getStatus();
                SendEmail.send(jobData.getReportEmail(), subject, text, emailServer);
                jobData.setEmailSent(true);
            }
        }

        return result;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String jobURL = getExistingJobURLbyJobId(job);
        File file;
        if(jobURL.equals("")){
            file = reporter.buildODSReport(job, getTimeByJobId(job));
        }
        else {
            file = reporter.buildODSReport(job, jobURL, getTimeByJobId(job));
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
            htmlReport = reporter.buildHtmlReport(job, getTimeByJobId(job));
        }
        else {
            htmlReport = reporter.buildHtmlReport(job, jobURL, getTimeByJobId(job));
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
            return reporter.getOfficeReport(job, getTimeByJobId(job));
        }
        else{
            return reporter.getOfficeReport(job, jobURL, getTimeByJobId(job));
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("invalid_pdf_list/{job}")
    public String getInvalidPdfReport(@PathParam("job") String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String jobURL = getExistingJobURLbyJobId(job);
        if(jobURL.equals("")) {
            return reporter.getInvalidPDFReport(job, getTimeByJobId(job));
        }
        else{
            return reporter.getInvalidPDFReport(job, jobURL, getTimeByJobId(job));
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
        FileReader reader = new FileReader(client.baseDirectory + "crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        for(CSVRecord record : records) {
            currentJobs.add(new CurrentJob(record.get("id"),
                    record.get("jobURL"),
                    record.get("crawlURL"),
                    null, ""));
        }
    }

    private CurrentJob getJobByCrawlUrl(String crawlUrl) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getCrawlURL().equals(crawlUrl))
                return jobData;
        }
        return null;
    }

    private CurrentJob getJobById(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData;
        }
        return null;
    }

    private LocalDateTime getTimeByJobId(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData.getCrawlSinceTime();
        }
        return null;
    }
}

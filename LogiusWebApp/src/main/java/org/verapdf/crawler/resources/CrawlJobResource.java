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
import org.verapdf.crawler.validation.ValidationLauncher;
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
    private EmailServer emailServer;
    private HeritrixReporter reporter;
    protected ArrayList<CurrentJob> currentJobs;
    private ArrayList<BatchJob> batchJobs;
    private String resourceUri;
    private ValidationLauncher launcher;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer, String verapdfPath, String heritrixPath) throws IOException {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new ArrayList<>();
        batchJobs = new ArrayList<>();
        reporter = new HeritrixReporter(client);
        loadJobs();
        launcher = new ValidationLauncher(heritrixPath + "validation/validation-jobs.txt", verapdfPath);
        new Thread(new StatusMonitor(this)).start();
        new Thread(launcher).start();
    }

    public ArrayList<CurrentJob> getCurrentJobs() {
        return currentJobs;
    }

    public ArrayList<BatchJob> getBatchJobs() {
        return batchJobs;
    }

    public EmailServer getEmailServer() { return emailServer; }

    public String getResourceUri() { return resourceUri; }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(StartJobData startJobData) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        if(resourceUri == null && uriInfo != null) {
            resourceUri = uriInfo.getBaseUri().toString();
        }
        if(isCurrentJob(trimUrl(startJobData.getDomain())) && !startJobData.isForceStart()) { // This URL has already been crawled and job is not forced to overwrite
            return new SingleURLJobReport(getJobByCrawlUrl(trimUrl(startJobData.getDomain())).getId(), "", "", 0);
        }
        else {
            if(startJobData.isForceStart() && isCurrentJob(trimUrl(startJobData.getDomain()))) { // This URL has already been crawled but the old job needs to be overwritten
                currentJobs.remove(getJobByCrawlUrl(trimUrl(startJobData.getDomain())));
                removeJobFromFile(trimUrl(startJobData.getDomain()));
            }
            // Brand new URL
            ArrayList<String> list = new ArrayList<>();
            if(startJobData.getDomain().startsWith("http://") || startJobData.getDomain().startsWith("https://")) {
                list.add(trimUrl(startJobData.getDomain()));
            }
            else {
                list.add(trimUrl(startJobData.getDomain()));
                list.add(list.get(0).replace("https://", "http://"));
            }

            String job = UUID.randomUUID().toString();
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            String jobStatus = client.getCurrentJobStatus(job);
            if(startJobData.getDate() == null || startJobData.getDate().isEmpty()) {
                currentJobs.add(new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                        null, startJobData.getReportEmail()));
            }
            else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                currentJobs.add(new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                        LocalDateTime.of(LocalDate.parse(startJobData.getDate(), formatter), LocalTime.MIN),
                        startJobData.getReportEmail()));
            }
            String jobURL ="";
            String reportUri = client.getValidPDFReportUri(job);
            if(reportUri.contains("mirror/Valid_PDF_Report.txt")) {
                jobURL = reportUri.replace("mirror/Valid_PDF_Report.txt", "");
            }
            FileWriter writer = new FileWriter(HeritrixClient.baseDirectory + "crawled_urls.txt", true);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            printer.printRecord(new String[] {job, trimUrl(startJobData.getDomain()), jobURL});
            writer.close();

            return new SingleURLJobReport(job, trimUrl(startJobData.getDomain()), jobStatus, 0);
        }
    }

    @POST
    @Timed
    @Path("/batch")
    @Produces("text/plain")
    @Consumes(MediaType.APPLICATION_JSON)
    public String startBatchJob(StartBatchJobData jobData) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        String id = UUID.randomUUID().toString();
        BatchJob batch = new BatchJob(id, jobData.getReportEmail());
        for(String domain : jobData.getDomains()) {
            StartJobData data = new StartJobData(domain, jobData.getDate());
            data.setReportEmail(jobData.getReportEmail());
            data.setForceStart(false);
            startJob(data);
            batch.getDomains().add(trimUrl(domain));
        }

        batchJobs.add(batch);
        String reportUrl = uriInfo.getBaseUri().toString() + "batch/" + id;
        String response = "Batch job successfully submitted. You can track it on " + reportUrl +
                ". Notification will be sent on the email address you provided when the job is finished.";
        return response;
    }

    @GET
    @Timed
    @Produces(MediaType.TEXT_HTML)
    @Path("/batch/{job}")
    public String getBatchJob(@PathParam("job") String job) throws IOException, SAXException, NoSuchAlgorithmException, ParserConfigurationException, KeyManagementException {
        StringBuilder responseHtml = new StringBuilder();
        StringBuilder jobList = new StringBuilder();
        BatchJob batchJob = getBatchJobById(job);
        boolean isBatchFinished = true;
        for(String domain : batchJob.getDomains()) {
            try {
                jobList.append("<li>");
                jobList.append("Job on " + domain + ", ");
                SingleURLJobReport report = getJob(getJobByCrawlUrl(domain).getId());
                jobList.append(report.getStatus());
                isBatchFinished = isBatchFinished && report.getStatus().startsWith("Finished");
                jobList.append(", <a href=\"");
                jobList.append(resourceUri.replace("crawl-job/", "jobinfo?id=") + getJobByCrawlUrl(domain).getId());
                jobList.append("\">details</a>.");
                jobList.append("</li>");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(isBatchFinished) {
            batchJob.setFinished(true);
        }
        responseHtml.append("<html>");
        responseHtml.append("<p>Batch job is ");
        if(batchJob.isFinished()) {
            responseHtml.append("finished.</p>");
        }
        else {
            responseHtml.append(" running.</p>");
        }
        responseHtml.append("<ul>");
        responseHtml.append(jobList.toString());
        responseHtml.append("</ul>");
        responseHtml.append("</html>");
        return responseHtml.toString();
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
            if(!jobData.getReportEmail().equals("") && !jobData.isEmailSent()) {
                String subject = "Crawl job";
                String text = "Crawl job on " + jobData.getCrawlURL() + " was finished with status " + result.getStatus() +
                        "\nResults are available at " + resourceUri.replace("crawl-job/","jobinfo?id=") + job;
                SendEmail.send(jobData.getReportEmail(), subject, text, emailServer);
                jobData.setEmailSent(true);
                client.teardownJob(jobData.getId());
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
        htmlReport = htmlReport.replace("INVALID_PDF_REPORT", resourceUri + "invalid_pdf_list/" + job);
        htmlReport = htmlReport.replace("OFFICE_REPORT", resourceUri + "office_list/" + job);
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

    private BatchJob getBatchJobById(String id) {
        for(BatchJob job : batchJobs) {
            if(job.getId().equals(id)) {
                return job;
            }
        }
        return null;
    }

    private boolean isCurrentJob(String crawlUrl) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getCrawlURL().equals(crawlUrl))
                return true;
        }
        return false;
    }

    private void loadJobs() throws IOException {
        FileReader reader = new FileReader(HeritrixClient.baseDirectory + "crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        for(CSVRecord record : records) {
            currentJobs.add(new CurrentJob(record.get("id"),
                    record.get("jobURL"),
                    record.get("crawlURL"),
                    null, ""));
        }
    }

    public CurrentJob getJobByCrawlUrl(String crawlUrl) {
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

    private void removeJobFromFile(String crawlUrl) throws IOException {
        StringBuilder builder = new StringBuilder();
        FileReader reader = new FileReader(HeritrixClient.baseDirectory + "crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        builder.append("id,crawlURL,jobURL" + System.lineSeparator());
        for(CSVRecord record : records) {
            if(!record.get("crawlURL").equals(crawlUrl)) {
                builder.append(record.get("id") + ",");
                builder.append(record.get("crawlURL") + ",");
                builder.append(record.get("jobURL") + System.lineSeparator());
            }
        }
        reader.close();
        FileWriter writer = new FileWriter(HeritrixClient.baseDirectory + "crawled_urls.txt");
        writer.write(builder.toString());
        writer.close();
    }

    private String trimUrl(String url) {
        if(!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if(url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if(url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
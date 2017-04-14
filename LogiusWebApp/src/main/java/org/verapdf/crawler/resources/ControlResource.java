package org.verapdf.crawler.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.crawling.StartBatchJobData;
import org.verapdf.crawler.domain.crawling.StartJobData;
import org.verapdf.crawler.domain.email.EmailAddress;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.report.SingleURLJobReport;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.emailUtils.SendEmail;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.validation.ValidationService;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ControlResource {

    @Context
    private UriInfo uriInfo;

    private ArrayList<CurrentJob> currentJobs;
    private HeritrixClient client;
    private HeritrixReporter reporter;
    private EmailServer emailServer;
    private ArrayList<BatchJob> batchJobs;
    private ValidationService service;
    private ResourceManager resourceManager;

    public ControlResource(ArrayList<CurrentJob> currentJobs, HeritrixClient client,
                           HeritrixReporter reporter, EmailServer emailServer, ArrayList<BatchJob> batchJobs,
                           ValidationService service, ResourceManager resourceManager) {
        this.currentJobs = currentJobs;
        this.client = client;
        this.reporter = reporter;
        this.emailServer = emailServer;
        this.batchJobs = batchJobs;
        this.service = service;
        this.resourceManager = resourceManager;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(StartJobData startJobData) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        if(resourceManager.getResourceUri()  == null && uriInfo != null) {
            resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
        }
        if(isCurrentJob(trimUrl(startJobData.getDomain())) && !startJobData.isForceStart()) { // This URL has already been crawled and job is not forced to overwrite
            return new SingleURLJobReport(getJobByCrawlUrl(trimUrl(startJobData.getDomain())).getId(), "", "", 0);
        }
        else {
            if(startJobData.isForceStart() && isCurrentJob(trimUrl(startJobData.getDomain()))) { // This URL has already been crawled but the old job needs to be overwritten
                client.teardownJob(getJobByCrawlUrl(trimUrl(startJobData.getDomain())).getId());
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
            LocalDateTime now = LocalDateTime.now();
            if(startJobData.getDate() == null || startJobData.getDate().isEmpty()) {
                currentJobs.add(new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                        null, startJobData.getReportEmail(), now));
            }
            else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                currentJobs.add(new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                        LocalDateTime.of(LocalDate.parse(startJobData.getDate(), formatter), LocalTime.MIN),
                        startJobData.getReportEmail(), now));
            }
            String jobURL ="";
            String reportUri = client.getValidPDFReportUri(job);
            if(reportUri.contains("mirror/Valid_PDF_Report.txt")) {
                jobURL = reportUri.replace("mirror/Valid_PDF_Report.txt", "");
            }
            FileWriter writer = new FileWriter(HeritrixClient.baseDirectory + "crawled_urls.txt", true);
            CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
            printer.printRecord(new String[] {job, trimUrl(startJobData.getDomain()), jobURL, startJobData.getDate(),
                    now.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")), ""});
            writer.close();

            return new SingleURLJobReport(job, trimUrl(startJobData.getDomain()), jobStatus, 0);
        }
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        if(resourceManager.getResourceUri()  == null && uriInfo != null) {
            resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        String jobURL = getExistingJobURLbyJobId(job);
        SingleURLJobReport result;
        if(jobURL.equals("")){
            result = reporter.getReport(job, getTimeByJobId(job));
        }
        else {
            result = reporter.getReport(job, jobURL, getTimeByJobId(job));
        }
        CurrentJob jobData = getJobById(job);
        if(result.getStatus().startsWith("Finished")) {
            if(!jobData.getReportEmail().equals("") && !jobData.isEmailSent()) {
                String subject = "Crawl job";
                String text = "Crawl job on " + jobData.getCrawlURL() + " was finished with status " + result.getStatus() +
                        "\nResults are available at " + resourceManager.getResourceUri().replace("api/","jobinfo?id=") + job;
                SendEmail.send(jobData.getReportEmail(), subject, text, emailServer);
                jobData.setEmailSent(true);
            }
            if(jobData.getJobURL().equals("")) {
                jobData.setJobURL(client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt", ""));
                jobData.setFinishTime(LocalDateTime.now());
                writeFinishDate(job);
            }
            client.teardownJob(jobData.getId());
        }
        result.startTime = jobData.getStartTime().format(formatter) + " GMT";
        if(jobData.getFinishTime() != null) {
            result.finishTime = jobData.getFinishTime().format(formatter) + " GMT";
        }
        else {
            result.finishTime = "";
        }
        return result;
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
        return "Batch job successfully submitted. You can track it on " + reportUrl +
                ". Notification will be sent on the email address you provided when the job is finished.";
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
                jobList.append(resourceManager.getResourceUri().replace("api/", "jobinfo?id=") + getJobByCrawlUrl(domain).getId());
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

    @POST
    @Timed
    @Path("/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addValidationJob(ValidationJobData data) throws IOException {
        String[] parts = data.getJobDirectory().split("/");
        data.errorOccurances = getJobById(parts[parts.length - 3]).getErrorOccurances();
        service.addJob(data);
    }

    private boolean isCurrentJob(String crawlUrl) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getCrawlURL().equals(crawlUrl))
                return true;
        }
        return false;
    }

    private void removeJobFromFile(String crawlUrl) throws IOException {
        StringBuilder builder = new StringBuilder();
        FileReader reader = new FileReader(HeritrixClient.baseDirectory + "crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        builder.append("id,crawlURL,jobURL,crawlSince,startTime,finishTime" + System.lineSeparator());
        for(CSVRecord record : records) {
            if(!record.get("crawlURL").equals(crawlUrl)) {
                builder.append(record.get("id") + ",");
                builder.append(record.get("crawlURL") + ",");
                builder.append(record.get("jobURL") + ",");
                builder.append(record.get("crawlSince") + ",");
                builder.append(record.get("startTime") + ",");
                builder.append(record.get("finishTime") + System.lineSeparator());
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

    public CurrentJob getJobByCrawlUrl(String crawlUrl) { return resourceManager.getJobByCrawlUrl(crawlUrl); }

    private String getExistingJobURLbyJobId(String job) { return resourceManager.getExistingJobURLbyJobId(job); }

    private LocalDateTime getTimeByJobId(String job) { return resourceManager.getTimeByJobId(job); }

    public CurrentJob getJobById(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData;
        }
        return null;
    }

    private void writeFinishDate(String job) throws IOException {
        Scanner sc = new Scanner(new File(HeritrixClient.baseDirectory + "crawled_urls.txt"));
        StringBuilder builder = new StringBuilder();
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.startsWith(job) && line.endsWith(",")) {
                line += LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
            }
            builder.append(line);
            builder.append(System.lineSeparator());
        }
        sc.close();
        FileWriter fw = new FileWriter(HeritrixClient.baseDirectory + "crawled_urls.txt");
        fw.write(builder.toString());
        fw.close();
    }

    private BatchJob getBatchJobById(String id) {
        for(BatchJob job : batchJobs) {
            if(job.getId().equals(id)) {
                return job;
            }
        }
        return null;
    }
}

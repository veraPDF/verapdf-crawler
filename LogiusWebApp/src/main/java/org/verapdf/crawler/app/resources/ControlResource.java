package org.verapdf.crawler.app.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.crawling.StartBatchJobData;
import org.verapdf.crawler.domain.crawling.StartJobData;
import org.verapdf.crawler.domain.email.EmailAddress;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.office.OfficeFileData;
import org.verapdf.crawler.domain.report.SingleURLJobReport;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.ValidationService;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ControlResource {

    @Context
    private UriInfo uriInfo;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final ArrayList<BatchJob> batchJobs;
    private final HeritrixClient client;
    private final HeritrixReporter reporter;
    private final EmailServer emailServer;
    private final ValidationService service;
    private final ResourceManager resourceManager;
    private final CrawlJobDao crawlJobDao;
    private final InsertDocumentDao insertDocumentDao;

    ControlResource(HeritrixClient client, HeritrixReporter reporter,
                           EmailServer emailServer, ArrayList<BatchJob> batchJobs, ValidationService service,
                           ResourceManager resourceManager, CrawlJobDao crawlJobDao, DataSource dataSource) {
        this.client = client;
        this.reporter = reporter;
        this.emailServer = emailServer;
        this.batchJobs = batchJobs;
        this.service = service;
        this.resourceManager = resourceManager;
        this.crawlJobDao = crawlJobDao;
        this.insertDocumentDao = new InsertDocumentDao(dataSource);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(StartJobData startJobData){
        try {
            if (resourceManager.getResourceUri() == null && uriInfo != null) {
                resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
            }
            if (crawlJobDao.doesJobExist(trimUrl(startJobData.getDomain())) && !startJobData.isForceStart()) { // This URL has already been crawled and job is not forced to overwrite
                return new SingleURLJobReport(crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(startJobData.getDomain())).getId(), "", "", 0);
            } else {
                if (startJobData.isForceStart() && crawlJobDao.doesJobExist(trimUrl(startJobData.getDomain()))) { // This URL has already been crawled but the old job needs to be overwritten
                    client.teardownJob(crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(startJobData.getDomain())).getId());
                    crawlJobDao.removeJob(crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(startJobData.getDomain())));
                }
                // Brand new URL
                ArrayList<String> list = new ArrayList<>();
                if (startJobData.getDomain().startsWith("http://") || startJobData.getDomain().startsWith("https://")) {
                    list.add(trimUrl(startJobData.getDomain()));
                } else {
                    list.add(trimUrl(startJobData.getDomain()));
                    list.add(list.get(0).replace("https://", "http://"));
                }

                String job = UUID.randomUUID().toString();
                client.createJob(job, list);
                client.buildJob(job);
                client.launchJob(job);
                String jobStatus = client.getCurrentJobStatus(job);
                LocalDateTime now = LocalDateTime.now();
                CurrentJob newJob;
                if (startJobData.getDate() == null || startJobData.getDate().isEmpty()) {
                    newJob = new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                            null, startJobData.getReportEmail(), now);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    newJob = new CurrentJob(job, "", trimUrl(startJobData.getDomain()),
                            LocalDateTime.of(LocalDate.parse(startJobData.getDate(), formatter), LocalTime.MIN),
                            startJobData.getReportEmail(), now);
                }
                crawlJobDao.addJob(newJob);
                logger.info("Job creation on " + startJobData.getDomain());
                return new SingleURLJobReport(job, trimUrl(startJobData.getDomain()), jobStatus, 0);
            }
        }
        catch (Exception e) {
            logger.error("Error on job creation", e);
        }
        return new SingleURLJobReport("", trimUrl(startJobData.getDomain()), "unbuilt", 0);
    }

    @POST
    @Timed
    @Path("/pause/{job}")
    public void pauseJob(@PathParam("job") String job) {
        try {
            client.pauseJob(job);
            crawlJobDao.setStatus(job, "paused");
            logger.info("Job on "+ crawlJobDao.getCrawlUrl(job) + " paused");
        }
        catch (Exception e) {
            logger.error("Error pausing job", e);
        }
    }

    @POST
    @Timed
    @Path("/unpause/{job}")
    public void unpauseJob(@PathParam("job") String job) {
        try {
            client.unpauseJob(job);
            crawlJobDao.setStatus(job, "running");
            logger.info("Job on "+ crawlJobDao.getCrawlUrl(job) + " unpaused");
        }
        catch (Exception e) {
            logger.error("Error unpausing job", e);
        }
    }

    @POST
    @Timed
    @Path("/terminate/{job}")
    public void terminateJob(@PathParam("job") String job) {
        try {
            client.terminateJob(job);
            logger.info("Job on "+ crawlJobDao.getCrawlUrl(job) + " terminated");
        }
        catch (Exception e) {
            logger.error("Error terminating job", e);
        }
    }

    @POST
    @Timed
    @Path("/delete/{job}")
    public void deleteJob(@PathParam("job") String job) {
        try {
            client.terminateJob(job);
            logger.info("Job on "+ crawlJobDao.getCrawlUrl(job) + " deleted");
        }
        catch (Exception e) {
            logger.error("Error deleting job", e);
        }
    }

    @POST
    @Timed
    @Path("/restart/{job}")
    public void restartJob(@PathParam("job") String job) {
        try {
            CurrentJob currentJob = crawlJobDao.getCrawlJob(job);
            List<String> list = new ArrayList<>();
            list.add(currentJob.getCrawlURL());
            currentJob.setErrorOccurances(new HashMap<>());
            crawlJobDao.setJobFinished(job, false);
            client.teardownJob(job);
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            logger.info("Job on "+ currentJob.getCrawlURL() + " restarted");
        }
        catch (Exception e) {
            logger.error("Error restarting job", e);
        }
    }


    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) {
        try {
            if (resourceManager.getResourceUri() == null && uriInfo != null) {
                resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
            String jobURL = crawlJobDao.getCrawlJob(job).getJobURL();
            SingleURLJobReport result;
            if (jobURL.equals("")) {
                result = reporter.getReport(job, crawlJobDao.getCrawlSince(job));
            } else {
                result = reporter.getReport(job, jobURL, crawlJobDao.getCrawlSince(job));
            }
            CurrentJob jobData = crawlJobDao.getCrawlJob(job);

            if (result.getStatus().startsWith("finished") || result.getStatus().startsWith("aborted")) {
                if (!jobData.getReportEmail().equals("") && !jobData.isFinished()) {
                    String subject = "Crawl job";
                    String text = "Crawl job on " + jobData.getCrawlURL() + " was finished with status " + result.getStatus() +
                            "\nResults are available at " + resourceManager.getResourceUri().replace("api/", "jobinfo?id=") + job;
                    SendEmail.send(jobData.getReportEmail(), subject, text, emailServer);
                    crawlJobDao.setJobFinished(job, true);
                }
                if (jobData.getJobURL().equals("")) {
                    crawlJobDao.setJobUrl(job, client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt", ""));
                    //jobData.setFinishTime(LocalDateTime.now());
                    crawlJobDao.writeFinishTime(crawlJobDao.getCrawlJob(job));
                }
                client.teardownJob(jobData.getId());
            }

            result.startTime = jobData.getStartTime().format(formatter) + " GMT";
            if (jobData.getFinishTime() != null) {
                result.finishTime = jobData.getFinishTime().format(formatter) + " GMT";
            } else {
                result.finishTime = "";
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on job data request", e);
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Timed
    @Path("/batch")
    @Produces("text/plain")
    @Consumes(MediaType.APPLICATION_JSON)
    public String startBatchJob(StartBatchJobData jobData) {
        String id = UUID.randomUUID().toString();
        BatchJob batch = new BatchJob(id, jobData.getReportEmail());
        logger.info("Batch job creation.");
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
    public String getBatchJob(@PathParam("job") String job) {
        StringBuilder responseHtml = new StringBuilder();
        StringBuilder jobList = new StringBuilder();
        BatchJob batchJob = getBatchJobById(job);
        boolean isBatchFinished = true;
        for(String domain : batchJob.getDomains()) {
            try {
                jobList.append("<li>");
                jobList.append("Job on " + domain + ", ");
                SingleURLJobReport report = getJob(crawlJobDao.getCrawlJobByCrawlUrl(domain).getId());
                jobList.append(report.getStatus());
                isBatchFinished = isBatchFinished && (report.getStatus().startsWith("finished") || report.getStatus().startsWith("aborted"));
                jobList.append(", <a href=\"");
                jobList.append(resourceManager.getResourceUri().replace("api/", "jobinfo?id=") + crawlJobDao.getCrawlJobByCrawlUrl(domain).getId());
                jobList.append("\">details</a>.");
                jobList.append("</li>");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(isBatchFinished) {
            batchJob.setFinished();
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
        logger.info("Email address updated for job " + address.getJob());
        crawlJobDao.setReportEmail(address.getJob(), address.getEmailAddress());
    }

    @POST
    @Timed
    @Path("/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addValidationJob(ValidationJobData data) {
        logger.info("Received information about PDF file");
        String[] parts = data.getJobDirectory().split("/");
        data.errorOccurances = crawlJobDao.getCrawlJob(parts[parts.length - 3]).getErrorOccurances();
        try {
            service.addJob(data);
        }
        catch (IOException e) {
            logger.error("Error on adding file for validation", e);
        }
    }

    @POST
    @Timed
    @Path("/microsoft_office")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addMicrosoftOfficeFile(OfficeFileData data) {
        logger.info("Received information about Microsoft office file");
        insertDocumentDao.addMicrosoftOfficeFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
    }

    @POST
    @Timed
    @Path("/odf")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addOdfFile(OfficeFileData data) {
        logger.info("Received information about ODF file");
        insertDocumentDao.addOdfFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
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

    private BatchJob getBatchJobById(String id) {
        for(BatchJob job : batchJobs) {
            if(job.getId().equals(id)) {
                return job;
            }
        }
        return null;
    }
}

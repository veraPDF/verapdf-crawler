package org.verapdf.crawler.app.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.*;
import org.verapdf.crawler.domain.email.EmailAddress;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.office.OfficeDocumentData;
import org.verapdf.crawler.domain.report.CrawlJobReport;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.jobs.BatchJobDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.ValidationService;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    private static final String[] ODF_SUFFIXES = {".odt", ".ods", ".odp"};
    private static final String[] OFFICE_SUFFIXES = {".doc", ".xls", ".ppt"};
    private static final String[] OOXML_SUFFIXES = {".docx", ".xlsx", ".pptx"};

    @Context
    private UriInfo uriInfo;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final HeritrixClient client;
    private final HeritrixReporter reporter;
    private final EmailServer emailServer;
    private final ValidationService service;
    private final ResourceManager resourceManager;
    private final CrawlJobDao crawlJobDao;
    private final InsertDocumentDao insertDocumentDao;
    private final BatchJobDao batchJobDao;

    ControlResource(HeritrixClient client, HeritrixReporter reporter,
                    EmailServer emailServer, ValidationService service,
                    ResourceManager resourceManager, CrawlJobDao crawlJobDao,
                    DataSource dataSource, BatchJobDao batchJobDao) {
        this.client = client;
        this.reporter = reporter;
        this.emailServer = emailServer;
        this.service = service;
        this.resourceManager = resourceManager;
        this.crawlJobDao = crawlJobDao;
        this.insertDocumentDao = new InsertDocumentDao(dataSource);
        this.batchJobDao = batchJobDao;
    }

    @POST
    @Timed
    @Path("/pause/{job}")
    public void pauseJob(@PathParam("job") String job) {
        try {
            client.pauseJob(job);
            crawlJobDao.setStatus(job, "paused");
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " paused");
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
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " unpaused");
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
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " terminated");
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
            logger.info("Crawl job on "+ crawlJobDao.getCrawlUrl(job) + " deleted");
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
            crawlJobDao.setJobFinished(job, false);
            client.teardownJob(job);
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            logger.info("Crawl job on "+ currentJob.getCrawlURL() + " restarted");
        }
        catch (Exception e) {
            logger.error("Error restarting job", e);
        }
    }

    @POST
    @Timed
    @Produces("text/plain")
    @Consumes(MediaType.APPLICATION_JSON)
    public String startBatchJob(StartBatchJobData jobData) {
        String id = UUID.randomUUID().toString();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        BatchJob batch = new BatchJob(id, jobData.getReportEmail(),
                LocalDateTime.of(LocalDate.parse(jobData.getDate(), dateFormatter), LocalTime.MIN));
        logger.info("Batch job creation on domains: " + String.join(", ",jobData.getDomains()));
        for(String domain : jobData.getDomains()) {
            batch.getCrawlJobs().add(startCrawlJob(domain, jobData.isDoOverwrite()));
        }
        batchJobDao.addBatchJob(batch);
        return id;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{job}")
    public List<CrawlJobReport> getBatchJob(@PathParam("job") String jobId) {
        if (resourceManager.getResourceUri() == null && uriInfo != null) {
            resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
        }
        List<CrawlJobReport> result = new ArrayList<>();
        boolean isBatchJobFinished = true;
        BatchJob job = batchJobDao.getBatchJob(jobId);
        for(String crawlJobId: job.getCrawlJobs()) {
            CrawlJobReport report = getJob(crawlJobId, job.getCrawlSinceTime());
            result.add(report);
            if(report.getFinishTime() == null) {
                isBatchJobFinished = false;
            }
        }
        if(isBatchJobFinished && !job.isFinished()) {
            batchJobDao.setJobFinished(jobId);
            List<String> domains = new ArrayList<>();
            for(String crawlJobId: job.getCrawlJobs()) {
                domains.add(crawlJobDao.getCrawlUrl(crawlJobId));
            }
            if(job.getEmailAddress() != null && !job.getEmailAddress().equals("")) {
                String subject = "Crawl job";
                String text = "Batch job was finished successfully. List of crawled domains:\n" + String.join("\n ", domains);
                SendEmail.send(job.getEmailAddress(), subject, text, emailServer);
            }
        }
        return result;
    }

    @POST
    @Timed
    @Path("/email")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setReportEmail(EmailAddress address) {
        logger.info("Email address updated for batch job " + address.getBatchJobId());
        batchJobDao.setReportEmail(address.getBatchJobId(), address.getEmailAddress());
    }

    @POST
    @Timed
    @Path("/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addValidationJob(ValidationJobData data) {
        logger.info("Received information about PDF file");
        try {
            service.addJob(data);
        }
        catch (IOException e) {
            logger.error("Error on adding file for validation", e);
        }
    }

    @POST
    @Timed
    @Path("/office_document")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addMicrosoftOfficeFile(OfficeDocumentData data) {
        logger.info("Received information about office document " + data.getFileUrl());
        if(stringEndsWithItemFromList(data.getFileUrl(), ODF_SUFFIXES)) {
            insertDocumentDao.addOdfFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OFFICE_SUFFIXES)) {
            insertDocumentDao.addMicrosoftOfficeFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OOXML_SUFFIXES)) {
            insertDocumentDao.addOpenOfficeXMLFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
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

    private boolean stringEndsWithItemFromList(String string, String[] suffixes) {
        return Arrays.stream(suffixes).parallel().anyMatch(string::endsWith);
    }

    private String startCrawlJob(String domain, boolean overwrite){
        // TODO: rework overwirite
        overwrite = false;
        try {
            if (resourceManager.getResourceUri() == null && uriInfo != null) {
                resourceManager.setResourceUri(uriInfo.getBaseUri().toString());
            }
            if (crawlJobDao.doesJobExist(trimUrl(domain)) && !overwrite) { // This URL has already been crawled and job is not forced to overwrite
                return crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(domain)).getId();
            } else {
                if (overwrite && crawlJobDao.doesJobExist(trimUrl(domain))) { // This URL has already been crawled but the old job needs to be overwritten
                    client.teardownJob(crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(domain)).getId());
                    crawlJobDao.removeJob(crawlJobDao.getCrawlJobByCrawlUrl(trimUrl(domain)));
                }
                // Brand new URL
                ArrayList<String> list = new ArrayList<>();
                if (domain.startsWith("http://") || domain.startsWith("https://")) {
                    list.add(trimUrl(domain));
                } else {
                    list.add(trimUrl(domain));
                    list.add(list.get(0).replace("https://", "http://"));
                }

                String id = UUID.randomUUID().toString();
                client.createJob(id, list);
                client.buildJob(id);
                client.launchJob(id);
                crawlJobDao.addJob(new CurrentJob(id, "", trimUrl(domain), LocalDateTime.now()));
                logger.info("Job creation on " + domain);
                return id;
            }
        }
        catch (Exception e) {
            logger.error("Error on job creation", e);
            return "";
        }
    }

    private CrawlJobReport getJob(String job, LocalDateTime crawlSince) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
            String jobURL = crawlJobDao.getCrawlJob(job).getJobURL();
            CrawlJobReport result;
            if (jobURL.equals("")) {
                result = reporter.getReport(job, crawlSince);
            } else {
                result = reporter.getReport(job, jobURL, crawlSince);
            }
            crawlJobDao.setStatus(job, result.getStatus());
            CurrentJob jobData = crawlJobDao.getCrawlJob(job);

            if (result.getStatus().startsWith("finished") || result.getStatus().startsWith("aborted")) {
                if (!jobData.isFinished()) {
                    crawlJobDao.setJobFinished(job, true);
                }
                if (jobData.getJobURL().equals("")) {
                    crawlJobDao.setJobUrl(job, client.getValidPDFReportUri(job).replace("mirror/Valid_PDF_Report.txt", ""));
                    logger.info("Writing finish time for job " + job);
                    result.setFinishTime(crawlJobDao.writeFinishTime(job));
                }
                client.teardownJob(jobData.getId());
            }

            result.setStartTime(jobData.getStartTime().format(formatter) + " GMT");
            if (jobData.getFinishTime() != null) {
                result.setFinishTime(jobData.getFinishTime().format(formatter) + " GMT");
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on job data request", e);
            e.printStackTrace();
        }
        return null;
    }
}

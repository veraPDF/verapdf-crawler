package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CrawlRequest;
import org.verapdf.crawler.domain.crawling.CrawlJob;
import org.verapdf.crawler.domain.report.CrawlJobSummary;
import org.verapdf.crawler.domain.report.DocumentList;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/report")
public class ReportResource {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final HeritrixReporter reporter;
    private final CrawlRequestDao crawlRequestDao;
    private final CrawlJobDao crawlJobDao;

    ReportResource(HeritrixReporter reporter, CrawlJobDao crawlJobDao, CrawlRequestDao crawlRequestDao) {
        this.reporter = reporter;
        this.crawlJobDao = crawlJobDao;
        this.crawlRequestDao = crawlRequestDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{batchJob}/{crawlJob}")
    public Response getODSReport(@PathParam("batchJob") String batchJob, @PathParam("crawlJob") String crawlJob) {
        try {
            CrawlJob currentJob = crawlJobDao.getCrawlJob(crawlJob);
            String jobURL = currentJob.getJobURL();
            File file;
            if (jobURL.equals("")) {
                file = reporter.buildODSReport(crawlJob, crawlRequestDao.getCrawlSince(batchJob));
            } else {
                file = reporter.buildODSReport(crawlJob, jobURL, crawlRequestDao.getCrawlSince(batchJob));
            }
            logger.info("ODS report requested for job " + crawlJob);
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                    .build();
        }
        catch (Exception e) {
            logger.error("Error on ODS report request for job " + crawlJob, e);
        }
        return Response.serverError().build();
    }

    @GET
    @Path("/{job}")
    public List<CrawlJobSummary> getReport(@PathParam("job") String job) {
        try {
            CrawlRequest crawlRequest = crawlRequestDao.getBatchJob(job);
            logger.info("Job report requested for batch job " + job);
            List<CrawlJobSummary> result = new ArrayList<>();
            for(String crawlJobId: crawlRequest.getCrawlJobs()) {
                String jobURL = crawlJobDao.getCrawlJob(crawlJobId).getJobURL();
                if (jobURL.equals("")) {
                    result.add(reporter.getReport(crawlJobId, crawlRequest.getCrawlSinceTime()));
                } else {
                    result.add(reporter.getReport(crawlJobId, jobURL, crawlRequest.getCrawlSinceTime()));
                }
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on report request for batch job " + job, e);
        }
        return null;
    }

    @GET
    @Path("office_list/{job}")
    public List<DocumentList> getOfficeReport(@PathParam("job") String job) {
        try {
            logger.info("List of Microsoft office files requested for batch job " + job);
            List<DocumentList> result = new ArrayList<>();
            CrawlRequest crawlRequest = crawlRequestDao.getBatchJob(job);
            for(String crawlJobId: crawlRequest.getCrawlJobs()) {
                result.add(new DocumentList(crawlJobDao.getCrawlUrl(crawlJobId), reporter.getOfficeReport(crawlJobId, crawlRequest.getCrawlSinceTime())));
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on list of Microsoft Office files request for batch job "+ job, e);
        }
        return null;
    }

    @GET
    @Path("invalid_pdf_list/{job}")
    public List<DocumentList> getInvalidPdfReport(@PathParam("job") String job) {
        try {
            logger.info("List of invalid PDF documents requested for batch job " + job);
            List<DocumentList> result = new ArrayList<>();
            CrawlRequest crawlRequest = crawlRequestDao.getBatchJob(job);
            for(String crawlJobId: crawlRequest.getCrawlJobs()) {
                result.add(new DocumentList(crawlJobDao.getCrawlUrl(crawlJobId), reporter.getInvalidPdfReport(crawlJobId, crawlRequest.getCrawlSinceTime())));
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on list of invalid PDF documents request for batch job "+ job, e);
        }
        return null;
    }

    @GET
    @Path("ooxml_list/{job}")
    public List<DocumentList> getOoxmlReport(@PathParam("job") String job) {
        try {
            logger.info("List of Open office XML files requested for batch job " + job);
            List<DocumentList> result = new ArrayList<>();
            CrawlRequest crawlRequest = crawlRequestDao.getBatchJob(job);
            for(String crawlJobId: crawlRequest.getCrawlJobs()) {
                result.add(new DocumentList(crawlJobDao.getCrawlUrl(crawlJobId), reporter.getOoxmlReport(crawlJobId, crawlRequest.getCrawlSinceTime())));
            }
            return result;
        }
        catch (Exception e) {
            logger.error("Error on list of Open office XML files request for batch job "+ job, e);
        }
        return null;
    }
}

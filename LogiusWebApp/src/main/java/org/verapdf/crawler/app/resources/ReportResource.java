package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.report.ValidationError;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/report")
public class ReportResource {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final HeritrixReporter reporter;
    private final ResourceManager resourceManager;
    private final CrawlJobDao crawlJobDao;

    ReportResource(HeritrixReporter reporter, ResourceManager resourceManager, CrawlJobDao crawlJobDao) {
        this.reporter = reporter;
        this.resourceManager = resourceManager;
        this.crawlJobDao = crawlJobDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) {
        try {
            CurrentJob currentJob = crawlJobDao.getCrawlJob(job);
            String jobURL = currentJob.getJobURL();
            File file;
            if (jobURL.equals("")) {
                file = reporter.buildODSReport(job, crawlJobDao.getCrawlSince(job));
            } else {
                file = reporter.buildODSReport(job, jobURL, crawlJobDao.getCrawlSince(job));
            }
            logger.info("ODS report requested");
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                    .build();
        }
        catch (Exception e) {
            logger.error("Error on ODS report request", e);
        }
        return null;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/html_report/{job}")
    public String getHtmlReport(@PathParam("job") String job) {
        try {
            String jobURL = crawlJobDao.getCrawlJob(job).getJobURL();
            String htmlReport;
            if (jobURL.equals("")) {
                htmlReport = reporter.buildHtmlReport(job, crawlJobDao.getCrawlSince(job));
            } else {
                htmlReport = reporter.buildHtmlReport(job, jobURL, crawlJobDao.getCrawlSince(job));
            }
            htmlReport = htmlReport.replace("INVALID_PDF_REPORT", resourceManager.getResourceUri() + "report/invalid_pdf_list/" + job);
            htmlReport = htmlReport.replace("OFFICE_REPORT", resourceManager.getResourceUri() + "report/office_list/" + job);
            logger.info("HTML report requested");
            return htmlReport;
        }
        catch (Exception e) {
            logger.error("Exception on HTML report request", e);
        }
        return "";
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("office_list/{job}")
    public String getOfficeReport(@PathParam("job") String job) {
        try {
            String result = String.join("\n", reporter.getOfficeReport(job, crawlJobDao.getCrawlSince(job)));
            logger.info("List of Microsoft Office files requested");
            return addLinksToUrlList(result).toString();
        }
        catch (Exception e) {
            logger.error("Error on list of Microsoft Office files request", e);
        }
        return "";
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("invalid_pdf_list/{job}")
    public String getInvalidPdfReport(@PathParam("job") String job) {
        try {
            CurrentJob jobData = crawlJobDao.getCrawlJob(job);
            StringBuilder result = new StringBuilder("</table><h2>File details<h2>");
            result.append(reporter.getInvalidPdfHtmlReport(job, jobData.getCrawlSinceTime()));
            logger.info("List of invalid PDF documents requested");
            return result.toString();
        }
        catch (Exception e) {
            logger.error("Error on list of invalid PDF documents request", e);
        }
        return "";
    }

    private StringBuilder addLinksToUrlList(String list) {
        StringBuilder result = new StringBuilder();
        Scanner scanner = new Scanner(list);
        while (scanner.hasNextLine()) {
            String url = scanner.nextLine();
            result.append("<p><a href=\"");
            result.append(url);
            result.append("\">");
            result.append(url);
            result.append("</a></p>");
        }
        return result;
    }
}

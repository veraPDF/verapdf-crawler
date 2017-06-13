package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.database.MySqlCredentials;
import org.verapdf.crawler.domain.report.ValidationError;
import org.verapdf.crawler.report.HeritrixReporter;

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
    private final MySqlCredentials credentials;

    public ReportResource(HeritrixReporter reporter, ResourceManager resourceManager, MySqlCredentials credentials) {
        this.reporter = reporter;
        this.resourceManager = resourceManager;
        this.credentials = credentials;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) {
        try {
            CurrentJob currentJob = getJobById(job);
            String jobURL = currentJob.getJobURL();
            File file;
            if (jobURL.equals("")) {
                file = reporter.buildODSReport(job, getTimeByJobId(job));
            } else {
                file = reporter.buildODSReport(job, jobURL, getTimeByJobId(job));
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
            String jobURL = getExistingJobURLbyJobId(job);
            String htmlReport;
            if (jobURL.equals("")) {
                htmlReport = reporter.buildHtmlReport(job, getTimeByJobId(job));
            } else {
                htmlReport = reporter.buildHtmlReport(job, jobURL, getTimeByJobId(job));
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
            String jobURL = getExistingJobURLbyJobId(job);
            String result;
            if (jobURL.equals("")) {
                result = reporter.getOfficeReport(job, getTimeByJobId(job));
            } else {
                result = reporter.getOfficeReport(job, jobURL, getTimeByJobId(job));
            }
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
            CurrentJob jobData = getJobById(job);
            String jobURL = jobData.getJobURL();
            StringBuilder result = new StringBuilder("<h2>Most common issues<h2><table>");
            int i = 0;
            for (Map.Entry<ValidationError, Integer> record : sortFailedRules(jobData)) {
                i++;
                result.append("<tr style=\"BACKGROUND: #dcdaf6\"><td>");
                result.append(record.getKey().toString());
                result.append("</td></tr><tr style=\"BACKGROUND: #dcdaf6\"><td>");
                result.append(record.getValue());
                result.append(" occurrences.</td></tr> <tr></tr>");
                if (i == 10) {
                    break;
                }
            }
            result.append("</table><h2>File details<h2>");
            if (jobURL.equals("")) {
                result.append(reporter.getInvalidPDFReport(job, jobData.getCrawlSinceTime()));
            } else {
                result.append(reporter.getInvalidPDFReport(job, jobURL, jobData.getCrawlSinceTime()));
            }
            logger.info("List of invalid PDF documents requested");
            return result.toString();
        }
        catch (Exception e) {
            logger.error("Error on list of invalid PDF documents request", e);
        }
        return "";
    }

    private CurrentJob getJobById(String job) {
        return resourceManager.getJobById(job);
    }

    private LocalDateTime getTimeByJobId(String job) { return resourceManager.getTimeByJobId(job); }

    private String getExistingJobURLbyJobId(String job) { return resourceManager.getExistingJobURLbyJobId(job); }

    private List<Map.Entry<ValidationError, Integer>> sortFailedRules(CurrentJob job) {
        Map<ValidationError, Integer> sortedMap = new HashMap<>();
        List<Map.Entry<ValidationError, Integer>> list = new LinkedList<>( job.getErrorOccurances().entrySet() );
        Collections.sort( list, (o1, o2) -> (o2.getValue()).compareTo( o1.getValue() ));
        return list;
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

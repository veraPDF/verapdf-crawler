package org.verapdf.crawler.resources;

import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.report.ValidationError;
import org.verapdf.crawler.report.HeritrixReporter;
import org.xml.sax.SAXException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/report")
public class ReportResource {

    private HeritrixReporter reporter;
    private ArrayList<CurrentJob> currentJobs;
    private ResourceManager resourceManager;

    public ReportResource(HeritrixReporter reporter, ArrayList<CurrentJob> currentJobs, ResourceManager resourceManager) {
        this.reporter = reporter;
        this.currentJobs = currentJobs;
        this.resourceManager = resourceManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        CurrentJob currentJob = getJobById(job);
        String jobURL = currentJob.getJobURL();
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
        htmlReport = htmlReport.replace("INVALID_PDF_REPORT", resourceManager.getResourceUri() + "report/invalid_pdf_list/" + job);
        htmlReport = htmlReport.replace("OFFICE_REPORT", resourceManager.getResourceUri()  + "report/office_list/" + job);
        return htmlReport;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("office_list/{job}")
    public String getOfficeReport(@PathParam("job") String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String jobURL = getExistingJobURLbyJobId(job);
        String result;
        if(jobURL.equals("")) {
            result = reporter.getOfficeReport(job, getTimeByJobId(job));
        }
        else{
            result = reporter.getOfficeReport(job, jobURL, getTimeByJobId(job));
        }
        return addLinksToUrlList(result).toString();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("invalid_pdf_list/{job}")
    public String getInvalidPdfReport(@PathParam("job") String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        CurrentJob jobData = getJobById(job);
        String jobURL = jobData.getJobURL();
        StringBuilder result = new StringBuilder("<h2>Most common issues<h2><table>");
        int i = 0;
        for(Map.Entry<ValidationError, Integer> record: sortFailedRules(jobData)) {
            i++;
            result.append("<tr style=\"BACKGROUND: #dcdaf6\"><td>");
            result.append(record.getKey().toString());
            result.append("</td></tr><tr style=\"BACKGROUND: #dcdaf6\"><td>");
            result.append(record.getValue());
            result.append(" occurrences.</td></tr> <tr></tr>");
            if(i == 10) {
                break;
            }
        }
        result.append("</table><h2>File details<h2>");
        if(jobURL.equals("")) {
            result.append(reporter.getInvalidPDFReport(job, jobData.getCrawlSinceTime()));
        }
        else{
            result.append(reporter.getInvalidPDFReport(job, jobURL, jobData.getCrawlSinceTime()));
        }
        return result.toString();
    }

    public CurrentJob getJobById(String job) {
        return resourceManager.getJobById(job);
    }

    private LocalDateTime getTimeByJobId(String job) { return resourceManager.getTimeByJobId(job); }

    private String getExistingJobURLbyJobId(String job) { return resourceManager.getExistingJobURLbyJobId(job); }

    private List<Map.Entry<ValidationError, Integer>> sortFailedRules(CurrentJob job) {
        Map<ValidationError, Integer> sortedMap = new HashMap<>();
        List<Map.Entry<ValidationError, Integer>> list = new LinkedList<Map.Entry<ValidationError, Integer>>( job.getErrorOccurances().entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<ValidationError, Integer>>()
        {
            public int compare( Map.Entry<ValidationError, Integer> o1, Map.Entry<ValidationError, Integer> o2 )
            {
                return (o2.getValue()).compareTo( o1.getValue() );
            }
        } );
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

package org.verapdf.crawler.resources;

import org.jopendocument.dom.OOUtils;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.verapdf.crawler.api.*;
import com.codahale.metrics.annotation.Timed;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.helpers.emailUtils.SendEmail;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class CrawlJobResource {
    protected HeritrixClient client;
    protected HashMap<String, String> currentJobs;
    protected String reportToEmail;
    protected EmailServer emailServer;

    public CrawlJobResource(HeritrixClient client, EmailServer emailServer)
    {
        this.client = client;
        this.emailServer = emailServer;
        currentJobs = new HashMap<>();
    }

    public String getreportEmail() {
        return reportToEmail;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public SingleURLJobReport startJob(Domain domain) {
        ArrayList<String> list = new ArrayList<>();
        list.add(domain.getDomain());
        String jobStatus = "";

        String job = UUID.randomUUID().toString();
        try {
            client.createJob(job, list);
            client.buildJob(job);
            client.launchJob(job);
            jobStatus = client.getCurrentJobStatus(job);
            currentJobs.put(job, domain.getDomain());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return new SingleURLJobReport(job, domain.getDomain(), jobStatus, 0);
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
        return currentJobs;
    }

    @GET
    @Timed
    @Path("/{job}")
    public SingleURLJobReport getJob(@PathParam("job") String job) {
        String jobStatus = "";
        String domain = "";
        int numberOfCrawledUrls = 0;
        SingleURLJobReport result = new SingleURLJobReport(job, domain, jobStatus, numberOfCrawledUrls);
        try {
            jobStatus = client.getCurrentJobStatus(job);
            domain = client.getListOfCrawlUrls(job).get(0);
            numberOfCrawledUrls = client.getDownloadedCount(job);
            if(client.isJobFinished(job)) {
                if(reportToEmail != null) {
                    SendEmail.send(reportToEmail,
                            "Crawl report",
                            "The crawl job on " + domain + " is finished.",
                            emailServer);
                }
            }
            result = new SingleURLJobReport(job, domain, jobStatus, numberOfCrawledUrls);
            result.setPdfStatistics(client.getValidationStatistics(job));
            result.setNumberOfODFDocuments(client.getODFFileCount(job));
            result.setNumberOfOfficeDocuments(client.getOfficeFileCount(job));
            result.setOfficeReportURL(client.getOfficeReportUri(job));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return result;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/ods_report/{job}")
    public Response getODSReport(@PathParam("job") String job) {
        File file;
        try {
            file = buildODSReport(job);
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" ) //optional
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return Response.serverError().build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/html_report/{job}")
    public String getHtmlReport(@PathParam("job") String job) {
        SingleURLJobReport reportData = getJob(job);
        StringBuilder builder = new StringBuilder();
        builder.append("<p>Valid PDF files ");
        builder.append(reportData.getPdfStatistics().getNumberOfValidPDFs());
        builder.append("</p>");
        builder.append("<p>ODF files ");
        builder.append(reportData.getNumberOfODFDocuments());
        builder.append("</p>");
        builder.append("<p><font color=\"green\">Total ");
        builder.append(reportData.getNumberOfODFDocuments() +
                reportData.getPdfStatistics().getNumberOfValidPDFs());
        builder.append("</font></p>");

        builder.append("<p>Invalid PDF files ");
        builder.append(reportData.getPdfStatistics().getNumberOfInvalidPDFs());
        builder.append("</p>");
        builder.append("<p>Microsoft Office files ");
        builder.append(reportData.getNumberOfOfficeDocuments());
        builder.append("</p>");
        builder.append("<p><font color=\"red\">Total ");
        builder.append(reportData.getNumberOfOfficeDocuments() +
                reportData.getPdfStatistics().getNumberOfInvalidPDFs());
        builder.append("</font></p>");
        builder.append("<p><a href=\"");
        builder.append(reportData.getPdfStatistics().getInvalidPDFReportURL());
        builder.append("\">Invalid PDF URLs</a></p>");
        builder.append("<p><a href=\"");
        builder.append(reportData.getOfficeReportURL());
        builder.append("\">Microsoft Office files URLs</a></p>");

        return builder.toString();
    }

    private File buildODSReport(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        SingleURLJobReport reportData = getJob(job);

        File file = new File("src/main/resources/sample_report.ods");
        final Sheet totalSheet = SpreadSheet.createFromFile(file).getSheet(0);
        totalSheet.ensureColumnCount(2);
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPDFs(),1, 0);
        totalSheet.setValueAt(reportData.getNumberOfODFDocuments(),1, 1);
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPDFs() +
                reportData.getNumberOfODFDocuments(), 1, 2);
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfInvalidPDFs(),1, 3);
        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments(),1, 4);
        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments() +
                reportData.getPdfStatistics().getNumberOfInvalidPDFs(), 1, 5);

        SpreadSheet spreadSheet = totalSheet.getSpreadSheet();
        setLinesInSheet(spreadSheet.getSheet(1), reportData.getOfficeReportURL());
        setLinesInSheet(spreadSheet.getSheet(2), reportData.getPdfStatistics().getInvalidPDFReportURL());

        File ODSReport = new File("src/main/resources/report.ods");
        OOUtils.open(spreadSheet.saveAs(ODSReport));
        return ODSReport;
    }

    private void setLinesInSheet(Sheet sheet, String lines) {
        Scanner scanner = new Scanner(lines);
        int i = 0;
        sheet.ensureColumnCount(1);
        while(scanner.hasNext()) {
            sheet.ensureRowCount(i + 1);
            sheet.setValueAt(scanner.nextLine(), 0, i);
            i++;
        }
        scanner.close();
    }
}

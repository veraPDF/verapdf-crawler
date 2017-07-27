package org.verapdf.crawler.report;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.verapdf.crawler.domain.validation.ValidationReportData;
import org.verapdf.crawler.domain.report.SingleURLJobReport;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.repository.document.ReportFileDao;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HeritrixReporter {

    private final HeritrixClient client;
    private final ReportFileDao reportFileDao;

    public HeritrixReporter(HeritrixClient client, DataSource dataSource) {
        this.client = client;
        reportFileDao = new ReportFileDao(dataSource);
    }

    // If time is null settings were not provided
    public SingleURLJobReport getReport(String job, LocalDateTime time) throws IOException, ParserConfigurationException, SAXException {
        SingleURLJobReport result = new SingleURLJobReport(job,
                client.getListOfCrawlUrls(job).get(0),
                client.getCurrentJobStatus(job).replaceAll("\\s+",""),
                client.getDownloadedCount(job));
        result.setPdfStatistics(reportFileDao.getValidationStatistics(job, time));
        result.setNumberOfODFDocuments(reportFileDao.getNumberOfOdfFilesForJob(job, time));
        result.setNumberOfOfficeDocuments(reportFileDao.getNumberOfMicrosoftFilesForJob(job, time));
        return result;
    }

    public SingleURLJobReport getReport(String job, String jobURL, LocalDateTime time) throws IOException {
        String config = client.getConfig(jobURL);
        SingleURLJobReport result = new SingleURLJobReport(job,
                HeritrixClient.getListOfCrawlUrlsFromXml(config).get(0),
                "finished",0);
        result.setPdfStatistics(reportFileDao.getValidationStatistics(job,time));
        result.setNumberOfODFDocuments(reportFileDao.getNumberOfOdfFilesForJob(job, time));
        result.setNumberOfOfficeDocuments(reportFileDao.getNumberOfMicrosoftFilesForJob(job, time));
        return result;
    }

    private File buildODSReport(SingleURLJobReport reportData, LocalDateTime time) throws IOException {
        File file = new File(HeritrixClient.baseDirectory + "sample_report.ods");
        final Sheet totalSheet = SpreadSheet.createFromFile(file).getSheet(0);
        totalSheet.ensureColumnCount(2);
        if(time != null) {
            totalSheet.setValueAt(time.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")) + " GMT", 1, 0);
        }
        else {
            totalSheet.setValueAt("", 0, 0);
        }
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPDFs(),1, 1);
        totalSheet.setValueAt(reportData.getNumberOfODFDocuments(),1, 2);
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPDFs() +
                reportData.getNumberOfODFDocuments(), 1, 3);
        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfInvalidPDFs(),1, 4);
        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments(),1, 5);
        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments() +
                reportData.getPdfStatistics().getNumberOfInvalidPDFs(), 1, 6);

        SpreadSheet spreadSheet = totalSheet.getSpreadSheet();
        setStringsInSheet(spreadSheet.getSheet(1), reportFileDao.getMicrosoftOfficeFiles(reportData.getId(), time));
        setValidationReportsInSheet(spreadSheet.getSheet(2), reportFileDao.getInvalidPdfFiles(reportData.getId(), time));

        File ODSReport = new File(HeritrixClient.baseDirectory + "report.ods");
        totalSheet.getSpreadSheet().saveAs(ODSReport);
        return ODSReport;
    }

    public File buildODSReport(String job, LocalDateTime time) throws IOException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildODSReport(reportData, time);
    }

    public File buildODSReport(String job, String jobURL, LocalDateTime time) throws IOException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildODSReport(reportData,time);
    }

    private String buildHtmlReport(SingleURLJobReport reportData, LocalDateTime time) {
        StringBuilder builder = new StringBuilder();
        if(time != null) {
            builder.append("<tr><td>Crawl files since date</td><td>");
            builder.append(time.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")));
            builder.append(" GMT</td></tr>");
        }
        builder.append("<tr><td>Compliant PDF/A files</td><td>");
        builder.append(reportData.getPdfStatistics().getNumberOfValidPDFs());
        builder.append("</td></tr>");
        builder.append("<tr><td>ODF files</td><td>");
        builder.append(reportData.getNumberOfODFDocuments());
        builder.append("</td></tr>");
        builder.append("<tr><td><font color=\"green\">Total</font></td><td><font color=\"green\">");
        builder.append(reportData.getNumberOfODFDocuments() +
                reportData.getPdfStatistics().getNumberOfValidPDFs());
        builder.append("</font></td></tr>");
        if(reportData.getPdfStatistics().getNumberOfInvalidPDFs() != 0) {
            builder.append("<tr><td><a href=\"INVALID_PDF_REPORT\">PDF documents that are not PDF/A</a></td><td> ");
        }
        else {
            builder.append("<tr><td>PDF documents that are not PDF/A</td><td> ");
        }
        builder.append(reportData.getPdfStatistics().getNumberOfInvalidPDFs());
        builder.append("</td></tr>");
        if(reportData.getNumberOfOfficeDocuments() != 0) {
            builder.append("<tr><td><a href=\"OFFICE_REPORT\">Microsoft Office files</a></td><td> ");
        }
        else {
            builder.append("<tr><td>Microsoft Office files</td><td> ");
        }
        builder.append(reportData.getNumberOfOfficeDocuments());
        builder.append("</td></tr>");
        builder.append("<tr><td><font color=\"red\">Total</font></td><td><font color=\"red\">");
        builder.append(reportData.getNumberOfOfficeDocuments() +
                reportData.getPdfStatistics().getNumberOfInvalidPDFs());
        builder.append("</font></td></tr>");

        return builder.toString();
    }

    public String buildHtmlReport(String job, LocalDateTime time) throws SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildHtmlReport(reportData, time);
    }

    public String buildHtmlReport(String job, String jobURL, LocalDateTime time) throws IOException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildHtmlReport(reportData, time);
    }

    public List<String> getOfficeReport(String job, LocalDateTime time) {
        return reportFileDao.getListOfOfficeFiles(job, time);
    }

    private void setValidationReportsInSheet(Sheet sheet, List<ValidationReportData> list) {
        int i = 1;
        sheet.ensureColumnCount(3);
        for(ValidationReportData data: list) {
            sheet.ensureRowCount(i + 1);
            sheet.setValueAt(data.getPassedRules(), 0, i);
            sheet.setValueAt(data.getFailedRules(), 1, i);
            sheet.setValueAt(data.getUrl(), 2, i);
            i++;
        }
    }

    private void setStringsInSheet(Sheet sheet, List<String> list) {
        int i = 1;
        sheet.ensureColumnCount(1);
        for(String line: list) {
            sheet.ensureRowCount(i + 1);
            sheet.setValueAt(line, 0, i);
            i++;
        }
    }

    public String getInvalidPdfHtmlReport(String job, LocalDateTime time) throws IOException {
        List<ValidationReportData> invalidPdfFiles = reportFileDao.getInvalidPdfFiles(job, time);
        StringBuilder builder = new StringBuilder();
        builder.append("<table>");
        builder.append("<tr><td>Passed rules</td><td>Failed rules</td><td>File location</td></tr>");
        for (ValidationReportData data : invalidPdfFiles) {
            builder.append("<tr><td>");
            builder.append(data.getPassedRules());
            builder.append("</td><td>");
            builder.append(data.getFailedRules());
            builder.append("</td><td><a href = \"");
            builder.append(data.getUrl());
            builder.append("\">");
            builder.append(data.getUrl());
            builder.append("</a></td></tr>");
        }
        builder.append("</table>");
        return builder.toString();
    }
}

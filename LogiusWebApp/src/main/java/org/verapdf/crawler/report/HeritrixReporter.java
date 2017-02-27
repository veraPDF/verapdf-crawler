package org.verapdf.crawler.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.verapdf.crawler.api.InvalidReportData;
import org.verapdf.crawler.api.PDFValidationStatistics;
import org.verapdf.crawler.api.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class HeritrixReporter {

    private HeritrixClient client;

    public HeritrixReporter(HeritrixClient client) {
        this.client = client;
    }

    // If time is null settings were not provided
    public SingleURLJobReport getReport(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        SingleURLJobReport result = new SingleURLJobReport(job,
                client.getListOfCrawlUrls(job).get(0),
                client.getCurrentJobStatus(job),
                client.getDownloadedCount(job));
        result.setPdfStatistics(getValidationStatistics(job, time));
        result.setNumberOfODFDocuments(getODFFileCount(job, time));
        result.officeReport = getOfficeReport(job, time);
        result.setNumberOfOfficeDocuments(getNumberOfLines(result.officeReport, null));
        result.setOfficeReportURL(getOfficeReportUri(job));
        return result;
    }

    public SingleURLJobReport getReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        return getReport(job, null);
    }

    public SingleURLJobReport getReport(String job, String jobURL, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String config = client.getLogFileByURL(jobURL + "sample_configuration.cxml");
        if(config.equals("")) {
            config = client.getLogFileByURL(jobURL + "crawler-beans.cxml");
        }
        SingleURLJobReport result = new SingleURLJobReport(job,
                HeritrixClient.getListOfCrawlUrlsFromXml(config).get(0),
                "Finished",0);
        result.setPdfStatistics(getValidationStatistics(job,
                jobURL + "mirror/Invalid_PDF_Report.txt",
                jobURL + "mirror/Valid_PDF_Report.txt",
                time));
        result.setNumberOfODFDocuments(getNumberOfLines(client.getLogFileByURL(jobURL + "mirror/ODFReport.txt"), time));
        result.officeReport = removeEarlyLines(client.getLogFileByURL(jobURL + "mirror/OfficeReport.txt"),time);
        result.setNumberOfOfficeDocuments(getNumberOfLines(result.officeReport, null));
        result.setOfficeReportURL(jobURL + "mirror/OfficeReport.txt");
        return result;
    }

    public File buildODSReport(SingleURLJobReport reportData, LocalDateTime time) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
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
        setLinesInSheet(spreadSheet.getSheet(1), reportData.officeReport);
        setLinesInSheet(spreadSheet.getSheet(2), reportData.getPdfStatistics().invalidPDFReport);

        File ODSReport = new File(HeritrixClient.baseDirectory + "report.ods");
        totalSheet.getSpreadSheet().saveAs(ODSReport);
        return ODSReport;
    }

    public File buildODSReport(String job, LocalDateTime time) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildODSReport(reportData, time);
    }

    public File buildODSReport(String job, String jobURL, LocalDateTime time) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildODSReport(reportData,time);
    }

    public String buildHtmlReport(SingleURLJobReport reportData, LocalDateTime time) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
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

    public String buildHtmlReport(String job, LocalDateTime time) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildHtmlReport(reportData, time);
    }

    public String buildHtmlReport(String job, String jobURL, LocalDateTime time) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildHtmlReport(reportData, time);
    }

    private String getInvalidPDFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt","Invalid_PDF_Report.txt");
    }

    public String getInvalidPDFReport(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String fileText = client.getLogFileByURL(getInvalidPDFReportUri(job));
        return getInvalidPdfListFromReportText(fileText, time);
    }

    public String getInvalidPDFReport(String job, String jobURL, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String fileText = client.getLogFileByURL(jobURL + "mirror/Invalid_PDF_Report.txt");
        return getInvalidPdfListFromReportText(fileText, time);
    }

    private PDFValidationStatistics getValidationStatistics(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getValidationStatistics(job, getInvalidPDFReportUri(job), client.getValidPDFReportUri(job), time);
    }

    private PDFValidationStatistics getValidationStatistics(String job, String invalidReport, String validReport, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        int numberOfInvalidPDFs, numberOfValidPDFs;
        String invalidPDFReport = client.getLogFileByURL(invalidReport);
        ArrayList<InvalidReportData> list = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(invalidPDFReport);
            ObjectMapper mapper = new ObjectMapper();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                list.add(mapper.readValue(line, InvalidReportData.class));
            }
            numberOfValidPDFs = getNumberOfLines(client.getLogFileByURL(validReport), time);
            numberOfInvalidPDFs = getNumberOfLines(list, time);
        }
        catch (IOException e) {
            return new PDFValidationStatistics(0,0, invalidReport);
        }
        PDFValidationStatistics result = new PDFValidationStatistics(numberOfInvalidPDFs, numberOfValidPDFs, invalidReport);
        result.invalidPDFReport = list;
        return result;
    }

    private String getODFReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getLogFileByURL(getODFReportUri(job));
    }

    private Integer getODFFileCount(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getNumberOfLines(getODFReport(job), time);
    }

    public String getOfficeReport(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return removeEarlyLines(client.getLogFileByURL(getOfficeReportUri(job)), time);
    }

    public String getOfficeReport(String job, String jobURL, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return removeEarlyLines(client.getLogFileByURL(jobURL + "mirror/OfficeReport.txt"), time);
    }

    private String getOfficeReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt", "OfficeReport.txt");
    }

    private String getODFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt", "ODFReport.txt");
    }

    private Integer getNumberOfLines(String report, LocalDateTime time) {
        Scanner sc = new Scanner(report);
        Integer result = 0;
        while(sc.hasNext()) {
            String line = sc.nextLine();
            if(time == null) {
                result++;
            }
            else {
                String fileTimeString = line.split("Last-Modified:.*, ")[1];
                fileTimeString = fileTimeString.substring(0, fileTimeString.length() - 4);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                LocalDateTime fileTime = LocalDateTime.parse(fileTimeString, formatter);
                if(time.isBefore(fileTime)) {
                    result++;
                }
            }
        }
        return result;
    }

    private Integer getNumberOfLines(ArrayList<InvalidReportData> list, LocalDateTime time) {
        Integer result = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        for(InvalidReportData data: list) {
            if(time == null) {
                result++;
            }
            else {
                String timestamp = data.getLastModified().split("Last-Modified:.*, ")[1];
                timestamp = timestamp.substring(0, timestamp.length()-4);
                if (LocalDate.parse(timestamp, formatter).isAfter(time.toLocalDate())) {
                    result++;
                }
            }
        }
        return result;
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

    private void setLinesInSheet(Sheet sheet, ArrayList<InvalidReportData> list) {
        int i = 1;
        sheet.ensureColumnCount(3);
        for(InvalidReportData data: list) {
            sheet.ensureRowCount(i + 1);
            sheet.setValueAt(data.getPassedRules(), 0, i);
            sheet.setValueAt(data.getFailedRules(), 1, i);
            sheet.setValueAt(data.getUrl(), 2, i);
            i++;
        }
    }

    private static String removeEarlyLines(String report, LocalDateTime time) {
        Scanner sc = new Scanner(report);
        StringBuilder builder = new StringBuilder();
        while(sc.hasNext()) {
            String line = sc.nextLine();
            String[] parts = line.split(", Last-Modified:.*, ");
            if(time == null) {
                builder.append(parts[0]);
                builder.append(System.lineSeparator());
            }
            else {
                String fileTimeString = parts[1].substring(0, parts[1].length() - 4);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                LocalDateTime fileTime = LocalDateTime.parse(fileTimeString, formatter);
                if (time.isBefore(fileTime)) {
                    builder.append(parts[0]);
                    builder.append(System.lineSeparator());
                }
            }
        }
        return builder.toString();
    }

    private String getInvalidPdfListFromReportText(String text, LocalDateTime time) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("<table>");
        builder.append("<tr><td>Passed rules</td><td>Failed rules</td><td>File location</td></tr>");
        Scanner scanner = new Scanner(text);
        ObjectMapper mapper = new ObjectMapper();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            InvalidReportData data = mapper.readValue(line, InvalidReportData.class);
            if(time == null) {
                appendInvalidPDFLine(builder, data);
            }
            else {
                String timestamp = data.getLastModified().split("Last-Modified:.*, ")[1];
                timestamp = timestamp.substring(0, timestamp.length()-4);
                if (LocalDate.parse(timestamp, formatter).isAfter(time.toLocalDate())) {
                    appendInvalidPDFLine(builder, data);
                }
            }
        }
        builder.append("</table>");
        return builder.toString();
    }

    private void appendInvalidPDFLine(StringBuilder builder, InvalidReportData data) {
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
}

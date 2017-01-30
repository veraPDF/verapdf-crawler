package org.verapdf.crawler.report;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.verapdf.crawler.api.PDFValidationStatistics;
import org.verapdf.crawler.api.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return getReport(job, (LocalDateTime) null);
    }

    public SingleURLJobReport getReport(String job, String jobURL, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String config = client.getLogFileByURL(jobURL + "sample_configuration.cxml");
        if(config.equals("")) {
            config = client.getLogFileByURL(jobURL + "crawler-beans.cxml");
        }
        SingleURLJobReport result = new SingleURLJobReport(job,
                HeritrixClient.getListOfCrawlUrlsFromXml(config).get(0),
                "Finished",
                getNumberOfLines(client.getLogFileByURL(jobURL + "logs/crawl-log"), time));
        result.setPdfStatistics(getValidationStatistics(job,
                jobURL + "mirror/Invalid_PDF_Report.txt",
                jobURL + "mirror/Valid_PDF_Report.txt",
                time));
        result.setNumberOfODFDocuments(getNumberOfLines(client.getLogFileByURL(jobURL + "mirror/ODFReport.txt"), time));
        result.officeReport = client.getLogFileByURL(jobURL + "mirror/OfficeReport.txt");
        result.setNumberOfOfficeDocuments(getNumberOfLines(result.officeReport, time));
        result.setOfficeReportURL(jobURL + "mirror/OfficeReport.txt");
        return result;
    }

    public File buildODSReport(SingleURLJobReport reportData) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        File file = new File(HeritrixClient.baseDirectory + "sample_report.ods");
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
        setLinesInSheet(spreadSheet.getSheet(1), reportData.officeReport);
        setLinesInSheet(spreadSheet.getSheet(2), reportData.getPdfStatistics().invalidPDFReport);

        File ODSReport = new File(HeritrixClient.baseDirectory + "report.ods");
        totalSheet.getSpreadSheet().saveAs(ODSReport);
        return ODSReport;
    }

    public File buildODSReport(String job, LocalDateTime time) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildODSReport(reportData);
    }

    public File buildODSReport(String job, String jobURL, LocalDateTime time) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildODSReport(reportData);
    }

    public String buildHtmlReport(SingleURLJobReport reportData) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
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
        builder.append("INVALID_PDF_REPORT");
        builder.append("\">Invalid PDF URLs</a></p>");
        builder.append("<p><a href=\"");
        builder.append("OFFICE_REPORT");
        builder.append("\">Microsoft Office files URLs</a></p>");

        return builder.toString();
    }

    public String buildHtmlReport(String job, LocalDateTime time) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, time);
        return buildHtmlReport(reportData);
    }

    public String buildHtmlReport(String job, String jobURL, LocalDateTime time) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, jobURL, time);
        return buildHtmlReport(reportData);
    }

    private String getInvalidPDFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt","Invalid_PDF_Report.txt");
    }

    public String getInvalidPDFReport(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return removeEarlyLines(client.getLogFileByURL(getInvalidPDFReportUri(job)),time);
    }

    public String getInvalidPDFReport(String job, String jobURL, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return removeEarlyLines(client.getLogFileByURL(jobURL + "mirror/Invalid_PDF_Report.txt"), time);
    }

    private PDFValidationStatistics getValidationStatistics(String job, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getValidationStatistics(job, getInvalidPDFReportUri(job), client.getValidPDFReportUri(job), time);
    }

    private PDFValidationStatistics getValidationStatistics(String job, String invalidReport, String validReport, LocalDateTime time) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        int numberOfInvalidPDFs = 0, numberOfValidPDFs = 0;
        String invalidPDFReport = client.getLogFileByURL(invalidReport);
        try {
            numberOfValidPDFs = getNumberOfLines(client.getLogFileByURL(validReport), time);
            numberOfInvalidPDFs = getNumberOfLines(invalidPDFReport, time);
        }
        catch (IOException e) {
            return new PDFValidationStatistics(0,0, invalidReport);
        }
        PDFValidationStatistics result = new PDFValidationStatistics(numberOfInvalidPDFs, numberOfValidPDFs, invalidReport);
        result.invalidPDFReport = removeEarlyLines(invalidPDFReport, time);
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

    public static String removeEarlyLines(String report, LocalDateTime time) {
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
}
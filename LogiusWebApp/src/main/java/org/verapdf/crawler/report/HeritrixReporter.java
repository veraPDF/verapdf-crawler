package org.verapdf.crawler.report;

import org.jopendocument.dom.OOUtils;
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
import java.util.Scanner;

public class HeritrixReporter {

    private HeritrixClient client;

    public HeritrixReporter(HeritrixClient client) {
        this.client = client;
    }

    public SingleURLJobReport getReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        SingleURLJobReport result = new SingleURLJobReport(job,
                client.getListOfCrawlUrls(job).get(0),
                client.getCurrentJobStatus(job),
                client.getDownloadedCount(job));
        result.setPdfStatistics(getValidationStatistics(job));
        result.setNumberOfODFDocuments(getODFFileCount(job));
        result.officeReport = getOfficeReport(job);
        result.setNumberOfOfficeDocuments(getNumberOfLines(result.officeReport));
        result.setOfficeReportURL(getOfficeReportUri(job));


        return result;
    }

    public SingleURLJobReport getReport(String job, String jobURL) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        String config = client.getLogFileByURL(jobURL + "sample_configuration.cxml");
        if(config.equals("")) {
            config = client.getLogFileByURL(jobURL + "crawler-beans.cxml");
        }
        SingleURLJobReport result = new SingleURLJobReport(job,
                HeritrixClient.getListOfCrawlUrlsFromXml(config).get(0),
                "Finished",
                getNumberOfLines(client.getLogFileByURL(jobURL + "logs/crawl-log")));
        result.setPdfStatistics(getValidationStatistics(job,
                jobURL + "mirror/Invalid_PDF_Report.txt",
                jobURL + "mirror/Valid_PDF_Report.txt"));
        result.setNumberOfODFDocuments(getNumberOfLines(client.getLogFileByURL(jobURL + "mirror/ODFReport.txt")));
        result.officeReport = client.getLogFileByURL(jobURL + "mirror/OfficeReport.txt");
        result.setNumberOfOfficeDocuments(getNumberOfLines(result.officeReport));
        result.setOfficeReportURL(jobURL + "mirror/OfficeReport.txt");
        return result;
    }

    public File buildODSReport(SingleURLJobReport reportData) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        File file = new File(client.baseDirectory + "/src/main/resources/sample_report.ods");
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

        File ODSReport = new File(client.baseDirectory + "/src/main/resources/report.ods");
        return ODSReport;
    }

    public File buildODSReport(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job);
        return buildODSReport(reportData);
    }

    public File buildODSReport(String job, String jobURL) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job, jobURL);
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
        builder.append(reportData.getPdfStatistics().getInvalidPDFReportURL());
        builder.append("\">Invalid PDF URLs</a></p>");
        builder.append("<p><a href=\"");
        builder.append(reportData.getOfficeReportURL());
        builder.append("\">Microsoft Office files URLs</a></p>");

        return builder.toString();
    }

    public String buildHtmlReport(String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job);
        return buildHtmlReport(reportData);
    }

    public String buildHtmlReport(String job, String jobURL) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job, jobURL);
        return buildHtmlReport(reportData);
    }

    private String getInvalidPDFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt","Invalid_PDF_Report.txt");
    }

    private String getInvalidPDFReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getLogFileByURL(getInvalidPDFReportUri(job));
    }

    private PDFValidationStatistics getValidationStatistics(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getValidationStatistics(job, getInvalidPDFReportUri(job), client.getValidPDFReportUri(job));
    }

    private PDFValidationStatistics getValidationStatistics(String job, String invalidReport, String validReport) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        int numberOfInvalidPDFs = 0, numberOfValidPDFs = 0;
        String invalidPDFReport = client.getLogFileByURL(invalidReport);
        try {
            numberOfValidPDFs = getNumberOfLines(client.getLogFileByURL(validReport));
            numberOfInvalidPDFs = getNumberOfLines(invalidPDFReport);
        }
        catch (IOException e) {
            return new PDFValidationStatistics(0,0, invalidReport);
        }
        PDFValidationStatistics result = new PDFValidationStatistics(numberOfInvalidPDFs, numberOfValidPDFs, invalidReport);
        result.invalidPDFReport = invalidPDFReport;
        return result;
    }

    private String getODFReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getLogFileByURL(getODFReportUri(job));
    }

    private Integer getODFFileCount(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getNumberOfLines(getODFReport(job));
    }

    private String getOfficeReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getLogFileByURL(getOfficeReportUri(job));
    }

    private String getOfficeReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt", "OfficeReport.txt");
    }

    private Integer getOfficeFileCount(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return getNumberOfLines(getOfficeReport(job));
    }

    private String getODFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt", "ODFReport.txt");
    }

    private Integer getNumberOfLines(String report) {
        Scanner sc = new Scanner(report);
        Integer result = 0;
        while(sc.hasNext()) {
            sc.nextLine();
            result++;
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
}

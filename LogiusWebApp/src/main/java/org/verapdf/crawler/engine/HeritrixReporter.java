package org.verapdf.crawler.engine;

import org.jopendocument.dom.OOUtils;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.verapdf.crawler.api.PDFValidationStatistics;
import org.verapdf.crawler.api.SingleURLJobReport;
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

    public File buildODSReport(String job) throws IOException, KeyManagementException, NoSuchAlgorithmException, ParserConfigurationException, SAXException {
        SingleURLJobReport reportData = getReport(job);

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
        setLinesInSheet(spreadSheet.getSheet(1), reportData.officeReport);
        setLinesInSheet(spreadSheet.getSheet(2), reportData.getPdfStatistics().invalidPDFReport);

        File ODSReport = new File("src/main/resources/report.ods");
        OOUtils.open(spreadSheet.saveAs(ODSReport));
        return ODSReport;
    }

    public String buildHtmlReport(String job) throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        SingleURLJobReport reportData = getReport(job);
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

    public boolean isJobFinished(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException, ParserConfigurationException, SAXException {
        return client.getCurrentJobStatus(job).startsWith("Finished");
    }

    private String getInvalidPDFReportUri(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getValidPDFReportUri(job).replace("Valid_PDF_Report.txt","Invalid_PDF_Report.txt");
    }

    private String getInvalidPDFReport(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return client.getLogFileByURL(getInvalidPDFReportUri(job));
    }

    private PDFValidationStatistics getValidationStatistics(String job) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        int numberOfInvalidPDFs = 0, numberOfValidPDFs = 0;
        String invalidPDFReport = client.getLogFileByURL(getInvalidPDFReportUri(job));
        try {
            numberOfValidPDFs = getNumberOfLines(client.getLogFileByURL(client.getValidPDFReportUri(job)));
            numberOfInvalidPDFs = getNumberOfLines(invalidPDFReport);
        }
        catch (IOException e) {
            return new PDFValidationStatistics(0,0, getInvalidPDFReportUri(job));
        }
        PDFValidationStatistics result = new PDFValidationStatistics(numberOfInvalidPDFs, numberOfValidPDFs, getInvalidPDFReportUri(job));
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

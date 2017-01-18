package org.verapdf.crawler.report;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verapdf.crawler.api.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.engine.HttpClientStub;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class TestHeritrixReporter {
    private static SingleURLJobReport report;
    private static HeritrixReporter reporter;

    @BeforeClass
    public static void initializeReport() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParserConfigurationException, SAXException {
        HeritrixClient client = new HeritrixClient("https://localhost:8443/",8443, "admin", "admin");
        client.setHttpClient(new HttpClientStub());
        ArrayList<String> list = new ArrayList<>();
        list.add("http://localhost:8080");
        client.createJob("report", list);
        reporter = new HeritrixReporter(client);
        report = reporter.getReport("report");
        }

    @Test
    public void testODSReport() {
        Assert.assertEquals("report",report.getId());
        Assert.assertEquals("http://localhost:8080", report.getUrl());
        Assert.assertEquals("Unbuilt", report.getStatus());
        Assert.assertEquals(6, report.getNumberOfCrawledUrls());
        Assert.assertEquals(2, report.getNumberOfODFDocuments());
        Assert.assertEquals(4, report.getNumberOfOfficeDocuments());
        Assert.assertEquals("file.doc\nfile.xlsx\nfile.ppt\nfile.docx\n", report.officeReport);
        Assert.assertEquals("https://localhost:8443/engine/anypath/jobs/test/20161224163617/mirror/OfficeReport.txt", report.getOfficeReportURL());
        Assert.assertEquals(3, report.getPdfStatistics().getNumberOfInvalidPDFs());
        Assert.assertEquals(5, report.getPdfStatistics().getNumberOfValidPDFs());
        Assert.assertEquals("invalidfile1.pdf\ninvalidfile2.pdf\ninvalidfile3.pdf\n", report.getPdfStatistics().invalidPDFReport);
        Assert.assertEquals("https://localhost:8443/engine/anypath/jobs/test/20161224163617/mirror/Invalid_PDF_Report.txt",report.getPdfStatistics().getInvalidPDFReportURL());
    }

    @Test
    public void testHtmlReport() throws IOException, SAXException, NoSuchAlgorithmException, ParserConfigurationException, KeyManagementException {
        String htmlReport = reporter.buildHtmlReport(report);
        String correctValue = "<p>Valid PDF files 5</p><p>ODF files 2</p><p><font color=\"green\">Total 7</font></p><p>Invalid PDF files 3</p><p>Microsoft Office files 4</p><p><font color=\"red\">Total 7</font></p><p><a href=\"INVALID_PDF_REPORT\">Invalid PDF URLs</a></p><p><a href=\"OFFICE_REPORT\">Microsoft Office files URLs</a></p>";
        Assert.assertEquals(correctValue, htmlReport);
    }

    @Test
    public void testGetReport() throws KeyManagementException, NoSuchAlgorithmException, SAXException, ParserConfigurationException, IOException {
        File ODSReport = reporter.buildODSReport(report);
        Sheet sheet1 = SpreadSheet.createFromFile(ODSReport).getSheet(0);
        Assert.assertEquals("Valid PDF files", sheet1.getValueAt(0, 0));
        Assert.assertEquals("Invalid PDF files", sheet1.getValueAt(0, 3));
        Assert.assertEquals(new BigDecimal(7), sheet1.getValueAt(1, 2));
        Assert.assertEquals(new BigDecimal(7), sheet1.getValueAt(1, 5));
        Assert.assertEquals(new BigDecimal(5), sheet1.getValueAt(1, 0));
        Assert.assertEquals(new BigDecimal(4), sheet1.getValueAt(1, 4));
        Sheet sheet2 = SpreadSheet.createFromFile(ODSReport).getSheet(1);
        Assert.assertEquals("file.docx", sheet2.getValueAt(0, 3));
        Sheet sheet3 = SpreadSheet.createFromFile(ODSReport).getSheet(2);
        Assert.assertEquals("invalidfile3.pdf", sheet3.getValueAt(0, 2));
    }
}

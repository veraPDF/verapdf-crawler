package org.verapdf.crawler.core.heritrix;

import org.jopendocument.dom.spreadsheet.Sheet;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.report.CrawlJobSummary;
import org.verapdf.crawler.db.document.ReportDocumentDao;
import org.verapdf.crawler.db.jobs.CrawlJobDao;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class HeritrixReporter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private final HeritrixClient client;
    private final ReportDocumentDao reportDocumentDao;
    private final CrawlJobDao crawlJobDao;

    public HeritrixReporter(HeritrixClient client, ReportDocumentDao reportDocumentDao, CrawlJobDao crawlJobDao) {
        this.client = client;
        this.crawlJobDao = crawlJobDao;
        this.reportDocumentDao = reportDocumentDao;
    }

    public String getCrawlJobStatus(String job) throws ParserConfigurationException, SAXException, IOException {
        return client.getCurrentJobStatus(job).replaceAll("\\s+","");
    }

    // If time is null settings were not provided
    public CrawlJobSummary getReport(String job, Date time) throws IOException, ParserConfigurationException, SAXException {
        CrawlJobSummary result = new CrawlJobSummary(job,
                client.getListOfCrawlUrls(job).get(0),
                client.getCurrentJobStatus(job).replaceAll("\\s+",""),
                client.getDownloadedCount(job));
        setFields(result, job, time);
        return result;
    }

    public CrawlJobSummary getReport(String job, String jobURL, Date time) throws IOException {
        String config = client.getConfig(jobURL);
        CrawlJobSummary result = new CrawlJobSummary(job,
                HeritrixClient.getListOfCrawlUrlsFromXml(config).get(0),
                "finished",0);
        setFields(result, job, time);
        return result;
    }

    private void setFields(CrawlJobSummary report, String job, Date time) {
        //report.setPdfStatistics(reportDocumentDao.getValidationStatistics(job, time));
        report.setNumberOfODFDocuments(reportDocumentDao.getNumberOfOdfFilesForJob(report.getDomain(), time));
        report.setNumberOfOfficeDocuments(reportDocumentDao.getNumberOfMicrosoftFilesForJob(report.getDomain(), time));
        report.setNumberOfOoxmlDocuments(reportDocumentDao.getNumberOfOoxmlFilesForJob(report.getDomain(), time));
        CrawlJob crawlJob = crawlJobDao.getCrawlJob(job);
        report.setStartTime(crawlJob.getStartTime());
        if(crawlJob.getFinishTime() != null) {
            report.setFinishTime(crawlJob.getFinishTime());
        }
    }

//    private File buildODSReport(CrawlJobSummary reportData, Date time) throws IOException {
//        File file = new File(HeritrixClient.baseDirectory + "sample_report.ods");
//        final Sheet totalSheet = SpreadSheet.createFromFile(file).getSheet(0);
//        totalSheet.ensureColumnCount(2);
//        if(time != null) {
//            totalSheet.setValueAt(time.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")) + " GMT", 1, 0);
//        }
//        else {
//            totalSheet.setValueAt("", 0, 0);
//        }
//        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPdfDocuments(),1, 1);
//        totalSheet.setValueAt(reportData.getNumberOfODFDocuments(),1, 2);
//        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfValidPdfDocuments() +
//                reportData.getNumberOfODFDocuments(), 1, 3);
//        totalSheet.setValueAt(reportData.getPdfStatistics().getNumberOfInvalidPdfDocuments(),1, 4);
//        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments(),1, 5);
//        totalSheet.setValueAt(reportData.getNumberOfOoxmlDocuments(),1, 6);
//        totalSheet.setValueAt(reportData.getNumberOfOfficeDocuments() +
//                reportData.getPdfStatistics().getNumberOfInvalidPdfDocuments() +
//                reportData.getNumberOfOoxmlDocuments(), 1, 7);
//
//        SpreadSheet spreadSheet = totalSheet.getSpreadSheet();
//        setStringsInSheet(spreadSheet.getSheet(1), reportDocumentDao.getMicrosoftOfficeFiles(reportData.getDomain(), time));
//        setStringsInSheet(spreadSheet.getSheet(2), reportDocumentDao.getInvalidPdfFiles(reportData.getDomain(), time));
//        setStringsInSheet(spreadSheet.getSheet(3), reportDocumentDao.getOoxmlFiles(reportData.getDomain(), time));
//
//        File ODSReport = new File(HeritrixClient.baseDirectory + "report.ods");
//        totalSheet.getSpreadSheet().saveAs(ODSReport);
//        return ODSReport;
//    }

//    public File buildODSReport(String job, Date time) throws IOException, ParserConfigurationException, SAXException {
//        CrawlJobSummary reportData = getReport(job, time);
//        return buildODSReport(reportData, time);
//    }

//    public File buildODSReport(String job, String jobURL, Date time) throws IOException {
//        CrawlJobSummary reportData = getReport(job, jobURL, time);
//        return buildODSReport(reportData, time);
//    }

    public List<String> getOfficeReport(String domain, Date time) {
        return reportDocumentDao.getMicrosoftOfficeFiles(domain, time);
    }

    public List<String> getOoxmlReport(String domain, Date time) {
        return reportDocumentDao.getOoxmlFiles(domain, time);
    }

    public List<String> getInvalidPdfReport(String domain, Date time) {
        return reportDocumentDao.getInvalidPdfFiles(domain, time);
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
}

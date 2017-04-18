package org.verapdf.crawler.resources;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.report.SingleURLJobReport;
import org.verapdf.crawler.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.validation.ValidationService;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ResourceManager {
    private static Logger logger = LoggerFactory.getLogger("CustomLogger");
    private InfoResourse infoResourse;
    private ReportResource reportResource;
    private ControlResource controlResource;
    private ArrayList<CurrentJob> currentJobs;
    private ArrayList<BatchJob> batchJobs;

    private String resourceUri;
    private ValidationService validationService;
    private EmailServer emailServer;

    public ResourceManager(HeritrixClient client, EmailServer emailServer, String verapdfPath) {

        currentJobs = new ArrayList<>();
        batchJobs = new ArrayList<>();
        HeritrixReporter reporter = new HeritrixReporter(client);
        this.emailServer = emailServer;

        validationService = new ValidationService(verapdfPath, client.getBaseDirectory(), this);
        infoResourse = new InfoResourse(validationService, client, currentJobs, this);
        reportResource = new ReportResource(reporter, currentJobs, this);
        controlResource = new ControlResource(currentJobs, client, reporter, emailServer, batchJobs, validationService, this);
        try {
            loadJobs();
        }
        catch (IOException e) {
            logger.error("Error on loading jobs", e);
        }
        new Thread(new StatusMonitor(this)).start();
        validationService.setRunning(true);
        new Thread(validationService).start();
        logger.info("Validation service started.");
    }

    public InfoResourse getInfoResourse() {
        return infoResourse;
    }

    public ReportResource getReportResource() {
        return reportResource;
    }

    public ControlResource getControlResource() {
        return controlResource;
    }

    public CurrentJob getJobById(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData;
        }
        return null;
    }

    private void loadJobs() throws IOException {
        FileReader reader = new FileReader(HeritrixClient.baseDirectory + "crawled_urls.txt");
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
        List<CSVRecord> records = parser.getRecords();
        for(CSVRecord record : records) {
            String startTimeString = record.get("startTime");
            String finishTimeString = record.get("finishTime");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(startTimeString, formatter);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            CurrentJob newJob = new CurrentJob(record.get("id"),
                    record.get("jobURL"),
                    record.get("crawlURL"),
                    LocalDateTime.of(LocalDate.parse(record.get("crawlSince"), dateFormatter), LocalTime.MIN),
                    "",startTime);
            if(!finishTimeString.equals("")) {
                newJob.setFinishTime(LocalDateTime.parse(finishTimeString, formatter));
            }
            currentJobs.add(newJob);
        }
    }

    public ArrayList<BatchJob> getBatchJobs() { return batchJobs; }

    public CurrentJob getJobByCrawlUrl(String crawlUrl) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getCrawlURL().equals(crawlUrl))
                return jobData;
        }
        return null;
    }

    public SingleURLJobReport getJob(String job) throws IOException, SAXException, NoSuchAlgorithmException, ParserConfigurationException, KeyManagementException { return controlResource.getJob(job); }

    public String getResourceUri() { return resourceUri; }

    public void setResourceUri(String resourceUri) { this.resourceUri = resourceUri; }

    public EmailServer getEmailServer() { return emailServer; }

    public ArrayList<CurrentJob> getCurrentJobs() { return currentJobs; }

    public String getExistingJobURLbyJobId(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData.getJobURL();
        }
        return "";
    }

    public LocalDateTime getTimeByJobId(String job) {
        for(CurrentJob jobData : currentJobs) {
            if(jobData.getId().equals(job))
                return jobData.getCrawlSinceTime();
        }
        return null;
    }
}

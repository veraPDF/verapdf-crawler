package org.verapdf.crawler.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.ValidationReportData;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.file.InsertFileDao;
import org.verapdf.crawler.repository.jobs.ValidationJobDao;

import javax.sql.DataSource;
import java.io.*;
import java.util.LinkedList;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final LinkedList<ValidationJobData> queue;
    private final PDFValidator validator;
    private final ValidationJobDao validationJobDao;
    private final InsertFileDao insertFileDao;

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
    }

    private boolean isRunning;
    public ValidationService(String verapdfPath, DataSource dataSource) {
        this.queue = new LinkedList<>();
        validationJobDao = new ValidationJobDao(dataSource);
        insertFileDao = new InsertFileDao(dataSource);
        isRunning = true;
        validator = new VerapdfValidator(verapdfPath);
        queue.addAll(validationJobDao.getAllJobs());
    }

    public void addJob(ValidationJobData data) throws IOException {
        queue.add(data);
        validationJobDao.addJob(data);
        logger.info("Added validation job " + data.getUri());
    }

    public Integer getQueueSize() {
        return queue.size();
    }

    @Override
    public void run() {
        while (isRunning) {
            ValidationJobData data = new ValidationJobData();
            try {
                if(!queue.isEmpty()) {
                    data = queue.remove();
                    validationJobDao.deleteJob(data);
                    logger.info("Validating " + data.getUri());
                    ValidationReportData result;
                    try {
                        result = validator.validateAndWirteErrors(data.getFilepath(), data.errorOccurances);
                    }
                    catch (Exception e) {
                        result = new ValidationReportData();
                        result.setValid(false);
                        result.setFailedRules(0);
                        result.setPassedRules(0);
                    }
                    String[] parts = data.getJobDirectory().split("/");
                    String jobId = parts[parts.length - 3];
                    if(result.isValid()) {
                        insertFileDao.addValidPdfFile(data, jobId);
                    }
                    else {
                        result.setUrl(data.getUri());
                        result.setLastModified(data.getTime());
                        insertFileDao.addInvalidPdfFile(result, jobId);
                    }
                }
                else {
                    Thread.sleep(60000);
                }
            } catch (Exception e) {
                logger.error("Error in validation runner",e);
            }
            finally {
                if(data != null && data.getFilepath() != null) {
                    new File(data.getFilepath()).delete();
                }
            }
        }
    }
}
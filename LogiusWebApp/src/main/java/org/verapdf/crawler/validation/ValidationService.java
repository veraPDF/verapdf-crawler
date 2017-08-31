package org.verapdf.crawler.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.jobs.ValidationJobDao;

import javax.sql.DataSource;
import java.io.*;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final PDFValidator validator;
    private final ValidationJobDao validationJobDao;
    private final InsertDocumentDao insertDocumentDao;

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
    }

    private boolean isRunning;
    public ValidationService(DataSource dataSource, PDFValidator validator) {
        validationJobDao = new ValidationJobDao(dataSource);
        insertDocumentDao = new InsertDocumentDao(dataSource);
        isRunning = true;
        this.validator = validator;
    }

    public void addJob(ValidationJobData data) throws IOException {
        validationJobDao.addJob(data);
        logger.info("Added validation job " + data.getUri());
    }

    public Integer getQueueSize() {
        return validationJobDao.getQueueSize();
    }

    @Override
    public void run() {
        while (isRunning) {
            ValidationJobData data = validationJobDao.getOneJob();
            if(data != null) {
                try {
                    logger.info("Validating " + data.getUri());
                    boolean validationResult;
                    try {
                        validationResult = validator.validateAndWirteResult(data.getFilepath(), data.getUri());
                    } catch (Exception e) {
                        logger.error("Error in validator", e);
                        validationResult = false;
                    }
                    String[] parts = data.getJobDirectory().split("/");
                    String jobId = parts[parts.length - 3];
                    if (validationResult) {
                        insertDocumentDao.addPdfFile(data, jobId);
                    } else {
                        insertDocumentDao.addInvalidPdfFile(data, jobId);
                    }
                } catch (Exception e) {
                    logger.error("Error in validation runner", e);
                } finally {
                    if (data.getFilepath() != null) {
                        new File(data.getFilepath()).delete();
                        validationJobDao.deleteJob(data);
                    }
                }
            }
            else {
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
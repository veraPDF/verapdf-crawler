package org.verapdf.crawler.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;
import org.verapdf.crawler.repository.jobs.ValidationJobDao;

import javax.sql.DataSource;
import java.io.*;
import java.util.List;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final PDFValidator validator;
    private final ValidationJobDao validationJobDao;
    private final InsertDocumentDao insertDocumentDao;
    private final ValidatedPDFDao validatedPDFDao;
    private List<ValidationJobData> validationJobs;

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
    }

    private boolean isRunning;
    public ValidationService(String verapdfUrl, DataSource dataSource, ValidatedPDFDao validatedPDFDao) {
        validationJobDao = new ValidationJobDao(dataSource);
        insertDocumentDao = new InsertDocumentDao(dataSource);
        isRunning = true;
        this.validatedPDFDao = validatedPDFDao;
        validator = new VerapdfServiceValidator(verapdfUrl);
    }

    public void addJob(ValidationJobData data) throws IOException {
        validationJobDao.addJob(data);
        logger.info("Added validation job " + data.getUri());
    }

    public Integer getQueueSize() {
        return validationJobDao.getQueueSize() + validationJobs.size();
    }

    @Override
    public void run() {
        while (isRunning) {
            validationJobs = validationJobDao.removeAll();
            if(validationJobs != null && !validationJobs.isEmpty()) {
                for(ValidationJobData data: validationJobs) {
                    try {
                        logger.info("Validating " + data.getUri());
                        boolean validationResult;
                        try {
                                validationResult = validator.validateAndWirteResult(data.getFilepath(), data.getUri(), validatedPDFDao);
                            } catch (Exception e) {
                                logger.error("Error in validator",e);
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
                        logger.error("Error in validation runner",e);
                    }
                    finally {
                        if(data != null && data.getFilepath() != null) {
                            new File(data.getFilepath()).delete();
                        }
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
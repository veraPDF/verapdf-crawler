package org.verapdf.crawler.core.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.db.jobs.ValidationJobDao;

import javax.sql.DataSource;
import java.io.*;

public class ValidationService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final PDFValidator validator;
    private final ValidationJobDao validationJobDao;
    private boolean running;

    public ValidationService(DataSource dataSource, PDFValidator validator) {
        validationJobDao = new ValidationJobDao(dataSource);
        running = false;
        this.validator = validator;
    }

    public void start() {
        running = true;
        new Thread(this).start();
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
        while (running) {
            ValidationJobData data = validationJobDao.getOneJob();
            if(data != null) {
                try {
                    logger.info("Validating " + data.getUri());
                    try {
                        validator.validateAndWriteResult(data);
                    } catch (Exception e) {
                        logger.error("Error in validator", e);
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
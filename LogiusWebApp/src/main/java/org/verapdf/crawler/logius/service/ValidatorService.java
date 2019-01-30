package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.PDFValidator;
import org.verapdf.crawler.logius.core.validation.ValidationDeadlockException;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorService.class);
    private final ValidationJobService validationJobService;
    private final PDFValidator validator;
    private final List<PDFProcessorAdapter> pdfProcessors;
    public ValidatorService(ValidationJobService validationJobService, PDFValidator validator,
                            List<PDFProcessorAdapter> pdfProcessors) {
        this.validationJobService = validationJobService;
        this.validator = validator;
        this.pdfProcessors = pdfProcessors;
    }

    public void abortCurrentJob() {
        try {
            logger.info("Aborting current job");
            validationJobService.getCurrentJob().setStatus(ValidationJob.Status.ABORTED);
            validator.terminateValidation();
        } catch (IOException e) {
            logger.error("Can't terminate current job", e);
        }
    }


    public void processCurrentJob() throws InterruptedException, ValidationDeadlockException {
        if (validationJobService.retrieveCurrentJob() != null) {
            try {
                processStartedJob();
            } catch (IOException e) {
                saveErrorResult(e);
            }
        }
    }

    public boolean processNextJob() throws InterruptedException, ValidationDeadlockException {
        if (validationJobService.retrieveNextJob() != null) {
            logger.info("Validating " + validationJobService.getCurrentJob().getId());
            try {
                validator.startValidation(validationJobService.getCurrentJob());
                processStartedJob();
            } catch (IOException e) {
                saveErrorResult(e);
            }
            return false;
        }
        return true;
    }


    private void processStartedJob() throws IOException, ValidationDeadlockException, InterruptedException {
        VeraPDFValidationResult result = validator.getValidationResult(validationJobService.getCurrentJob());
        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
            Map<String, String> properties = pdfProcessor.evaluateProperties(validationJobService.getCurrentJob());
            for (Map.Entry<String, String> property : properties.entrySet()) {
                result.addProperty(property.getKey(), property.getValue());
            }
        }
        validationJobService.saveResult(result);
    }

    private void saveErrorResult(Throwable e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e.getMessage());
        validationJobService.saveResult(result);
    }
}

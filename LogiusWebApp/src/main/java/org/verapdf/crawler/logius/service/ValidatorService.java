package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.PDFValidator;
import org.verapdf.crawler.logius.core.validation.ValidationDeadlockException;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorService.class);
    private final ValidationJobService validationJobService;
    private final PDFValidator validator;
    private final List<PDFProcessorAdapter> pdfProcessors;
    private final FileService fileService;

    public ValidatorService(ValidationJobService validationJobService, PDFValidator validator,
                            List<PDFProcessorAdapter> pdfProcessors, FileService fileService) {
        this.validationJobService = validationJobService;
        this.validator = validator;
        this.pdfProcessors = pdfProcessors;
        this.fileService = fileService;
    }

    @PostConstruct
    public void init() {
        validationJobService.clean();
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


    public boolean processNextJob() throws InterruptedException, ValidationDeadlockException {
        if (validationJobService.retrieveNextJob() != null) {
            ValidationJob validationJob = validationJobService.getCurrentJob();
            logger.info("Validating " + validationJob.getId());
            File file = null;
            try {
                file = fileService.save(validationJob.getDocument().getUrl());
                if (file != null){
                    boolean isValidationDisabled = validationJob.getDocument().getCrawlJob().isValidationDisabled();
                    validator.startValidation(file, isValidationDisabled);
                    processStartedJob(file, isValidationDisabled);
                }else {
                    saveErrorResult("Can't create url: " + validationJob.getId());
                }
            } catch (IOException e) {
                saveErrorResult(e);
            } finally {
                fileService.removeFile(file);
            }
            return false;
        }
        return true;
    }


    private void processStartedJob(File file, boolean isValidationDisabled) throws IOException, ValidationDeadlockException, InterruptedException {
        VeraPDFValidationResult result = validator.getValidationResult(file, isValidationDisabled);
        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
            Map<String, String> properties = pdfProcessor.evaluateProperties(file.getPath());
            for (Map.Entry<String, String> property : properties.entrySet()) {
                result.addProperty(property.getKey(), property.getValue());
            }
        }
        validationJobService.saveResult(result);
    }

    private void saveErrorResult(String e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e);
        validationJobService.saveResult(result);
    }

    private void saveErrorResult(Throwable e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e.getMessage());
        validationJobService.saveResult(result);
    }
}

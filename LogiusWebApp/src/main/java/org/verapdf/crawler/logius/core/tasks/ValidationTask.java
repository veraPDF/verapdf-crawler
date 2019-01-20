package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.PDFValidator;
import org.verapdf.crawler.logius.core.validation.ValidationDeadlockException;
import org.verapdf.crawler.logius.service.ValidationService;
import org.verapdf.crawler.logius.tools.AbstractService;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ValidationTask extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationTask.class);
    private static final long SLEEP_DURATION = 60 * 1000;

    private final PDFValidator validator;
    private final List<PDFProcessorAdapter> pdfProcessors;
    private final ValidationService validationService;

    public ValidationTask(PDFValidator validator, List<PDFProcessorAdapter> pdfProcessors,
                          ValidationService validationService) {
        super("ValidationService", SLEEP_DURATION);
        this.validator = validator;
        this.pdfProcessors = pdfProcessors;
        this.validationService = validationService;
    }


    @Override
    protected void onStart() throws InterruptedException, ValidationDeadlockException {
        if (validationService.retrieveCurrentJob() != null) {
            try {
                processStartedJob();
            } catch (IOException e) {
                saveErrorResult(e);
            }
        }
    }

    @Override
    protected boolean onRepeat() throws ValidationDeadlockException, InterruptedException {
        if (validationService.retrieveNextJob() != null) {
            logger.info("Validating " + validationService.getCurrentJob().getId());
            try {
                validator.startValidation(validationService.getCurrentJob());
                processStartedJob();
            } catch (IOException e) {
                saveErrorResult(e);
            }
            return false;
        }
        return true;
    }

    private void processStartedJob() throws IOException, ValidationDeadlockException, InterruptedException {
        VeraPDFValidationResult result = validator.getValidationResult(validationService.getCurrentJob());
        // additional processors logic
        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
            Map<String, String> properties = pdfProcessor.evaluateProperties(validationService.getCurrentJob());
            for (Map.Entry<String, String> property : properties.entrySet()) {
                result.addProperty(property.getKey(), property.getValue());
            }
        }
        validationService.saveResult(result);
    }

    private void saveErrorResult(Throwable e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e.getMessage());
        validationService.saveResult(result);
    }
}
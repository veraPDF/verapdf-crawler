package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.VeraPDFProcessor;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import java.util.List;
import java.util.Map;

@Service
public class ValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorService.class);
    private final ValidationJobService validationJobService;
    private final List<PDFProcessorAdapter> pdfProcessors;
    private final FileService fileService;
    private final ValidationSettingsService validationSettingsService;
    private final VeraPDFProcessor veraPDFProcessor;

    public ValidatorService(ValidationJobService validationJobService, List<PDFProcessorAdapter> pdfProcessors,
                            FileService fileService, ValidationSettingsService validationSettingsService,
                            VeraPDFProcessor veraPDFProcessor) {
        this.validationJobService = validationJobService;
        this.pdfProcessors = pdfProcessors;
        this.fileService = fileService;
        this.veraPDFProcessor = veraPDFProcessor;
        this.validationSettingsService = validationSettingsService;
    }

    public void processJob(ValidationJob validationJob) {
        logger.info("Validating " + validationJob.getId());

        fileService.save(validationJob);
        if (validationJob.getFilePath() == null){
            validationJobService.cleanJob(validationJob, true);
        }
        VeraPDFValidationResult result = this.getVeraPDFValidationResult(validationJob.getFilePath());
        if (result != null){
            addProperties(result, validationJob.getFilePath());
            validationJobService.saveResult(result, validationJob);
        }else {
            validationJobService.cleanJob(validationJob, true);
        }
    }

    private void addProperties(VeraPDFValidationResult result, String filepath) {
        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
            Map<String, String> properties = pdfProcessor.evaluateProperties(filepath);
            for (Map.Entry<String, String> property : properties.entrySet()) {
                result.addProperty(property.getKey(), property.getValue());
            }
        }
    }

    private VeraPDFValidationResult getVeraPDFValidationResult(String filepath) {
        logger.info("Sending file " + filepath + " to validator");
        return veraPDFProcessor.process(filepath, validationSettingsService.getValidationSettings());
    }
}

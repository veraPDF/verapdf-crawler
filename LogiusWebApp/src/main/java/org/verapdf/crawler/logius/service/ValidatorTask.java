package org.verapdf.crawler.logius.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.VeraPDFProcessor;
import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ValidatorTask implements Callable<VeraPDFValidationResult> {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorTask.class);
    private final List<PDFProcessorAdapter> pdfProcessors;
    private final FileService fileService;
    private final ValidationSettingsService validationSettingsService;
    private ObjectFactory<VeraPDFProcessor> veraPDFProcessorObjectFactory;
    private VeraPDFProcessor veraPDFProcessor;
    private ValidationJob job;

    public ValidatorTask(List<PDFProcessorAdapter> pdfProcessors, FileService fileService,
                         ObjectFactory<VeraPDFProcessor> veraPDFProcessorObjectFactory,
                         ValidationSettingsService validationSettingsService) {
        this.pdfProcessors = pdfProcessors;
        this.fileService = fileService;
        this.veraPDFProcessorObjectFactory = veraPDFProcessorObjectFactory;
        this.validationSettingsService = validationSettingsService;
    }

    public ValidationJob getValidationJob() {
        return job;
    }

    public void setValidationJob(ValidationJob validationJob) {
        this.job = validationJob;
    }

    public void abortCurrentJob() {
        logger.info("Terminating current job");
        job.setStatus(ValidationJob.Status.ABORTED);
        if (this.veraPDFProcessor != null) {
            this.veraPDFProcessor.stopProcess();
            this.veraPDFProcessor = null;
        }
    }

    public VeraPDFValidationResult call() {
        if (job != null) {
            logger.info("Validating " + job.getDocumentUrl());
            File file = null;
            try {
                file = fileService.save(job.getDocumentUrl());
                if (file == null) {
                    return saveErrorResult("Can't create url: " + job.getDocumentUrl());
                }
                return processJob(file, job);

            } finally {
                fileService.removeFile(file);
            }
        }
        return null;
    }


    private VeraPDFValidationResult processJob(File file, ValidationJob job) {
        veraPDFProcessor = veraPDFProcessorObjectFactory.getObject();
        veraPDFProcessor.setFilePath(file);
        veraPDFProcessor.setSettings(validationSettingsService.getValidationSettings());
        veraPDFProcessor.setValidationDisabled(job.getDocument().getDocumentId().getCrawlJob().isValidationDisabled());
        VeraPDFValidationResult result = veraPDFProcessor.call();

        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
            Map<String, String> properties = pdfProcessor.evaluateProperties(file.getPath());
            for (Map.Entry<String, String> property : properties.entrySet()) {
                result.addProperty(property.getKey(), property.getValue());
            }
        }
        return result;
    }

    private VeraPDFValidationResult saveErrorResult(String e) {
        return new VeraPDFValidationResult(e);
    }
}


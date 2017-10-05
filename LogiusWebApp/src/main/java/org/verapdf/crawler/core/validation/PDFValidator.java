package org.verapdf.crawler.core.validation;

import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;

import java.io.IOException;

public interface PDFValidator {
    void startValidation(ValidationJob job) throws IOException, ValidationDeadlockException;

    VeraPDFValidationResult getValidationResult(ValidationJob job) throws IOException, ValidationDeadlockException, InterruptedException;

    void terminateValidation() throws IOException;
}

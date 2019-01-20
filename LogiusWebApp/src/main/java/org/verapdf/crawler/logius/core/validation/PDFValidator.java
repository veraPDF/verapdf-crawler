package org.verapdf.crawler.logius.core.validation;

import org.verapdf.crawler.logius.validation.ValidationJob;
import org.verapdf.crawler.logius.validation.VeraPDFValidationResult;

import java.io.IOException;

public interface PDFValidator {
    void startValidation(ValidationJob job) throws IOException, ValidationDeadlockException;

    VeraPDFValidationResult getValidationResult(ValidationJob job) throws IOException, ValidationDeadlockException, InterruptedException;

    void terminateValidation() throws IOException;
}

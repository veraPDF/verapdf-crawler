package com.verapdf.crawler.logius.app.core.validation;

import com.verapdf.crawler.logius.app.validation.ValidationJob;
import com.verapdf.crawler.logius.app.validation.VeraPDFValidationResult;

import java.io.IOException;

public interface PDFValidator {
    void startValidation(ValidationJob job) throws IOException, ValidationDeadlockException;

    VeraPDFValidationResult getValidationResult(ValidationJob job) throws IOException, ValidationDeadlockException, InterruptedException;

    void terminateValidation() throws IOException;
}

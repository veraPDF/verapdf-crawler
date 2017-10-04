package org.verapdf.crawler.core.validation;

import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;

public interface PDFValidator {
    boolean startValidation(ValidationJob job);

    VeraPDFValidationResult getValidationResult(ValidationJob job) throws Throwable;

    void terminateValidation();
}

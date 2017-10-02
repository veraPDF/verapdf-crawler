package org.verapdf.crawler.core.validation;

import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;

public interface PDFValidator {
    VeraPDFValidationResult validate(ValidationJob data);
}

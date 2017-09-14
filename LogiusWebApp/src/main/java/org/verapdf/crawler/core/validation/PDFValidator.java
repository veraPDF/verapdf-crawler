package org.verapdf.crawler.core.validation;

import org.verapdf.crawler.api.validation.ValidationJobData;

public interface PDFValidator {
    void validateAndWriteResult(ValidationJobData data) throws Exception;
}

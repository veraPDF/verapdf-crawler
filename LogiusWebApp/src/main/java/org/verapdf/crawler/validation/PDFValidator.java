package org.verapdf.crawler.validation;

import org.verapdf.crawler.domain.validation.ValidationJobData;

public interface PDFValidator {
    void validateAndWriteResult(ValidationJobData data) throws Exception;
}

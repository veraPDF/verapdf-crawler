package org.verapdf.crawler.validation;

import org.verapdf.crawler.repository.document.ValidatedPDFDao;

public interface PDFValidator {
    boolean validateAndWirteResult(String localFilename, String fileUrl, ValidatedPDFDao validatedPDFDao) throws Exception;
}

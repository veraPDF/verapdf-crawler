package org.verapdf.crawler.validation;

public interface PDFValidator {
    boolean validateAndWirteResult(String localFilename, String fileUrl) throws Exception;
}

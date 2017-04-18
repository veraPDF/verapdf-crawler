package org.verapdf.crawler.validation;

import org.verapdf.crawler.domain.report.ValidationError;
import org.verapdf.crawler.domain.validation.ValidationReportData;

import java.util.Map;

public interface PDFValidator {

    ValidationReportData validate(String filename) throws Exception;

    ValidationReportData validateAndWirteErrors(String filename, Map<ValidationError, Integer> errorOccurances) throws Exception;
}

package org.verapdf.crawler.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.report.ValidationError;
import org.verapdf.crawler.domain.validation.ValidationReportData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.verapdf.processor.reports.Reports;
import org.verapdf.processor.reports.RuleSummary;
import org.verapdf.processor.reports.ValidationReport;

public class VerapdfValidator implements PDFValidator {
    private static Logger logger = LoggerFactory.getLogger("CustomLogger");
    private static final String VALIDATION_REPORT_HEAD = "validationReport";
    private static final String SUMMARY_HEAD = "summary";

    private String verapdfPath;

    public VerapdfValidator(String verapdfPath) {
        this.verapdfPath = verapdfPath;
    }

    @Override
    public ValidationReportData validate(String filename) throws Exception {
        ValidationReport validationReport = getValidationReportForFile(filename);
        return new ValidationReportData(validationReport);
    }

    @Override
    public ValidationReportData validateAndWirteErrors(String filename, Map<ValidationError, Integer> errorOccurances) throws Exception {
        ValidationReport validationReport = getValidationReportForFile(filename);
        String partRule;
        if(validationReport.getProfileName().contains("1")) {
            partRule = ValidationError.PART_ONE_RULE;
        }
        else {
            partRule = ValidationError.PART_TWO_THREE_RULE;
        }
        for(RuleSummary rule: validationReport.getDetails().getRuleSummaries()) {
            ValidationError error = new ValidationError(rule.getClause(), rule.getTestNumber(),
                    rule.getSpecification(), partRule, rule.getDescription());
            if(errorOccurances.containsKey(error)) {
                errorOccurances.put(error, errorOccurances.get(error) + 1);
            }
            else {
                errorOccurances.put(error, 1);
            }
        }
        return new ValidationReportData(validationReport);
    }

    private ValidationReport getValidationReportForFile(String filename) throws Exception {
        ValidationReport validationReport;
        String[] cmd = {verapdfPath, "--format", "mrr", filename};
        ProcessBuilder pb = new ProcessBuilder().inheritIO();
        File output = new File("output");
        File error = new File("error");
        output.createNewFile();
        error.createNewFile();
        pb.redirectOutput(output);
        pb.redirectError(error);
        pb.command(cmd);
        String mrrReport = new String(Files.readAllBytes(Paths.get("output")));
        if(pb.start().waitFor(20, TimeUnit.MINUTES) && !mrrReport.equals("")) { // Validation finished successfully in time
            String validationReportXml = getXMLObject(mrrReport, VALIDATION_REPORT_HEAD);
            String summaryXml = getXMLObject(mrrReport, SUMMARY_HEAD);
            validationReport = Reports.validationReportFromXml(validationReportXml);
        }
        else {
            if(mrrReport.equals("")) {
                logger.error("Verapdf output is empty.");
            }
            Scanner errorScanner = new Scanner(new File("error"));
            StringBuilder builder = new StringBuilder();
            while(errorScanner.hasNextLine()) {
                String line = errorScanner.nextLine();
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            throw new Exception(builder.toString());
        }
        return validationReport;
    }

    private String getXMLObject(String xml, String name) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                xml.substring(xml.indexOf("<" + name),
                xml.indexOf("</" + name + ">") + ("</" + name + ">").length());
    }
}

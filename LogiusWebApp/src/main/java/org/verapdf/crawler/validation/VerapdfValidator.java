package org.verapdf.crawler.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.verapdf.crawler.domain.validation.ValidationErrorWithdescription;
import org.verapdf.crawler.repository.document.ValidatedPDFDao;
import org.verapdf.processor.reports.Reports;
import org.verapdf.processor.reports.RuleSummary;
import org.verapdf.processor.reports.ValidationReport;

public class VerapdfValidator implements PDFValidator {
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private static final String VALIDATION_REPORT_HEAD = "validationReport";
    private static final String SUMMARY_HEAD = "summary";

    private final String verapdfPath;

    VerapdfValidator(String verapdfPath) {
        this.verapdfPath = verapdfPath;
    }

    @Override
    public boolean validateAndWirteResult(String filename, String fileUrl, ValidatedPDFDao validatedPDFDao) throws Exception {
        ValidationReport validationReport = getValidationReportForFile(filename);
        for(RuleSummary rule: validationReport.getDetails().getRuleSummaries()) {
            validatedPDFDao.addErrorToDocument(new ValidationErrorWithdescription(rule.getSpecification(), rule.getClause(),
                    Integer.toString(rule.getTestNumber()), rule.getDescription()), fileUrl);
        }
        return validationReport.getDetails().getFailedRules() == 0;
    }

    private ValidationReport getValidationReportForFile(String filename) throws Exception {
        ValidationReport validationReport;
        String[] cmd = {verapdfPath, "--format", "mrr", filename};
        ProcessBuilder pb = new ProcessBuilder().inheritIO();
        File output = new File("output");
        output.createNewFile();
        pb.redirectOutput(output);
        pb.command(cmd);
        if(pb.start().waitFor(20, TimeUnit.MINUTES)) { // Validation finished successfully in time
            String mrrReport = new String(Files.readAllBytes(Paths.get("output")));
            if(mrrReport.isEmpty()) {
                logger.info("Output is empty, waiting" + 0);
                for(int i = 0; i < 10; i++) {
                    Thread.sleep(100);
                    if(mrrReport.isEmpty()) {
                        logger.info("Output is empty, waiting " + (i + 1));
                        Thread.sleep(100);
                    }
                    else break;
                }
            }

            String validationReportXml = getXMLObject(mrrReport, VALIDATION_REPORT_HEAD);
            String summaryXml = getXMLObject(mrrReport, SUMMARY_HEAD);
            validationReport = Reports.validationReportFromXml(validationReportXml);
        }
        else {
            Scanner errorScanner = new Scanner(new File("output"));
            StringBuilder builder = new StringBuilder();
            while(errorScanner.hasNextLine()) {
                String line = errorScanner.nextLine();
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            new File("output").delete();
            throw new Exception(builder.toString());
        }
        new File("output").delete();
        return validationReport;
    }

    private String getXMLObject(String xml, String name) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                xml.substring(xml.indexOf("<" + name),
                        xml.indexOf("</" + name + ">") + ("</" + name + ">").length());
    }
}

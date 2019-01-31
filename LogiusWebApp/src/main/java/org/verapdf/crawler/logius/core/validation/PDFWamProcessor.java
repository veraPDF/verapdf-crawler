package org.verapdf.crawler.logius.core.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @author Maksim Bezrukov
 */

@Component
public class PDFWamProcessor extends PDFProcessorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PDFWamProcessor.class);

    private static Set<String> TESTS;
    private static Set<String> ERROR_PROPERTY_NAMES;

    static {
        String[] tempTests = new String[]{
                "egovmon.pdf.03",
                "egovmon.pdf.05",
                "egovmon.pdf.08",
                "wcag.pdf.01",
                "wcag.pdf.02",
                "wcag.pdf.03",
                "wcag.pdf.04",
                "wcag.pdf.06",
                "wcag.pdf.09",
                "wcag.pdf.12",
                "wcag.pdf.14",
                "wcag.pdf.15",
                "wcag.pdf.16",
                "wcag.pdf.17",
                "wcag.pdf.18",
                "wcag.pdf.sc244",
        };
        TESTS = new HashSet<>(tempTests.length);
        TESTS.addAll(Arrays.asList(tempTests));
        ERROR_PROPERTY_NAMES = new HashSet<>(TESTS);
        ERROR_PROPERTY_NAMES.add("pdfwam.error");
    }

    @Value("${logius.pdfProcessors.pdfwamChecker}")
    private String pdfwamPdfcheckerPath;


    public static Set<String> getErrorPropertyNames() {
        return ERROR_PROPERTY_NAMES;
    }

    @Override
    public Map<String, String> evaluateProperties(String filepath) {
        if (this.pdfwamPdfcheckerPath != null && Files.isRegularFile(Paths.get(this.pdfwamPdfcheckerPath))) {
            try (Scanner scanner = new Scanner(startProcess(filepath))) {
                return parseResult(scanner);
            } catch (InterruptedException | IOException e) {
                logger.error("Some error during pdfwam processing", e);
                return generateErrorResult();
            }
        }
        return Collections.emptyMap();
    }

    private InputStream startProcess(String filepath) throws IOException, InterruptedException {
        logger.info("Starting PDFWam process...");
        String[] cmd = {"python", this.pdfwamPdfcheckerPath, "-q", "-r", "-l", "ERROR", filepath};
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        Process process = pb.start();
        if (!process.waitFor(30, TimeUnit.MINUTES)) {
            logger.info("PDFWam process reached timeout. Destroying...");
            process.destroy();
        }
        return process.getInputStream();
    }

    private Map<String, String> parseResult(Scanner scanner) {
        Map<String, String> res = new HashMap<>();
        // skip all lines until ***Test Report*** has been read
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().startsWith("***Test Report***")) {
                break;
            }
        }

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] split = line.split("\\|");
            String test = split[0].trim().toLowerCase();
            if (split.length > 1 && TESTS.contains(test)) {
                res.put(test, split[1].trim().toLowerCase());
            }
        }
        if (res.isEmpty()) {
            res = generateErrorResult();
        }
        return res;
    }

    private Map<String, String> generateErrorResult() {
        Map<String, String> res = new HashMap<>();
        res.put("pdfwam.error", "fail");
        return res;
    }
}

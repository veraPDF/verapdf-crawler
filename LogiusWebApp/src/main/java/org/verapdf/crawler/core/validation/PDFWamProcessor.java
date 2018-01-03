package org.verapdf.crawler.core.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationJob;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * @author Maksim Bezrukov
 */
public class PDFWamProcessor extends PDFProcessorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(PDFWamProcessor.class);

	private static Set<String> TESTS;

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
	}

	private final String pdfwamPdfcheckerPath;

	public PDFWamProcessor(String pdfwamPdfcheckerPath) {
		this.pdfwamPdfcheckerPath = pdfwamPdfcheckerPath;
	}

	@Override
	public Map<String, String> evaluateProperties(ValidationJob job) {
		if (this.pdfwamPdfcheckerPath != null && Files.isRegularFile(Paths.get(this.pdfwamPdfcheckerPath))) {
			try (Scanner scanner = new Scanner(startProcess(job))) {
				return parseResult(scanner);
			} catch (InterruptedException | IOException e) {
				logger.error("Some error during pdfwam processing", e);
				return generateErrorResult();
			}
		}
		return super.evaluateProperties(job);
	}

	private InputStream startProcess(ValidationJob job) throws IOException, InterruptedException {
		String[] cmd = {"python", this.pdfwamPdfcheckerPath, "-q", "-r", "-l", "ERROR", job.getFilePath()};
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(cmd);
		Process process = pb.start();
		process.waitFor();
		return process.getInputStream();
	}

	private Map<String, String> parseResult(Scanner scanner) {
		Map<String, String> res = new HashMap<>();
		// skip all lines until ***Test Report*** has been read
		while(scanner.hasNextLine()) {
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

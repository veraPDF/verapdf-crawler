package org.verapdf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.ValidationError;
import org.verapdf.crawler.domain.validation.VeraPDFValidationResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
public class VeraPDFProcessor implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

	private static final String BASE_PATH = "/report/jobs/job/";
	private static final String VALIDATION_REPORT_PATH = BASE_PATH + "validationReport/";

	private final String verapdfPath;
	private final String filePath;
	private Process process;
	private ValidationResource resource;
	private boolean stopped = false;
	private final Map<String, String> propertiesPaths;

	VeraPDFProcessor(String verapdfPath, String filePath, ValidationResource resource, Map<String, String> propertiesPaths) {
		this.verapdfPath = verapdfPath;
		this.filePath = filePath;
		this.resource = resource;
		this.propertiesPaths = propertiesPaths;
	}

	private File getVeraPDFReport(String filename) throws IOException, InterruptedException {
		String[] cmd = {verapdfPath, "--extract", "--format", "mrr", "--maxfailuresdisplayed", "1", filename};
		ProcessBuilder pb = new ProcessBuilder().inheritIO();
		Path outputPath = Files.createTempFile("veraPDFReport", ".xml");
		File file = outputPath.toFile();
		if (!file.createNewFile()) {
			return null;
		}
		file.deleteOnExit();
		pb.redirectOutput(file);
		pb.command(cmd);
		this.process = pb.start();
		this.process.waitFor();
		return file;
	}

	@Override
	public void run() {
		VeraPDFValidationResult result;
		File report = null;
		try {
			report = getVeraPDFReport(this.filePath);
			if (report != null) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.parse(report);
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				// Uncomment this for specifying namespaces if it will be necessary
//		SimpleNamespaceContext nsc = new SimpleNamespaceContext();
//		nsc.setPrefix(SchematronGenerator.SCH_PREFIX, SchematronGenerator.SCH_NAMESPACE);
//		xpath.setNamespaceContext(nsc);
				result = generateBaseResult(document, xpath);
				if (this.propertiesPaths != null && !this.propertiesPaths.isEmpty()) {
					addProperties(result, document, xpath);
				}
			} else {
				result = generateProblemResult("Some problem in report generation");
			}
		} catch (InterruptedException e) {
			String message = "Process has been interrupted";
			logger.info(message, e);
			result = generateProblemResult(message, e);
		} catch (Throwable e) {
			String message = "Some problem in generating result";
			logger.info(message, e);
			result = generateProblemResult(message, e);
		} finally {
			if (report != null && !report.delete()) {
				logger.info("Report has not been deleted manually");
			}
		}
		if (!stopped) {
			this.resource.validationFinished(result);
		}
	}

	private void addProperties(VeraPDFValidationResult result, Document document, XPath xpath) {
		for (Map.Entry<String, String> property : this.propertiesPaths.entrySet()) {
			try {
				String value = (String) xpath.evaluate(property.getValue(), document, XPathConstants.STRING);
				result.addProperty(property.getKey(), value);
			} catch (Throwable e) {
				logger.info("Some problem in obtaining property", e);
			}
		}
	}

	private VeraPDFValidationResult generateBaseResult(Document document, XPath xpath) throws XPathExpressionException {
		VeraPDFValidationResult result = new VeraPDFValidationResult();
		String exceptionPath = BASE_PATH + "taskResult/exceptionMessage";
		String exception = (String) xpath.evaluate(exceptionPath,
				document,
				XPathConstants.STRING);
		result.setProcessingError(exception);

		String isCompliantPath = VALIDATION_REPORT_PATH + "@isCompliant";
		Boolean isCompliant = (Boolean) xpath.evaluate(isCompliantPath,
				document,
				XPathConstants.BOOLEAN);
		if (isCompliant != null) {
			result.setValid(isCompliant);
			if (!isCompliant) {
				result.setValidationErrors(getvalidationErrors(document, xpath));
			}
		}
		return result;
	}

	private List<ValidationError> getvalidationErrors(Document document, XPath xpath) throws XPathExpressionException {
		List<ValidationError> res = new ArrayList<>();
		String rulesPath = VALIDATION_REPORT_PATH + "details/rule";
		NodeList rules = (NodeList) xpath.evaluate(rulesPath,
				document,
				XPathConstants.NODESET);
		for (int i = 0; i < rules.getLength(); ++i) {
			Node rule = rules.item(i);
			NamedNodeMap attributes = rule.getAttributes();
			if (attributes.getNamedItem("status").getNodeValue().equals("failed")) {
				String specification = attributes.getNamedItem("specification").getNodeValue();
				String clause = attributes.getNamedItem("clause").getNodeValue();
				String testNumber = attributes.getNamedItem("testNumber").getNodeValue();
				String description = null;
				NodeList children = rule.getChildNodes();
				for (int j = 0; j < children.getLength(); ++j) {
					Node child = children.item(j);
					if (child.getNodeName().equals("description")) {
						description = child.getNodeValue();
					}
				}
				res.add(new ValidationError(specification, clause, testNumber, description));
			}
		}
		return res;
	}

	void stopProcess() {
		this.stopped = true;
		if (this.process != null && this.process.isAlive()) {
			this.process.destroy();
		}
	}

	private VeraPDFValidationResult generateProblemResult(String message, Throwable e) {
		return generateProblemResult(message + ": " + e.getMessage());
	}

	private VeraPDFValidationResult generateProblemResult(String message) {
		VeraPDFValidationResult res = new VeraPDFValidationResult();
		res.setProcessingError(message);
		return res;
	}
}

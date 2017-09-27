package org.verapdf.service;

import javanet.staxutils.SimpleNamespaceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationError;
import org.verapdf.crawler.api.validation.ValidationSettings;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
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
	private final ValidationSettings settings;

	VeraPDFProcessor(String verapdfPath, String filePath, ValidationResource resource, ValidationSettings settings) {
		this.verapdfPath = verapdfPath;
		this.filePath = filePath;
		this.resource = resource;
		this.settings = settings;
	}

	private File getVeraPDFReport(String filename) throws IOException, InterruptedException {
		String[] cmd = {verapdfPath, "--extract", "--format", "mrr", "--maxfailuresdisplayed", "1", filename};
		ProcessBuilder pb = new ProcessBuilder().inheritIO();
		Path outputPath = Files.createTempFile("veraPDFReport", ".xml");
		File file = outputPath.toFile();
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
			if (report != null && !stopped) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.parse(report);
				XPathFactory xpf = XPathFactory.newInstance();
				XPath xpath = xpf.newXPath();
				SimpleNamespaceContext nsc = new SimpleNamespaceContext();
				addNameSpaces(nsc);
				xpath.setNamespaceContext(nsc);
				result = generateBaseResult(document, xpath);
				if (this.settings != null) {
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

	private void addNameSpaces(SimpleNamespaceContext nsc) {
		if (this.settings != null) {
			Map<String, String> namespaces = this.settings.getNamespaces();
			if (namespaces != null) {
				for (Map.Entry<String, String> entry : namespaces.entrySet()) {
					nsc.setPrefix(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	private void addProperties(VeraPDFValidationResult result, Document document, XPath xpath) {
		Map<String, List<String>> properties = this.settings.getProperties();
		if (properties == null) {
			return;
		}
		for (Map.Entry<String, List<String>> property : properties.entrySet()) {
			try {
				for (String propertyXPath : property.getValue()) {
					String value = (String) xpath.evaluate(propertyXPath, document, XPathConstants.STRING);
					result.addProperty(property.getKey(), value);
					if (value != null && !value.isEmpty()) {
						break;
					}
				}
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
		if (exception != null && !exception.isEmpty()) {
			result.addValidationError(new ValidationError(exception));
		}

		String isCompliantPath = VALIDATION_REPORT_PATH + "@isCompliant";
		String isCompliantString = (String) xpath.evaluate(isCompliantPath,
				document,
				XPathConstants.STRING);
		boolean isCompliant = Boolean.parseBoolean(isCompliantString);
		result.setValid(isCompliant);
		if (!isCompliant) {
			addValidationErrors(result, document, xpath);
		}
		return result;
	}

	private void addValidationErrors(VeraPDFValidationResult result, Document document, XPath xpath) throws XPathExpressionException {
		String rulesPath = VALIDATION_REPORT_PATH + "details/rule";
		NodeList rules = (NodeList) xpath.evaluate(rulesPath,
				document,
				XPathConstants.NODESET);
		for (int i = 0; i < rules.getLength(); ++i) {
			Node rule = rules.item(i);
			NamedNodeMap attributes = rule.getAttributes();
			if (attributes.getNamedItem("status").getNodeValue().equalsIgnoreCase("failed")) {
				String specification = attributes.getNamedItem("specification").getNodeValue();
				String clause = attributes.getNamedItem("clause").getNodeValue();
				String testNumber = attributes.getNamedItem("testNumber").getNodeValue();
				String description = null;
				NodeList children = rule.getChildNodes();
				for (int j = 0; j < children.getLength(); ++j) {
					Node child = children.item(j);
					if (child.getNodeName().equals("description")) {
						description = child.getTextContent();
						break;
					}
				}
				result.addValidationError(new ValidationError(specification, clause, testNumber, description));
			}
		}
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
		res.addValidationError(new ValidationError(message));
		return res;
	}
}

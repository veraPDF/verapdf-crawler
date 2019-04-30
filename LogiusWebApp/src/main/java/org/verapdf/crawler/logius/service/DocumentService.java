package org.verapdf.crawler.logius.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.DocumentDAO;
import org.verapdf.crawler.logius.db.ValidationJobDAO;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {
	private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
	private final DocumentDAO documentDAO;
	private final ValidationJobDAO validationJobDAO;

	public DocumentService(DocumentDAO documentDAO, ValidationJobDAO validationJobDAO) {
		this.documentDAO = documentDAO;
		this.validationJobDAO = validationJobDAO;
	}

	@Transactional
	public List<String> findNotFinishedJobsByUserRoleAndServiceAndDownloadCount(Role role, CrawlJob.CrawlService service, int limit) {
		return documentDAO.findNotFinishedJobsByUserRoleAndServiceAndDownloadCount(role, service, limit);
	}

	@Transactional
	public void saveDocument(String url, String fileType, CrawlJob crawlJob) {
		DomainDocument domainDocument = new DomainDocument();
		domainDocument.getDocumentId().setDocumentUrl(url);
		domainDocument.getDocumentId().setCrawlJob(crawlJob);
		domainDocument.setContentType(fileType);
		saveDocument(domainDocument, crawlJob);
	}

	@Transactional
	public DomainDocument saveDocument(DomainDocument document, CrawlJob job) {
		document.getDocumentId().setCrawlJob(job);
		documentDAO.save(document);

		switch (document.getContentType().toLowerCase()) {
			case "pdf":
				validatePdfFile(document);
				break;
			case "odt":
			case "ods":
			case "odp":
				validateOpenOfficeFile(document);
				break;
			case "doc":
			case "docx":
			case "xls":
			case "xlsx":
			case "ppt":
			case "pptx":
				validateMSOfficeFile(document);
				break;
			default:
				logger.warn("Unknown document type " + document.getContentType() + ". Document " + document.getDocumentUrl() + " won't be tested.");
		}

		return document;
	}

	private void validatePdfFile(DomainDocument document) {
		ValidationJob validationJob = new ValidationJob(document);
		validationJob.setCreationDate(LocalDateTime.now());
		validationJobDAO.save(validationJob);
	}

	private void validateOpenOfficeFile(DomainDocument document) {
		document.setBaseTestResult(DomainDocument.BaseTestResult.OPEN);
	}

	private void validateMSOfficeFile(DomainDocument document) {
		document.setBaseTestResult(DomainDocument.BaseTestResult.NOT_OPEN);
	}
}

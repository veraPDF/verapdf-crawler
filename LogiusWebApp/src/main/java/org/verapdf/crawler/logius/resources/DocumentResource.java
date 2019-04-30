package org.verapdf.crawler.logius.resources;


import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.service.DocumentService;


@RestController
@RequestMapping(value = "api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentResource {
	private final CrawlJobDAO crawlJobDAO;
    private final DocumentService documentService;

	public DocumentResource(CrawlJobDAO crawlJobDAO, DocumentService documentService) {
		this.crawlJobDAO = crawlJobDAO;
		this.documentService = documentService;
	}

	@PostMapping
	@Transactional
	public DomainDocument saveDocument(@RequestBody DomainDocument document) {
		CrawlJob job = crawlJobDAO.getByHeritrixJobId(document.getDocumentId().getCrawlJob().getHeritrixJobId());
		if (job == null) {
			return null;
		}
		return documentService.saveDocument(document, job);
	}
}

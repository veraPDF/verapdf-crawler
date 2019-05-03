package org.verapdf.crawler.logius.resources;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;
import org.verapdf.crawler.logius.monitoring.CrawlJobStatus;
import org.verapdf.crawler.logius.service.CrawlJobService;
import org.xml.sax.SAXException;

import javax.validation.constraints.NotNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.UUID;


/**
 * @author Maksim Bezrukov
 */
@RestController
@RequestMapping(value = "api/admin")
public class AdminResource {

	private final CrawlJobService crawlJobService;

	public AdminResource(CrawlJobService crawlJobService) {
		this.crawlJobService = crawlJobService;
	}

	@GetMapping("/crawl-jobs/{id}/status")
	public ResponseEntity<CrawlJobStatus> getFullJobStatus(@PathVariable("id") UUID id) {
		CrawlJobStatus crawlJobStatus = crawlJobService.getFullJobStatus(id);
		return ResponseEntity.ok(crawlJobStatus);
	}

	@PutMapping("/crawl-jobs/{id}")
	public ResponseEntity<CrawlJob> updateCrawlJob(
			@PathVariable("id") UUID id,
			@RequestBody @NotNull CrawlJob update) throws IOException,
	                                                      XPathExpressionException,
	                                                      SAXException,
	                                                      ParserConfigurationException {
		CrawlJob updatedCrawlJob = crawlJobService.update(update);
		return ResponseEntity.ok(updatedCrawlJob);
	}

	@PostMapping("/crawl-jobs/{id}")
	public ResponseEntity<CrawlJob> restartCrawlJob(@PathVariable("id") UUID id) {
		return ResponseEntity.ok(crawlJobService.restartCrawlJob(id));
	}

	@DeleteMapping("/crawl-jobs/{id}")
	public ResponseEntity cancelCrawlJob(@PathVariable("id") UUID id) {
		crawlJobService.cancelCrawlJob(id);
		return ResponseEntity.noContent().build();
	}
}

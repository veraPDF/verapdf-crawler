package org.verapdf.crawler.logius.core.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.core.email.SendEmailService;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.CrawlJobDAO;
import org.verapdf.crawler.logius.service.DocumentService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class BingTask extends AbstractTask {
	private static final Logger logger = LoggerFactory.getLogger(BingTask.class);
	private static final long SLEEP_DURATION = 60 * 1000;
	private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList("pdf", "odt", "ods", "odp", "doc", "xls", "ppt");
	private final DocumentService documentService;
	private final CrawlJobDAO crawlJobDAO;
	@Value("${logius.bing.apiKey}")
	private String apiKey;
	private CrawlJob currentJob = null;

	public BingTask(SendEmailService email, DocumentService documentService, CrawlJobDAO crawlJobDAO) {
		super(SLEEP_DURATION, email);
		this.documentService = documentService;
		this.crawlJobDAO = crawlJobDAO;
	}

	@Override
	@Transactional
	public void run() {
		super.run();
	}

	@Override
	protected void process() throws InterruptedException {
		if (currentJob != null) {
			return;
		}
		try {
			List<CrawlJob> newJob = crawlJobDAO.findByStatus(CrawlJob.Status.NEW, CrawlJob.CrawlService.BING, null, 1);
			if (newJob != null && !newJob.isEmpty()) {
				CrawlJob crawlJob = newJob.get(0);
				crawlJob.setStatus(CrawlJob.Status.RUNNING);
				currentJob = crawlJob;
				for (String ALLOWED_FILE_TYPE : ALLOWED_FILE_TYPES) {
					processFileType(ALLOWED_FILE_TYPE);
				}
			}
		} finally {
			currentJob = null;
		}
	}

	private void processFileType(String fileType) throws InterruptedException {
		Set<String> pdfs = obtainURLs(fileType);
		for (String url : pdfs) {
			if (this.currentJob != null) {
				documentService.saveDocument(url, fileType, this.currentJob);
			}
		}
	}

	private Set<String> obtainURLs(String fileType) throws InterruptedException {
		Set<String> result = new HashSet<>();
		int offset = 0;
		if (this.currentJob != null) {
			String site = this.currentJob.getDomain();
			String urlWithoutOffset = "https://api.cognitive.microsoft.com/bing/v7.0/search?" +
			                          "q=site%3a" + site + "+filetype%3a" + fileType +
			                          "&count=50&offset=";
			int currentEstimations = offset + 1;
			// bing can return only 1000 results
			while (currentEstimations > offset && this.currentJob != null && offset <= 1000) {
				try {
					currentEstimations = obtainResults(result, urlWithoutOffset, this.apiKey, offset);
					offset += 50;
					Thread.sleep(10);
				} catch (IOException e) {
					logger.error("Some error during links obtaining", e);
				}
			}
		}
		return result;
	}

	private int obtainResults(Set<String> resultsSet, String urlWithoutOffset, String key, long offset) throws IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(urlWithoutOffset + offset);
		request.addHeader("Ocp-Apim-Subscription-Key", key);
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != 200) {
			return 0;
		}
		try (InputStream is = response.getEntity().getContent()) {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readValue(is, JsonNode.class);
			JsonNode webPages = rootNode.get("webPages");
			if (webPages == null) {
				return 0;
			}
			JsonNode value = webPages.get("value");
			Iterator<JsonNode> it = value.elements();
			while (it.hasNext()) {
				JsonNode next = it.next();
				JsonNode url = next.get("url");
				if (url.isTextual()) {
					resultsSet.add(url.asText());
				}
			}
			JsonNode totalEstimatedMatches = webPages.get("totalEstimatedMatches");
			return totalEstimatedMatches.isInt() ? totalEstimatedMatches.asInt() : 0;
		}
	}

	public CrawlJob getCurrentJob() {
		return currentJob;
	}

	public void discardJob(CrawlJob job) {
		if (this.currentJob != null && this.currentJob.getDomain().equals(job.getDomain())) {
			this.currentJob = null;
		}
	}
}

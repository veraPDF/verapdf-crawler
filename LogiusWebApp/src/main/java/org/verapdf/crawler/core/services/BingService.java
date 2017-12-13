package org.verapdf.crawler.core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.configurations.BingConfiguration;
import org.verapdf.crawler.db.CrawlJobDAO;
import org.verapdf.crawler.db.DocumentDAO;
import org.verapdf.crawler.db.ValidationJobDAO;
import org.verapdf.crawler.resources.DocumentResource;
import org.verapdf.crawler.tools.AbstractService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Maksim Bezrukov
 */
public class BingService extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(HeritrixCleanerService.class);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	private static final long SLEEP_DURATION = 60*1000;

	private static final Map<String, String> fileTypes = new HashMap<>();

	static {
		fileTypes.put("application/pdf", "pdf");
		fileTypes.put("application/vnd.oasis.opendocument.text", "odt");
		fileTypes.put("application/vnd.oasis.opendocument.spreadsheet", "ods");
		fileTypes.put("application/vnd.oasis.opendocument.presentation", "odp");
		fileTypes.put("application/msword", "doc");
		fileTypes.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
		fileTypes.put("application/vnd.ms-powerpoint", "ppt");
		fileTypes.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
		fileTypes.put("application/vnd.ms-excel", "xls");
		fileTypes.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
	}

	private final File baseTempFolder;
	private final String apiKey;
	private final ResourceManager resourceManager;
	private CrawlJob currentJob = null;

	public BingService(BingConfiguration bingConfiguration, ResourceManager resourceManager) {
		super("BingService", SLEEP_DURATION);
		this.baseTempFolder = new File(bingConfiguration.getBaseTempFolder());
		if (!this.baseTempFolder.isDirectory() && (this.baseTempFolder.exists() || !this.baseTempFolder.mkdirs())) {
			throw new IllegalStateException("Initialization fail on obtaining temp folder");
		}
		this.apiKey = bingConfiguration.getApiKey();
		this.resourceManager = resourceManager;
	}

	@Override
	protected void onStart() {
	}

	@Override
	protected boolean onRepeat() {
		return checkNewJobs();
	}

	@SuppressWarnings("WeakerAccess")
	@UnitOfWork
	public boolean checkNewJobs() {
		this.currentJob = getJob();
		if (this.currentJob != null) {
			File tempFolder = new File(this.baseTempFolder, this.currentJob.getHeritrixJobId());
			if (!tempFolder.isDirectory() && (tempFolder.exists() || !tempFolder.mkdirs())) {
				throw new IllegalStateException("Initialization fail on obtaining job temp folder");
			}
			processFileType("pdf", tempFolder);
			processFileType("odt", null);
			processFileType("ods", null);
			processFileType("odp", null);
			processFileType("doc", null);
			processFileType("xls", null);
			processFileType("ppt", null);
			this.currentJob = null;
			return false;
		}
		return true;
	}

	@SuppressWarnings("WeakerAccess")
	@UnitOfWork
	public CrawlJob getJob() {
		List<CrawlJob> byStatus = resourceManager.getCrawlJobDAO().findByStatus(CrawlJob.Status.NEW, CrawlJob.CrawlService.BING, null, 1);
		if (byStatus != null && !byStatus.isEmpty()) {
			CrawlJob crawlJob = byStatus.get(0);
			crawlJob.setStatus(CrawlJob.Status.RUNNING);
			return crawlJob;
		}
		return null;
	}

	private void processFileType(String fileType, File tempFolder) {
		Set<String> pdfs = obtainURLs(fileType);
		for (String url : pdfs) {
			processFile(url, fileType, tempFolder);
		}
	}

	@SuppressWarnings("WeakerAccess")
	@UnitOfWork
	public void processFile(String url, String fileType, File tempFolder) {
		try {
			try (CloseableHttpClient client = HttpClients.createDefault()) {
				HttpGet get = new HttpGet(url);
				CloseableHttpResponse response = client.execute(get);
				String contentType = null;
				Header[] contentTypeHeaders = response.getHeaders("Content-Type");
				if (contentTypeHeaders != null && contentTypeHeaders.length > 0) {
					String value = contentTypeHeaders[0].getValue();
					if (value != null) {
						if (value.startsWith("text")) {
							return;
						} else if (fileTypes.containsKey(value)) {
							contentType = fileTypes.get(value);
						}
					}
				}
				if (contentType == null) {
					contentType = FilenameUtils.getExtension(url);
				}
				if (contentType == null) {
					contentType = fileType;
				}
				DomainDocument domainDocument = new DomainDocument();
				domainDocument.setUrl(url);
				domainDocument.setCrawlJob(this.currentJob);
				domainDocument.setContentType(contentType);
				Header[] lastModHeaders = response.getHeaders("Last-Modified");
				if (lastModHeaders != null && lastModHeaders.length > 0) {
					domainDocument.setLastModified(dateFormat.parse(lastModHeaders[0].getValue()));
				}
				if (tempFolder != null) {
					File file = File.createTempFile("logius", "." + contentType, tempFolder);
					IOUtils.copy(response.getEntity().getContent(), new FileOutputStream(file));
					domainDocument.setFilePath(file.getAbsolutePath());
				}
				if (this.currentJob != null) {
					DocumentResource.saveDocument(domainDocument, this.currentJob, this.resourceManager);
				}
			} catch (ParseException e) {
				logger.error("Can't obtain last modified for url: "+ url, e);
			}
		} catch (IOException e) {
			logger.error("Can't create url: " + url, e);
		}
	}

	private Set<String> obtainURLs(String fileType) {
		Set<String> result = new HashSet<>();
		int offset = 0;
		if (this.currentJob != null) {
			String site = this.currentJob.getDomain();
			String urlWithoutOffset = "https://api.cognitive.microsoft.com/bing/v7.0/search?" +
					"q=site%3a" + site + "+filetype%3a" + fileType +
					"&count=50&offset=";
			int currentEstimations = offset + 1;
			while (currentEstimations > offset && this.currentJob != null) {
				try {
					currentEstimations = obtainResults(result, urlWithoutOffset, this.apiKey, offset);
					offset += 50;
					Thread.sleep(10);
				} catch (IOException | InterruptedException e) {
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
		if (response.getStatusLine().getStatusCode() == 200) {
			try (InputStream is = response.getEntity().getContent()) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readValue(is, JsonNode.class);
				JsonNode webPages = rootNode.get("webPages");
				if (webPages != null) {
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
					if (totalEstimatedMatches.isInt()) {
						return totalEstimatedMatches.asInt();
					}
				}
			}
		}
		return 0;
	}

	public CrawlJob getCurrentJob() {
		return currentJob;
	}

	public void discardJob(CrawlJob job) {
		if (this.currentJob != null && this.currentJob.getDomain().equals(job.getDomain())) {
			this.currentJob = null;
		}
		deleteTempFolder(job);
	}

	public boolean deleteTempFolder(CrawlJob job) {
		try {
			FileUtils.deleteDirectory(new File(this.baseTempFolder, job.getHeritrixJobId()));
			return true;
		} catch (IOException e) {
			logger.error("Can't delete bing job folder", e);
			return false;
		}
	}
}

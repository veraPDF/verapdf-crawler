package org.verapdf.crawler.logius.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.resources.DocumentResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BingService {
    private static final Logger logger = LoggerFactory.getLogger(BingService.class);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private final CrawlJobService crawlJobService;
    private final DocumentResource documentResource;
    private final Map<String, String> fileTypes;
    private String apiKey;
    private CrawlJob currentJob = null;

    public BingService(@Value("${logius.bing.apiKey}") String apiKey,
                       CrawlJobService crawlJobService,
                       DocumentResource documentResource,
                       @Qualifier("fileTypes") Map<String, String> fileTypes) {
        this.apiKey = apiKey;
        this.crawlJobService = crawlJobService;
        this.documentResource = documentResource;
        this.fileTypes = fileTypes;
    }

    @Transactional
    public boolean checkNewJobs() {
        this.currentJob = crawlJobService.getNewBingJob();
        if (this.currentJob != null) {
            processFileType("pdf");
            processFileType("odt");
            processFileType("ods");
            processFileType("odp");
            processFileType("doc");
            processFileType("xls");
            processFileType("ppt");
            this.currentJob = null;
            return false;
        }
        return true;
    }

    private void processFileType(String fileType) {
        Set<String> pdfs = obtainURLs(fileType);
        for (String url : pdfs) {
            processFile(url, fileType);
        }
    }


    public void processFile(String url, String fileType) {
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
                    try {
                        domainDocument.setLastModified(dateFormat.parse(lastModHeaders[0].getValue()));
                    } catch (ParseException e) {
                        logger.info("Fail to parse last modified date for " + url + ", lastModified won't be set for this document.");
                    }
                }
                if (this.currentJob != null) {
                    documentResource.saveDocument(domainDocument, this.currentJob);
                }
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
            while (currentEstimations > offset && this.currentJob != null && offset <= 1000) {
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
    }
}

package org.verapdf.crawler.extention;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.MirrorWriterProcessor;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DocumentProcessor extends MirrorWriterProcessor {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private CloseableHttpClient httpClient;

    private ObjectMapper mapper;

    private String jobId;

    private String logiusUrl;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getLogiusUrl() {
        return logiusUrl;
    }

    public void setLogiusUrl(String logiusUrl) {
        this.logiusUrl = logiusUrl;
    }

    private Map<String, String> supportedContentTypes;

    public Map<String, String> getSupportedContentTypes() {
        return supportedContentTypes;
    }

    public void setSupportedContentTypes(Map<String, String> supportedContentTypes) {
        this.supportedContentTypes = supportedContentTypes;
    }

    public DocumentProcessor() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        httpClient = HttpClients.createDefault();
    }

    @Override
    protected boolean shouldProcess(CrawlURI crawlURI) {
        // TODO: add crawlSince parameter, and compare here with Last-Modified value and do now download and process older files

        if (supportedContentTypes.keySet().contains(crawlURI.getContentType())) {
            return true;
        }

        String extension = FilenameUtils.getExtension(crawlURI.getURI());
        return supportedContentTypes.values().contains(extension);
    }

    @Override
    protected void innerProcess(CrawlURI crawlURI) {
        try {
            // Download file
            super.innerProcess(crawlURI);

            // Create document
            DomainDocument document = new DomainDocument(jobId, crawlURI.getURI());

            // File path
            String baseDir = getPath().getFile().getCanonicalPath();
            String fileName = crawlURI.getData().get(A_MIRROR_PATH).toString();
            document.setFilePath(FilenameUtils.concat(baseDir, fileName));

            // Content type
            if (supportedContentTypes.containsKey(crawlURI.getContentType())) {
                document.setContentType(supportedContentTypes.get(crawlURI.getContentType()));
            } else {
                document.setContentType(FilenameUtils.getExtension(crawlURI.getURI()));
            }

            // Set last modified
            Header lastModifiedHeader = crawlURI.getHttpMethod().getResponseHeader("Last-Modified");
            if (lastModifiedHeader != null) {
                try {
                    document.setLastModified(dateFormat.parse(lastModifiedHeader.getValue()));
                } catch (ParseException e) {
                    System.out.println("Fail to parse " + lastModifiedHeader + " for " + crawlURI.getURI() + ", lastModified won't be set for this document.");
                    e.printStackTrace();
                }
            }

            // Send to main application for further processing
            HttpPost request = new HttpPost(logiusUrl + "/api/documents");
            String documentString = mapper.writeValueAsString(document);
            StringEntity payload = new StringEntity(documentString, ContentType.APPLICATION_JSON);
            request.setEntity(payload);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    System.out.println("Fail to POST document " + documentString + "."
                            + " Response " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase()
                            + "\n" + IOUtils.toString(response.getEntity().getContent()));
                }
            }
        } catch (IOException e) {
            System.out.println("Fail to process " + crawlURI.getURI());
            e.printStackTrace();
        }
    }
}

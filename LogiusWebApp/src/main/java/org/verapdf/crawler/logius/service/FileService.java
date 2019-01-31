package org.verapdf.crawler.logius.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final Map<String, String> fileTypes;
    private File baseTempFolder;

    public FileService(@Value("${logius.bing.baseTempFolder}") String baseTempUrl,
                       @Qualifier("fileTypes") Map<String, String> fileTypes) {
        this.baseTempFolder = new File(baseTempUrl);
        this.fileTypes = fileTypes;
        if (!this.baseTempFolder.isDirectory() || !this.baseTempFolder.exists()) {
            throw new IllegalStateException("Initialization fail on obtaining temp folder");
        }
        try {
            FileUtils.cleanDirectory(this.baseTempFolder);
        } catch (IOException e) {
            throw new IllegalStateException("fail clean temp folder");
        }
    }

    public void save(ValidationJob validationJob) {
        try {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet get = new HttpGet(validationJob.getId());
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
                File file = File.createTempFile("logius", "." + contentType, baseTempFolder);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                IOUtils.copy(response.getEntity().getContent(), fileOutputStream);
                validationJob.setFilePath(file.getAbsolutePath());
                fileOutputStream.close();
            }
        } catch (IOException e) {
            logger.error("Can't create url: " + validationJob.getId(), e);
        }
    }
}

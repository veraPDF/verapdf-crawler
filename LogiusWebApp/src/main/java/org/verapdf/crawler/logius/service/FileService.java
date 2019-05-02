package org.verapdf.crawler.logius.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.exception.DownloadFileProcessingException;
import org.verapdf.crawler.logius.exception.IncorrectContentTypeException;
import org.verapdf.crawler.logius.tools.HttpClientUtils;
import org.verapdf.crawler.logius.validation.ValidationJob;

import javax.annotation.PostConstruct;
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
	}

	@PostConstruct
	public void init() {
		try {
			FileUtils.cleanDirectory(this.baseTempFolder);
		} catch (IOException e) {
			throw new IllegalStateException("fail clean temp folder");
		}
	}

	public File downloadFile(ValidationJob job) throws DownloadFileProcessingException {
		String url = job.getDocumentUrl();
		try (CloseableHttpClient client = HttpClientUtils.createTrustAllHttpClient()) {
			HttpGet get = new HttpGet(url);
			CloseableHttpResponse response = client.execute(get);
			Header contentTypeHeader = response.getFirstHeader("Content-Type");
			String contentType = getFileExtension(contentTypeHeader, url);
			if (contentType.equals(job.getDocument().getContentType())){
				throw new IncorrectContentTypeException("Content types are not equals");
			}
			File file = File.createTempFile("logius", "." + contentType, baseTempFolder);
			try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
				IOUtils.copy(response.getEntity().getContent(), fileOutputStream);
				return file;
			}
		} catch (Exception e) {
			logger.error("Can't create url: " + url, e);
			throw new DownloadFileProcessingException(e);
		}
	}

	public String getFileExtension(Header contentTypeHeader, String url) throws IncorrectContentTypeException {
		if (contentTypeHeader == null || contentTypeHeader.getValue().startsWith("text")) {
			throw new IncorrectContentTypeException("ContentType is null or start with text");
		}
		String value = contentTypeHeader.getValue();
		for (String fileType : fileTypes.keySet()) {
			if (value.startsWith(fileType)) {
				return fileTypes.get(fileType);
			}
		}
		String extension = FilenameUtils.getExtension(url);
		if (fileTypes.values().contains(extension)){
			return extension;
		}
		throw new IncorrectContentTypeException(String.format("Content type is null for url %s", url));
	}

	public void deleteFile(File file) {
		if (file != null && file.isFile() && !file.delete()) {
			logger.warn("Failed to clean validation job file " + file.getAbsolutePath());
		}
	}
}

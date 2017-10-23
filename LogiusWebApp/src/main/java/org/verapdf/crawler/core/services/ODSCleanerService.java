package org.verapdf.crawler.core.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.configurations.ReportsConfiguration;
import org.verapdf.crawler.tools.AbstractService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * @author Maksim Bezrukov
 */
public class ODSCleanerService extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(HeritrixCleanerService.class);

	private static final long SLEEP_DURATION = 60*60*1000;

	private static final long FILE_LIFETIME_IN_MILLIS = 7*24*60*60*1000;

	private final ReportsConfiguration config;

	public ODSCleanerService(ReportsConfiguration config) {
		super("ODSCleanerService", SLEEP_DURATION);
		this.config = config;
	}

	@Override
	protected void onStart() {
	}

	@Override
	protected boolean onRepeat() throws InterruptedException {
		File odsTempFolder = new File(config.getOdsTempFolder());
		if (odsTempFolder.isDirectory()) {
			long currentTimeInMillis = System.currentTimeMillis();
			for (File ods : odsTempFolder.listFiles()) {
				checkFile(ods, currentTimeInMillis);
			}
		}
		return true;
	}

	private void checkFile(File file, long currentTimeInMillis) {
		if (file.exists()) {
			try {
				BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				FileTime fileTime = attributes.creationTime();
				if (currentTimeInMillis - fileTime.toMillis() > FILE_LIFETIME_IN_MILLIS) {
					file.delete();
				}
			} catch (IOException e) {
				logger.error("Error in removing a file with path " + file.getAbsolutePath(), e);
			}
		}
	}
}

package org.verapdf.crawler.core.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

/**
 * @author Maksim Bezrukov
 */
public class HeritrixCleanerService implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(HeritrixCleanerService.class);

	private final HeritrixClient heritrixClient;
	private final Set<String> heritrixJobIds = Collections.synchronizedSet(new HashSet<>());
	private boolean running;

	public HeritrixCleanerService(HeritrixClient heritrixClient) {
		running = false;
		this.heritrixClient = heritrixClient;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		running = true;
		new Thread(this, "Thread-HeritrixCleanerService").start();
	}

	@Override
	public void run() {
		logger.info("Heritrix cleaner service started");
		while (running) {
			if (!heritrixJobIds.isEmpty()) {
				// will create here another set for removing objects
				// this is necesary, because with iterator based solution we have to
				// lock that part of code, but we do not want to do that here
				Set<String> removed = new HashSet<>();
				for (String id : heritrixJobIds) {
					try {
						if (heritrixClient.deleteJobFolder(id)) {
							removed.add(id);
						}
					} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
						logger.error("Error during heritrix job deleting", e);
					}
				}
				heritrixJobIds.removeAll(removed);
			}
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void teardownAndClearHeritrixJob(String heritrixJobId) {
		try {
			heritrixClient.teardownJob(heritrixJobId);
		} catch (IOException e) {
			logger.error("Can't teardown heritrix job: " + heritrixJobId, e);
		}
		this.heritrixJobIds.add(heritrixJobId);
	}
}

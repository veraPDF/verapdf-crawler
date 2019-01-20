package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.email.SendEmail;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Maksim Bezrukov
 */
@Service
public class HeritrixCleanerTask extends AbstractTask {
    private static final Logger logger = LoggerFactory.getLogger(HeritrixCleanerTask.class);
    private static final long SLEEP_DURATION = 60 * 1000;
    private final HeritrixClient heritrixClient;
    private final Set<String> heritrixJobIds = Collections.synchronizedSet(new HashSet<>());

    public HeritrixCleanerTask(HeritrixClient heritrixClient, SendEmail email) {
        super("HeritrixCleanerTask", SLEEP_DURATION, email);
        this.heritrixClient = heritrixClient;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onRepeat() {
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
        return true;
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

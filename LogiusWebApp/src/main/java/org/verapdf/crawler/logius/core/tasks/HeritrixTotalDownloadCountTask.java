package org.verapdf.crawler.logius.core.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.email.SendEmailService;
import org.verapdf.crawler.logius.core.heritrix.HeritrixClient;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.service.DocumentService;

import java.util.List;

import static org.verapdf.crawler.logius.crawling.CrawlJob.CrawlService.HERITRIX;

@Component
public class HeritrixTotalDownloadCountTask extends AbstractTask {
    private static final Logger logger = LoggerFactory.getLogger(HeritrixCleanerTask.class);
    private static final long SLEEP_DURATION = 60 * 1000;
    @Value("${logius.heritrix.maxDownloadDocumentCount}")
    private int maxDownloadDocumentCount;
    private final HeritrixClient heritrixClient;
    private final DocumentService documentService;

    public HeritrixTotalDownloadCountTask(HeritrixClient heritrixClient, SendEmailService email, DocumentService documentService) {
        super("HeritrixTotalDownloadCountTask", SLEEP_DURATION, email);
        this.heritrixClient = heritrixClient;
        this.documentService = documentService;
    }


    @Override
    protected void process() {
        List<String> crawlJobs = documentService.findNotFinishedJobsByUserRoleAndServiceAndDownloadCount(Role.USER, HERITRIX, maxDownloadDocumentCount);
        for (String heritrixJobId : crawlJobs) {
            heritrixClient.terminateJob(heritrixJobId);
        }
    }
}

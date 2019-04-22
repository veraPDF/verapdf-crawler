package org.verapdf.crawler.logius.core.tasks;

import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.core.email.SendEmail;
import org.verapdf.crawler.logius.service.MonitoringCrawlJobStatusService;

/**
 * @author Maksim Bezrukov
 */

@Component
public class MonitorCrawlJobStatusTask extends AbstractTask {
    private static final long SLEEP_DURATION = 60 * 1000;

    private final MonitoringCrawlJobStatusService monitoringCrawlJobStatusService;

    public MonitorCrawlJobStatusTask(MonitoringCrawlJobStatusService monitoringCrawlJobStatusService, SendEmail sendEmail) {
        super("MonitorCrawlJobStatusTask", SLEEP_DURATION, sendEmail);
        this.monitoringCrawlJobStatusService = monitoringCrawlJobStatusService;
    }

    @Override
    protected void process() {
        String lastDomain = null;
        do {
            lastDomain = monitoringCrawlJobStatusService.checkJobsBatch(lastDomain);
        } while (lastDomain != null);
    }
}

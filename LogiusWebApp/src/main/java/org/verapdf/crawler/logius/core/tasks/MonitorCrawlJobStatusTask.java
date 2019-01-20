package org.verapdf.crawler.logius.core.tasks;

import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.service.MonitoringCrawlJobStatusService;
import org.verapdf.crawler.logius.tools.AbstractService;

/**
 * @author Maksim Bezrukov
 */

@Service
public class MonitorCrawlJobStatusTask extends AbstractService {
    private static final long SLEEP_DURATION = 60 * 1000;

    private final MonitoringCrawlJobStatusService monitoringCrawlJobStatusService;

    public MonitorCrawlJobStatusTask(MonitoringCrawlJobStatusService monitoringCrawlJobStatusService) {
        super("MonitorCrawlJobStatusService", SLEEP_DURATION);
        this.monitoringCrawlJobStatusService = monitoringCrawlJobStatusService;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onRepeat() {
        String lastDomain = null;
        while (isRunning()) {
            lastDomain = monitoringCrawlJobStatusService.checkJobsBatch(lastDomain);
            if (lastDomain == null) {
                break;
            }
        }
        return true;
    }
}

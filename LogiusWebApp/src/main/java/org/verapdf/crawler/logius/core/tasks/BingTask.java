package org.verapdf.crawler.logius.core.tasks;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.core.email.SendEmail;
import org.verapdf.crawler.logius.service.BingService;

/**
 * @author Maksim Bezrukov
 */
@Component
public class BingTask extends AbstractTask {
    private static final long SLEEP_DURATION = 60 * 1000;
    private final BingService bingService;


    public BingTask(BingService bingService, SendEmail sendEmail) {
        super("BingTask", SLEEP_DURATION, sendEmail);
        this.bingService = bingService;
    }

    @Override
    protected void onStart() {
        bingService.checkNewJobs();
    }

    @Override
    protected boolean onRepeat() {
        return bingService.checkNewJobs();
    }
}

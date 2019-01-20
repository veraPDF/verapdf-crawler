package org.verapdf.crawler.logius.core.tasks;

import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.service.BingService;
import org.verapdf.crawler.logius.tools.AbstractService;

/**
 * @author Maksim Bezrukov
 */
@Service
public class BingTask extends AbstractService {
    private static final long SLEEP_DURATION = 60 * 1000;
    private final BingService bingService;


    public BingTask(BingService bingService) {
        super("BingService", SLEEP_DURATION);
        this.bingService = bingService;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected boolean onRepeat() {
        return bingService.checkNewJobs();
    }
}

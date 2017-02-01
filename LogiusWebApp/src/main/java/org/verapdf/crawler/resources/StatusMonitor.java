package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.CurrentJob;
import java.util.ArrayList;

public class StatusMonitor implements Runnable {
    private CrawlJobResource resource;

    public StatusMonitor(CrawlJobResource resource){
        this.resource = resource;
    }

    @Override
    public void run() {
        while(true) {
            ArrayList<CurrentJob> jobs = resource.getCurrentJobs();
            for(CurrentJob job : jobs) {
                if(!job.getReportEmail().equals("") && !job.isEmailSent()) {
                    try {
                        resource.getJob(job.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

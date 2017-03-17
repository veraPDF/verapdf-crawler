package org.verapdf.crawler.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.BatchJob;
import org.verapdf.crawler.api.CurrentJob;
import org.verapdf.crawler.helpers.emailUtils.SendEmail;

public class StatusMonitor implements Runnable {
    private CrawlJobResource resource;
    private static Logger logger = LoggerFactory.getLogger(StatusMonitor.class);

    public StatusMonitor(CrawlJobResource resource){
        this.resource = resource;
    }

    @Override
    public void run() {
        while(true) {
            try {
                for (BatchJob batch : resource.getBatchJobs()) { // Check if any batch jobs were finished
                    if(!batch.isFinished()) {
                        boolean isFinished = true;
                        for(String domain: batch.getDomains()) { // Look through every single-url job
                            if(!resource.getJob(resource.getJobByCrawlUrl(domain).getId()).getStatus().startsWith("Finished")) {
                                isFinished = false;
                                break;
                            }
                        }
                        if(isFinished) {
                            batch.setFinished(true);
                            String subject = "Batch job";
                            String text = "Batch job is finished. You can see the details on " +
                                    resource.getResourceUri() + "batch/" + batch.getId();
                            SendEmail.send(batch.getEmailAddress(), subject, text, resource.getEmailServer());
                        }
                    }
                }
                for (CurrentJob job : resource.getCurrentJobs()) { // Check if any single-url jobs were finished
                    if (job.getReportEmail() != null && !job.getReportEmail().equals("") && !job.isEmailSent()) {
                        resource.getJob(job.getId());
                    }
                }

                Thread.sleep(300000);
            }
            catch (Exception e) {
                logger.error("Status monitor error", e);
            }
        }
    }
}

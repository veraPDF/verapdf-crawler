package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.BatchJob;
import org.verapdf.crawler.domain.crawling.CurrentJob;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;

public class StatusMonitor implements Runnable {
    private final ResourceManager resource;
    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final CrawlJobDao crawlJobDao;

    StatusMonitor(ResourceManager resource, CrawlJobDao crawlJobDao) {
        this.resource = resource;
        this.crawlJobDao = crawlJobDao;
    }

    @Override
    public void run() {
        while(true) {
            try {
                for (BatchJob batch : resource.getBatchJobs()) { // Check if any batch jobs were finished
                    if(!batch.isFinished()) {
                        boolean isFinished = true;
                        for(String domain: batch.getDomains()) { // Look through every single-url job
                            String status = resource.getJob(crawlJobDao.getCrawlJobByCrawlUrl(domain).getId()).getStatus();
                            if(! (status.startsWith("finished") || status.startsWith("aborted"))) {
                                isFinished = false;
                                break;
                            }
                        }
                        if(isFinished) {
                            batch.setFinished();
                            String subject = "Batch job";
                            String text = "Batch job is finished. You can see the details on " +
                                    resource.getResourceUri() + "batch/" + batch.getId();
                            SendEmail.send(batch.getEmailAddress(), subject, text, resource.getEmailServer());
                        }
                    }
                }
                for (CurrentJob job : crawlJobDao.getAllJobs()) { // Check if any single-url jobs were finished
                    if (job.getReportEmail() != null && !job.getReportEmail().equals("") && !job.isFinished()) {
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

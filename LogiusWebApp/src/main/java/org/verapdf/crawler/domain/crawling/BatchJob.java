package org.verapdf.crawler.domain.crawling;

import java.util.ArrayList;

public class BatchJob {
    private ArrayList<String> domains;
    private String id;
    private boolean isFinished;
    private String emailAddress;

    public BatchJob(String id, String emailAddress) {
        domains = new ArrayList<>();
        this.id = id;
        this.emailAddress = emailAddress;
        this.isFinished = false;
    }

    public String getId() { return  id; }

    public ArrayList<String> getDomains() { return domains; }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public String getEmailAddress() { return emailAddress; }

}

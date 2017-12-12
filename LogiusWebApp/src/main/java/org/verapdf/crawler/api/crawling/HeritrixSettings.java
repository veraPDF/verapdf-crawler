package org.verapdf.crawler.api.crawling;

public class HeritrixSettings {
    private String engineUrl;

    public HeritrixSettings() {
    }

    public HeritrixSettings(String engineUrl) {
        this.engineUrl = engineUrl;
    }

    public String getEngineUrl() {
        return engineUrl;
    }

    public void setEngineUrl(String engineUrl) {
        this.engineUrl = engineUrl;
    }
}

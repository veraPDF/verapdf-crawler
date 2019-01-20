package org.verapdf.crawler.logius.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class HeritrixCrawlJobStatus {

    @JsonProperty
    private String statusDescription;
    @JsonProperty
    private HeritrixURITotalsStatus uriTotalsStatus;
    @JsonProperty
    private List<String> jobLogTail;

    public HeritrixCrawlJobStatus() {
    }

    public HeritrixCrawlJobStatus(String statusDescription, HeritrixURITotalsStatus uriTotalsStatus, List<String> jobLogTail) {
        this.statusDescription = statusDescription;
        this.uriTotalsStatus = uriTotalsStatus;
        this.jobLogTail = jobLogTail;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public HeritrixURITotalsStatus getUriTotalsStatus() {
        return uriTotalsStatus;
    }

    public void setUriTotalsStatus(HeritrixURITotalsStatus uriTotalsStatus) {
        this.uriTotalsStatus = uriTotalsStatus;
    }

    public List<String> getJobLogTail() {
        return jobLogTail;
    }

    public void setJobLogTail(List<String> jobLogTail) {
        this.jobLogTail = jobLogTail;
    }

    public static class HeritrixURITotalsStatus {
        @JsonProperty
        private long downloadedUriCount;
        @JsonProperty
        private long queuedUriCount;
        @JsonProperty
        private long totalUriCount;

        public HeritrixURITotalsStatus() {
        }

        public HeritrixURITotalsStatus(long downloadedUriCount, long queuedUriCount, long totalUriCount) {
            this.downloadedUriCount = downloadedUriCount;
            this.queuedUriCount = queuedUriCount;
            this.totalUriCount = totalUriCount;
        }

        public long getDownloadedUriCount() {
            return downloadedUriCount;
        }

        public void setDownloadedUriCount(long downloadedUriCount) {
            this.downloadedUriCount = downloadedUriCount;
        }

        public long getQueuedUriCount() {
            return queuedUriCount;
        }

        public void setQueuedUriCount(long queuedUriCount) {
            this.queuedUriCount = queuedUriCount;
        }

        public long getTotalUriCount() {
            return totalUriCount;
        }

        public void setTotalUriCount(long totalUriCount) {
            this.totalUriCount = totalUriCount;
        }
    }
}

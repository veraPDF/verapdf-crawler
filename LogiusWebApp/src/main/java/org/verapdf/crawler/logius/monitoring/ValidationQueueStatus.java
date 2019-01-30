package org.verapdf.crawler.logius.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class ValidationQueueStatus {

    private Long count;
    private List<ValidationJob> topDocuments;

    public ValidationQueueStatus() {
    }

    public ValidationQueueStatus(Long count, List<ValidationJob> topDocuments) {
        this.count = count;
        this.topDocuments = topDocuments;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<ValidationJob> getTopDocuments() {
        return topDocuments;
    }

    public void setTopDocuments(List<ValidationJob> documents) {
        this.topDocuments = documents;
    }
}

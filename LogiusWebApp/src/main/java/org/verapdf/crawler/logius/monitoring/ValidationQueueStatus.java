package org.verapdf.crawler.logius.monitoring;

import org.verapdf.crawler.logius.dto.ValidationJobDto;
import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maksim Bezrukov
 */
public class ValidationQueueStatus {

    private Long count;
    private List<ValidationJobDto> topDocuments;

    public ValidationQueueStatus() {
    }

    public ValidationQueueStatus(Long count, List<ValidationJob> topDocuments) {
        this.count = count;
        this.topDocuments = topDocuments.stream()
                                        .map(ValidationJobDto::new)
                                        .collect(Collectors.toList());
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<ValidationJobDto> getTopDocuments() {
        return topDocuments;
    }

    public void setTopDocuments(List<ValidationJobDto> topDocuments) {
        this.topDocuments = topDocuments;
    }
}

package org.verapdf.crawler.logius.report;

import org.verapdf.crawler.logius.validation.error.ValidationError;

import java.util.List;

public class ErrorStatistics {

    public static final int TOP_ERRORS_COUNT = 10;
    private Integer totalCount;
    private List<ErrorCount> topErrorStatistics;
    public ErrorStatistics() {
    }

    public ErrorStatistics(List<ErrorCount> errorStatistics) {
        this.topErrorStatistics = errorStatistics;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<ErrorCount> getTopErrorStatistics() {
        return topErrorStatistics;
    }

    public void setTopErrorStatistics(List<ErrorCount> topErrorStatistics) {
        this.topErrorStatistics = topErrorStatistics;
    }

    public static class ErrorCount {

        public ValidationError error;
        private Long count;

        public ErrorCount() {
        }

        public ErrorCount(ValidationError error, Long count) {
            this.error = error;
            this.count = count;
        }

        public ValidationError getError() {
            return error;
        }

        public void setError(ValidationError error) {
            this.error = error;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

}

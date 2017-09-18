package org.verapdf.crawler.api.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ErrorStatistics {

    private final Integer NUMBER_OF_ERRORS = 10;

    private Integer totalCount;
    private List<ErrorCount> topErrorStatistics;

    public ErrorStatistics() {}

    public ErrorStatistics(List<ErrorCount> errorStatistics) {
        Integer count = 0;
        for(ErrorCount error: errorStatistics) {
            count += error.count;
        }
        totalCount = count;
        this.topErrorStatistics = errorStatistics;
    }

    @JsonProperty
    public Integer getTotalCount() {
        return totalCount;
    }

    @JsonProperty
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    @JsonProperty
    public List<ErrorCount> getTopErrorStatistics() {
        return topErrorStatistics;
    }

    @JsonProperty
    public void setTopErrorStatistics(List<ErrorCount> topErrorStatistics) {
        this.topErrorStatistics = topErrorStatistics;
    }

    public static class ErrorCount implements Comparable<ErrorCount> {

        private String description;
        private Integer count;

        public ErrorCount() {
        }

        public ErrorCount(String description, Integer count) {
            this.description = description;
            this.count = count;
        }

        @Override
        public int compareTo(ErrorCount errorCount) {
            return count.compareTo(errorCount.count);
        }

        @JsonProperty
        public String getDescription() {
            return description;
        }

        @JsonProperty
        public void setDescription(String description) {
            this.description = description;
        }

        @JsonProperty
        public Integer getCount() {
            return count;
        }

        @JsonProperty
        public void setCount(Integer count) {
            this.count = count;
        }
    }
}

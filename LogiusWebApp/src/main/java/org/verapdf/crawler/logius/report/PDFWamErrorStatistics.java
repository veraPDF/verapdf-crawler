package org.verapdf.crawler.logius.report;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class PDFWamErrorStatistics {

    private List<ErrorCount> topErrorStatistics;

    public PDFWamErrorStatistics() {
    }

    public PDFWamErrorStatistics(List<ErrorCount> topErrorStatistics) {
        this.topErrorStatistics = topErrorStatistics;
    }

    public List<ErrorCount> getTopErrorStatistics() {
        return topErrorStatistics;
    }

    public void setTopErrorStatistics(List<ErrorCount> topErrorStatistics) {
        this.topErrorStatistics = topErrorStatistics;
    }

    public static class ErrorCount {
        private String id;
        private Long count;

        public ErrorCount() {
        }

        public ErrorCount(String id, Long count) {
            this.id = id;
            this.count = count;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}

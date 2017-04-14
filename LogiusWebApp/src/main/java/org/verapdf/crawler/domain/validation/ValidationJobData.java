package org.verapdf.crawler.domain.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.verapdf.crawler.domain.report.ValidationError;

import java.util.Map;

public class ValidationJobData {
    private String filepath;
    private String jobDirectory;
    private String uri;
    private String time;

    @JsonIgnore
    public Map<ValidationError, Integer> errorOccurances;

    public ValidationJobData() {}

    @JsonProperty
    public String getFilepath() { return filepath; }

    @JsonProperty
    public void setFilepath(String filepath) { this.filepath = filepath; }

    @JsonProperty
    public String getJobDirectory() { return jobDirectory; }

    @JsonProperty
    public void setJobDirectory(String jobDirectory) { this.jobDirectory = jobDirectory; }

    @JsonProperty
    public String getUri() { return uri; }

    @JsonProperty
    public void setUri(String uri) { this.uri = uri; }

    @JsonProperty
    public String getTime() { return time; }

    @JsonProperty
    public void setTime(String time) { this.time = time; }
}

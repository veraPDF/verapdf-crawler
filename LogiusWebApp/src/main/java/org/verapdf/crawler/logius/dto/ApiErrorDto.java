package org.verapdf.crawler.logius.dto;

import java.util.Collections;
import java.util.List;

public class ApiErrorDto {
    private List<String> errors;


    public ApiErrorDto(String error) {
        this.errors = Collections.singletonList(error);
    }

    public ApiErrorDto(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class FileTypesConfiguration {
    //todo switch to contains
    @Bean(name="fileTypes")
    public Map<String, String> fileTypes() {
        Map<String, String> fileTypes = new HashMap<>();
        fileTypes.put("application/pdf", "pdf");
        fileTypes.put("application/pdf;charset=UTF-8", "pdf");
        fileTypes.put("application/vnd.oasis.opendocument.text", "odt");
        fileTypes.put("application/vnd.oasis.opendocument.spreadsheet", "ods");
        fileTypes.put("application/vnd.oasis.opendocument.presentation", "odp");
        fileTypes.put("application/msword", "doc");
        fileTypes.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        fileTypes.put("application/vnd.ms-powerpoint", "ppt");
        fileTypes.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        fileTypes.put("application/vnd.ms-excel", "xls");
        fileTypes.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

        return fileTypes;
    }
}

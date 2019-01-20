package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PdfProcessorsConfiguration {

    //todo fill?
    @Bean
    public List<PDFProcessorAdapter> pdfProcessors() {
        return new ArrayList<>();
    }
}

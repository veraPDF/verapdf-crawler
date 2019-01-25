package org.verapdf.crawler.logius.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.verapdf.crawler.logius.core.validation.PDFProcessorAdapter;
import org.verapdf.crawler.logius.core.validation.PDFWamProcessor;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PdfProcessorsConfiguration {

    @Bean
    public List<PDFProcessorAdapter> pdfProcessors(@Value("${logius.pdfProcessors.pdfwamChecker}") String pdfwamChecker) {
        List<PDFProcessorAdapter> pdfProcessors = new ArrayList<>();
        pdfProcessors.add(new PDFWamProcessor(pdfwamChecker));
        return pdfProcessors;
    }
}

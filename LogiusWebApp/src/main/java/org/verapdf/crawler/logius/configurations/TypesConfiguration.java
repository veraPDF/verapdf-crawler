package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class TypesConfiguration {

    @Bean("pdfTypes")
    public List<String> pdfTypes(){
        return Arrays.asList("PDF/A", "PDF/UA", "PDF/X", "PDF/E");
    }
}

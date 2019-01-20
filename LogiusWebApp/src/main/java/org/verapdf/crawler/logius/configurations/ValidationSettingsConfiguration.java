package org.verapdf.crawler.logius.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.db.NamespaceDAO;
import org.verapdf.crawler.logius.db.PdfPropertyDAO;
import org.verapdf.crawler.logius.validation.settings.Namespace;
import org.verapdf.crawler.logius.validation.settings.PdfProperty;
import org.verapdf.crawler.logius.validation.settings.ValidationSettings;

import java.util.stream.Collectors;

@Configuration
public class ValidationSettingsConfiguration {

    @Bean
    @Transactional
    public ValidationSettings validationSettings(PdfPropertyDAO pdfPropertyDAO, NamespaceDAO namespaceDAO) {
        ValidationSettings validationSettings = new ValidationSettings();
        validationSettings.setProperties(pdfPropertyDAO.getEnabledPropertiesMap()
                .stream().collect(Collectors.toMap(PdfProperty::getName, PdfProperty::getXpathList)));
        validationSettings.setNamespaces(namespaceDAO.getNamespaces()
                .stream().collect(Collectors.toMap(Namespace::getPrefix, Namespace::getUrl)));

        return validationSettings;
    }
}

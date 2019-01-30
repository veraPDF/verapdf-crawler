package org.verapdf.crawler.logius.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.db.NamespaceDAO;
import org.verapdf.crawler.logius.db.PdfPropertyDAO;
import org.verapdf.crawler.logius.validation.settings.Namespace;
import org.verapdf.crawler.logius.validation.settings.PdfProperty;
import org.verapdf.crawler.logius.validation.settings.ValidationSettings;

import java.util.stream.Collectors;


@Service
public class ValidationSettingsService {
    private final PdfPropertyDAO pdfPropertyDAO;
    private final NamespaceDAO namespaceDAO;
    public ValidationSettingsService(PdfPropertyDAO pdfPropertyDAO, NamespaceDAO namespaceDAO) {
        this.pdfPropertyDAO = pdfPropertyDAO;
        this.namespaceDAO = namespaceDAO;
    }

    @Transactional
    public ValidationSettings getValidationSettings() {
        ValidationSettings validationSettings = new ValidationSettings();
        validationSettings.setProperties(pdfPropertyDAO.getEnabledPropertiesMap()
                .stream().collect(Collectors.toMap(PdfProperty::getName, PdfProperty::getXpathList)));
        validationSettings.setNamespaces(namespaceDAO.getNamespaces()
                .stream().collect(Collectors.toMap(Namespace::getPrefix, Namespace::getUrl)));
        return validationSettings;
    }
}

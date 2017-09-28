package org.verapdf.crawler;

import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.core.heritrix.HeritrixClient;
import org.verapdf.crawler.core.validation.PDFValidator;
import org.verapdf.crawler.db.*;
import org.verapdf.crawler.core.validation.ValidationService;
import org.verapdf.crawler.core.validation.VeraPDFValidator;
import org.verapdf.crawler.resources.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    private final List<Object> resources = new ArrayList<>();

    public ResourceManager(LogiusConfiguration config, HeritrixClient heritrix, HibernateBundle<LogiusConfiguration> hibernate) {
        // Initializing all DAO objects
        CrawlRequestDAO crawlRequestDAO = new CrawlRequestDAO(hibernate.getSessionFactory());
        CrawlJobDAO crawlJobDAO = new CrawlJobDAO(hibernate.getSessionFactory());
        DocumentDAO documentDAO = new DocumentDAO(hibernate.getSessionFactory());
        ValidationJobDAO validationJobDAO = new ValidationJobDAO(hibernate.getSessionFactory());
        ValidationErrorDAO validationErrorDAO = new ValidationErrorDAO(hibernate.getSessionFactory());
        PdfPropertyDAO pdfPropertyDAO = new PdfPropertyDAO(hibernate.getSessionFactory());
        NamespaceDAO namespaceDAO = new NamespaceDAO(hibernate.getSessionFactory());

        // Initializing validators and reporters
        VeraPDFValidator veraPDFValidator = new VeraPDFValidator(config.getVeraPDFServiceConfiguration());
        ValidationService validationService = new UnitOfWorkAwareProxyFactory(hibernate).create(ValidationService.class,
                new Class[]{ValidationJobDAO.class, ValidationErrorDAO.class, DocumentDAO.class, PDFValidator.class},
                new Object[]{validationJobDAO, validationErrorDAO, documentDAO, veraPDFValidator});

        // Initializing resources
        resources.add(new CrawlJobResource(crawlJobDAO, validationJobDAO, heritrix));
        resources.add(new CrawlRequestResource(crawlRequestDAO, crawlJobDAO, heritrix));
        resources.add(new DocumentResource(crawlJobDAO, documentDAO, validationJobDAO));
        resources.add(new DocumentPropertyResource(documentDAO));
        resources.add(new VeraPDFServiceResource(pdfPropertyDAO, namespaceDAO));
        resources.add(new ReportResource(documentDAO));

        // Launching validation
        validationService.start();
    }

    public List<Object> getResources() {
        return Collections.unmodifiableList(this.resources);
    }
}

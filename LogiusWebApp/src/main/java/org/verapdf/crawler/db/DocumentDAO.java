package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.document.DomainDocument;

public class DocumentDAO extends AbstractDAO<DomainDocument> {

    public DocumentDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public DomainDocument save(DomainDocument document) {
        return persist(document);
    }

}

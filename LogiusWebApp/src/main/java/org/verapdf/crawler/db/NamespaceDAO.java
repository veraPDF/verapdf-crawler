package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.validation.settings.Namespace;

import java.util.List;

public class NamespaceDAO extends AbstractDAO<Namespace> {

    public NamespaceDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Namespace> getNamespaces() {
        return list(criteriaQuery());
    }
}

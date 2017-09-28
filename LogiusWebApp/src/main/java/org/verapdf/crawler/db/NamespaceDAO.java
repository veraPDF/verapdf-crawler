package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.validation.settings.Namespace;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class NamespaceDAO extends AbstractDAO<Namespace> {

    public NamespaceDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Namespace> getNamespaces() {
        CriteriaQuery<Namespace> criteriaQuery = criteriaQuery();
        Root<Namespace> namespace = criteriaQuery.from(Namespace.class);
        return list(criteriaQuery);
    }
}

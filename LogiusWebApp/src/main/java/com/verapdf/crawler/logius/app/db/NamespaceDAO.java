package com.verapdf.crawler.logius.app.db;

import com.verapdf.crawler.logius.app.validation.settings.Namespace;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Repository
@Transactional
public class NamespaceDAO extends AbstractDAO<Namespace>{

    @Autowired
    public NamespaceDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Namespace> getNamespaces() {
        CriteriaQuery<Namespace> criteriaQuery = currentSession()
                .getCriteriaBuilder().createQuery(Namespace.class);
        Root<Namespace> namespace = criteriaQuery.from(Namespace.class);
        return currentSession().createQuery(requireNonNull(criteriaQuery)).getResultList();
    }
}

package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.verapdf.crawler.api.crawling.CrawlJob_;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.document.DomainDocument_;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentDAO extends AbstractDAO<DomainDocument> {

    public DocumentDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public DomainDocument save(DomainDocument document) {
        return persist(document);
    }

    public List<String> getDocumentPropertyValues(String propertyName, String domain, String propertyValueFilter, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = builder.createQuery(String.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        MapJoin<DomainDocument, String, String> properties = document.join(DomainDocument_.properties);
        criteriaQuery.select(properties.value()).distinct(true);
        criteriaQuery.where(builder.and(
                builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain),
                builder.equal(properties.key(), propertyName),
                builder.like(properties.value(), "%" + propertyValueFilter + "%")
        ));

        Query<String> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.list();
    }

    public Long count(String domain, List<String> documentTypes, DomainDocument.BaseTestResult testResult, Date startDate) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        criteriaQuery.select(builder.count(document));

        List<Predicate> clauses = new ArrayList<>();
        clauses.add(builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain));
        clauses.add(document.get(DomainDocument_.contentType).in(documentTypes));
        clauses.add(builder.equal(document.get(DomainDocument_.baseTestResult), testResult));
        if (startDate != null) {
            clauses.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }

        criteriaQuery.where(builder.and(clauses.toArray(new Predicate[clauses.size()])));

        return currentSession().createQuery(criteriaQuery).getSingleResult();
    }

}

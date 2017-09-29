package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.verapdf.crawler.api.crawling.CrawlJob_;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.document.DomainDocument_;
import org.verapdf.crawler.api.report.PdfPropertyStatistics;

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

    public List<PdfPropertyStatistics.ValueCount> getPropertyStatistics(String domain, String propertyName, Date startDate, boolean orderByCount, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PdfPropertyStatistics.ValueCount> criteriaQuery = builder.createQuery(PdfPropertyStatistics.ValueCount.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        MapJoin<DomainDocument, String, String> properties = document.join(DomainDocument_.properties);

        Expression<Long> documentCount = builder.count(document);
        criteriaQuery.select(builder.construct(
                PdfPropertyStatistics.ValueCount.class,
                properties.value(),
                documentCount
        ));

        List<Predicate> clauses = new ArrayList<>();
        clauses.add(builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain));
        clauses.add(builder.equal(properties.key(), propertyName));
        clauses.add(builder.notEqual(properties.value(), ""));   // TODO: remove once we don't keep empty property values
        if (startDate != null) {
            clauses.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        criteriaQuery.where(builder.and(clauses.toArray(new Predicate[clauses.size()])));

        criteriaQuery.groupBy(properties.value());

        if (orderByCount) {
            criteriaQuery.orderBy(builder.desc(documentCount));
        }

        Query<PdfPropertyStatistics.ValueCount> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }

        return query.list();
    }

    public Boolean isAllFinishedByDomain(String domain) {
        //TODO: implement me
        return false;
    }
}

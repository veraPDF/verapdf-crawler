package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.verapdf.crawler.api.crawling.CrawlJob_;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.document.DomainDocument_;
import org.verapdf.crawler.api.report.ErrorStatistics;
import org.verapdf.crawler.api.report.PdfPropertyStatistics;
import org.verapdf.crawler.api.validation.error.ValidationError;

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

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(document.get(DomainDocument_.contentType).in(documentTypes));
        if (testResult != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.baseTestResult), testResult));
        }
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

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

    public List<PdfPropertyStatistics.ValueCount> getPropertyStatistics(String domain, String propertyName, Date startDate) {
        return getPropertyStatistics(domain, propertyName, startDate, false, null);
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

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(builder.equal(properties.key(), propertyName));
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

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

    public List<ErrorStatistics.ErrorCount> getErrorsStatistics(String domain, Date startDate, String flavour, String version, String producer, int limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<ErrorStatistics.ErrorCount> criteriaQuery = builder.createQuery(ErrorStatistics.ErrorCount.class);

        // FROM document JOIN validationErrors
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        Join<DomainDocument, ValidationError> error = document.join(DomainDocument_.validationErrors);

        // SELECT error, count(document) as documentCount
        Expression<Long> documentCount = builder.count(document);
        criteriaQuery.select(builder.construct(
                ErrorStatistics.ErrorCount.class,
                error,
                documentCount
        ));

        // WHERE document.job.domain = <domain>
        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.crawlJob).get(CrawlJob_.domain), domain));

        if (startDate != null) {
            // AND document.lastModified >= startDate
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }

        if (flavour != null) {
            // AND document.properties['flavour'] = <flavour>
            MapJoin<DomainDocument, String, String> flavourProperty = document.join(DomainDocument_.properties);
            flavourProperty.on(builder.equal(flavourProperty.key(), PdfPropertyStatistics.FLAVOUR_PROPERTY_NAME));
            restrictions.add(builder.equal(flavourProperty.value(), flavour));
        }

        if (version != null) {
            // AND document.properties['version'] = <version>
            MapJoin<DomainDocument, String, String> versionProperty = document.join(DomainDocument_.properties);
            versionProperty.on(builder.equal(versionProperty.key(), PdfPropertyStatistics.VERSION_PROPERTY_NAME));
            restrictions.add(builder.equal(versionProperty.value(), version));
        }

        if (producer != null) {
            // AND document.properties['producer'] = <producer>
            MapJoin<DomainDocument, String, String> producerProperty = document.join(DomainDocument_.properties);
            producerProperty.on(builder.equal(producerProperty.key(), PdfPropertyStatistics.PRODUCER_PROPERTY_NAME));
            restrictions.add(builder.like(producerProperty.value(), "%" + producer + "%"));
        }

        if (restrictions.size() == 1) {
            criteriaQuery.where(restrictions.get(0));
        } else {
            criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));
        }

        // GROUP BY error
        criteriaQuery.groupBy(error);

        // ORDER BY documentCount DESC
        criteriaQuery.orderBy(builder.desc(documentCount));

        return currentSession().createQuery(criteriaQuery).setMaxResults(limit).list();
    }
}

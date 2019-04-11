package org.verapdf.crawler.logius.db;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.verapdf.crawler.logius.core.validation.PDFWamProcessor;
import org.verapdf.crawler.logius.crawling.CrawlJob_;
import org.verapdf.crawler.logius.document.DomainDocument;
import org.verapdf.crawler.logius.document.DomainDocument_;
import org.verapdf.crawler.logius.model.DocumentId;
import org.verapdf.crawler.logius.model.DocumentId_;
import org.verapdf.crawler.logius.model.Role;
import org.verapdf.crawler.logius.model.User_;
import org.verapdf.crawler.logius.report.ErrorStatistics;
import org.verapdf.crawler.logius.report.PDFWamErrorStatistics;
import org.verapdf.crawler.logius.report.PdfPropertyStatistics;
import org.verapdf.crawler.logius.validation.error.ValidationError;

import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class DocumentDAO extends AbstractDAO<DomainDocument> {

    private static final int PROPERTY_VALUE_LENGTH = 255;
    private final List<String> pdfTypes;

    public DocumentDAO(SessionFactory sessionFactory, @Qualifier("pdfTypes") List<String> pdfTypes) {
        super(sessionFactory);
        this.pdfTypes = pdfTypes;
    }

    @SuppressWarnings("UnusedReturnValue")
    public DomainDocument save(DomainDocument document) {
        Map<String, String> properties = document.getProperties();
        if (properties != null) {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                String currentValue = property.getValue();
                if (currentValue.length() > PROPERTY_VALUE_LENGTH) {
                    property.setValue(currentValue.substring(0, PROPERTY_VALUE_LENGTH));
                }
            }
        }
        return persist(document);
    }

    public Long count(String domain, UUID uuid, List<String> documentTypes, DomainDocument.BaseTestResult testResult, Date startDate) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        criteriaQuery.select(builder.count(document));

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(document.get(DomainDocument_.contentType).in(documentTypes));

        if (testResult != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.baseTestResult), testResult));
        }
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        if (uuid != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), uuid));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[0])));

        return currentSession().createQuery(criteriaQuery).getSingleResult();
    }

    public List<DomainDocument> getDocuments(String domain, UUID id, List<String> documentTypes, DomainDocument.BaseTestResult testResult, Date startDate, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<DomainDocument> criteriaQuery = builder.createQuery(DomainDocument.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(document.get(DomainDocument_.contentType).in(documentTypes));
        if (testResult != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.baseTestResult), testResult));
        }
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        if (id != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), id));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

        Query<DomainDocument> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.list();
    }

    public List<String> getDocumentsUrls(String domain, UUID id, List<String> documentTypes, DomainDocument.BaseTestResult testResult, Date startDate, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<DocumentId> criteriaQuery = builder.createQuery(DocumentId.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        criteriaQuery.select(document.get(DomainDocument_.documentId));

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(document.get(DomainDocument_.contentType).in(documentTypes));
        if (testResult != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.baseTestResult), testResult));
        }
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        if (id != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), id));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

        Query<DocumentId> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.list().stream().map(DocumentId::getDocumentUrl).collect(Collectors.toList());
    }

    public List<String> getDocumentPropertyValues(String propertyName, String domain, String propertyValueFilter, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = builder.createQuery(String.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        MapJoin<DomainDocument, String, String> properties = document.join(DomainDocument_.properties);
        criteriaQuery.select(properties.value()).distinct(true);
        criteriaQuery.where(builder.and(
                builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain),
                builder.equal(properties.key(), propertyName),
                builder.like(properties.value(), "%" + propertyValueFilter + "%")
        ));

        Query<String> query = currentSession().createQuery(criteriaQuery);
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.list();
    }

    public List<PdfPropertyStatistics.ValueCount> getPropertyStatistic(String domain, UUID uuid, Date startDate) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PdfPropertyStatistics.ValueCount> criteriaQuery = builder.createQuery(PdfPropertyStatistics.ValueCount.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        MapJoin<DomainDocument, String, String> properties = document.join(DomainDocument_.properties);
        Expression<Long> documentCount = builder.count(document);
        criteriaQuery.select(builder.construct(
                PdfPropertyStatistics.ValueCount.class,
                properties.key(),
                documentCount
        ));

        List<Predicate> restrictions = new ArrayList<>();
        if (uuid != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), uuid));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(properties.key().in(pdfTypes));
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

        criteriaQuery.groupBy(properties.key());
        Query<PdfPropertyStatistics.ValueCount> query = currentSession().createQuery(criteriaQuery);
        return query.list();
    }

    public List<PdfPropertyStatistics.ValueCount> getPropertyStatistics(String domain, UUID userId, String propertyName, Date startDate) {
        return getPropertyStatistics(domain, userId, propertyName, startDate, false, null);
    }

    public List<PdfPropertyStatistics.ValueCount> getPropertyStatistics(String domain, UUID userId, String propertyName, Date startDate, boolean orderByCount, Integer limit) {
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
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        restrictions.add(builder.equal(properties.key(), propertyName));
        if (userId != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), userId));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
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

    public List<PDFWamErrorStatistics.ErrorCount> getPDFWamErrorsStatistics(String domain, UUID userId, Date startDate, String flavour, String version, String producer) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<PDFWamErrorStatistics.ErrorCount> criteriaQuery = builder.createQuery(PDFWamErrorStatistics.ErrorCount.class);
        Root<DomainDocument> document = criteriaQuery.from(DomainDocument.class);
        MapJoin<DomainDocument, String, String> properties = document.join(DomainDocument_.properties);

        Expression<Long> documentCount = builder.count(document);
        criteriaQuery.select(builder.construct(
                PDFWamErrorStatistics.ErrorCount.class,
                properties.key(),
                documentCount
        ));

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));

        restrictions.add(properties.key().in(PDFWamProcessor.getErrorPropertyNames()));
        restrictions.add(builder.like(properties.value(), "%fail%"));
        restrictions.add(builder.not(builder.like(properties.value(), "%fail:0%")));
        if (userId != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), userId));
        } else {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.role), Role.ANONYMOUS));
        }
        if (startDate != null) {
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }

        if (flavour != null) {
            // AND document.properties['flavour'] = <flavour>
            restrictions.add(getPredicateByFlavour(flavour, document, builder, criteriaQuery));

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
        criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));

        criteriaQuery.groupBy(properties.key());

        criteriaQuery.orderBy(builder.desc(documentCount));

        Query<PDFWamErrorStatistics.ErrorCount> query = currentSession().createQuery(criteriaQuery);

        return query.list();
    }

    public List<ErrorStatistics.ErrorCount> getErrorsStatistics(String domain, UUID userId, Date startDate, String flavour, String version, String producer, int limit) {
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
        restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.domain), domain));
        if (userId != null) {
            restrictions.add(builder.equal(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id), userId));
        } else {
            restrictions.add(builder.isNull(document.get(DomainDocument_.documentId).get(DocumentId_.crawlJob).get(CrawlJob_.user).get(User_.id)));
        }
        if (startDate != null) {
            // AND document.lastModified >= startDate
            restrictions.add(builder.greaterThanOrEqualTo(document.get(DomainDocument_.lastModified), startDate));
        }

        if (flavour != null) {
            // AND document.properties['flavour'] = <flavour>
            restrictions.add(getPredicateByFlavour(flavour, document, builder, criteriaQuery));
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


    private <T> Predicate getPredicateByFlavour(String flavour,
                                                Root<DomainDocument> document,
                                                CriteriaBuilder builder,
                                                CriteriaQuery<T> criteriaQuery) {
        if (!flavour.equals("None")) {
            MapJoin<DomainDocument, String, String> flavourProperty = document.join(DomainDocument_.properties, JoinType.INNER);
            return getPredicateBySpecifiedFlavour(flavour, flavourProperty, builder);
        } else {
            Subquery<DocumentId> subquery = criteriaQuery.subquery(DocumentId.class);
            Root<DomainDocument> root = subquery.from(DomainDocument.class);
            subquery.select(root.get(DomainDocument_.documentId));
            MapJoin<DomainDocument, String, String> join = root.join(DomainDocument_.properties, JoinType.INNER);
            subquery.where(builder.or(pdfTypes.stream()
                    .map(type -> getPredicateBySpecifiedFlavour(type, join, builder))
                    .collect(Collectors.toList()).toArray(new Predicate[pdfTypes.size()])));

            return builder.not(document.get(DomainDocument_.documentId).in(subquery));
        }
    }

    private Predicate getPredicateBySpecifiedFlavour(String flavour,
                                                     MapJoin<DomainDocument, String, String> flavourProperty,
                                                     CriteriaBuilder builder) {
        return builder.equal(flavourProperty.key(), flavour);
    }
}

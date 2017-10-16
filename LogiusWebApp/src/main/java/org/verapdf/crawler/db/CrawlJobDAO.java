package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlJob_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CrawlJobDAO extends AbstractDAO<CrawlJob> {

    public CrawlJobDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CrawlJob getByDomain(String domain) {
        return get(domain);
    }

    public CrawlJob getByHeritrixJobId(String heritrixJobId) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);

        criteriaQuery.where(builder.equal(crawlJob.get(CrawlJob_.heritrixJobId), heritrixJobId));

        return uniqueResult(criteriaQuery);
    }

    public CrawlJob save(CrawlJob crawlJob) {
        return persist(crawlJob);
    }

    public long count(String domainFilter, Boolean finished) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<CrawlJob> crawlJob = query.from(CrawlJob.class);
        query.select(builder.count(crawlJob));

        if (domainFilter != null) {
            query = query.where(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
        }
        if (finished != null) {
            query.where(builder.equal(crawlJob.get(CrawlJob_.finished), finished));
        }

        return this.currentSession().createQuery(query).getSingleResult();
    }

    public List<CrawlJob> find(String domainFilter, Boolean finished, Integer start, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);

        if (domainFilter != null) {
            criteriaQuery.where(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
        }
        if (finished != null) {
            criteriaQuery.where(builder.equal(crawlJob.get(CrawlJob_.finished), finished));
        }
        criteriaQuery.orderBy(builder.desc(crawlJob.get(CrawlJob_.startTime)));

        Query<CrawlJob> query = this.currentSession().createQuery(criteriaQuery);
        if (start != null) {
            query.setFirstResult(start);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return list(query);
    }

    public List<CrawlJob> findByDomain(List<String> domains) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);

        criteriaQuery.where(crawlJob.get(CrawlJob_.domain).in(domains));

        return list(criteriaQuery);
    }

    public List<CrawlJob> findByStatus(CrawlJob.Status status, String afterDomain, int limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> job = criteriaQuery.from(CrawlJob.class);

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(job.get(CrawlJob_.status), status));
        if (afterDomain != null) {
            restrictions.add(builder.greaterThan(job.get(CrawlJob_.domain), afterDomain));
        }

        if (restrictions.size() == 1) {
            criteriaQuery.where(restrictions.get(0));
        } else {
            criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));
        }

        criteriaQuery.orderBy(builder.asc(job.get(CrawlJob_.domain)));

        return currentSession().createQuery(criteriaQuery).setMaxResults(limit).list();
    }

    public void remove(CrawlJob crawlJob) {
        currentSession().delete(crawlJob);
        currentSession().flush();
    }
}

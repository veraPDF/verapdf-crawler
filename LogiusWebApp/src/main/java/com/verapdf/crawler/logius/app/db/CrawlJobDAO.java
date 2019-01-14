package com.verapdf.crawler.logius.app.db;

import com.verapdf.crawler.logius.app.crawling.CrawlJob;
import com.verapdf.crawler.logius.app.crawling.CrawlJob_;
//import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Repository
@Transactional
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
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);
        criteriaQuery.select(builder.count(crawlJob));

        List<Predicate> restrictions = new ArrayList<>();
        if (domainFilter != null) {
            restrictions.add(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
        }
        if (finished != null) {
            restrictions.add(builder.equal(crawlJob.get(CrawlJob_.finished), finished));
        }
        if (restrictions.size() == 1) {
            criteriaQuery.where(restrictions.get(0));
        } else if (!restrictions.isEmpty()) {
            criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));
        }

        return this.currentSession().createQuery(criteriaQuery).getSingleResult();
    }

    public List<CrawlJob> find(String domainFilter, Boolean finished, Integer start, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);

        List<Predicate> restrictions = new ArrayList<>();
        if (domainFilter != null) {
            restrictions.add(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
        }
        if (finished != null) {
            restrictions.add(builder.equal(crawlJob.get(CrawlJob_.finished), finished));
        }

        if (restrictions.size() == 1) {
            criteriaQuery.where(restrictions.get(0));
        } else if (!restrictions.isEmpty()) {
            criteriaQuery.where(builder.and(restrictions.toArray(new Predicate[restrictions.size()])));
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

    public List<CrawlJob> findByStatus(CrawlJob.Status status, CrawlJob.CrawlService crawlService, String afterDomain, int limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> job = criteriaQuery.from(CrawlJob.class);

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(builder.equal(job.get(CrawlJob_.status), status));
        if (crawlService != null) {
            restrictions.add(builder.equal(job.get(CrawlJob_.crawlService), crawlService));
        }
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

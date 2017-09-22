package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.verapdf.crawler.api.crawling.CrawlJob;
import org.verapdf.crawler.api.crawling.CrawlJob_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class CrawlJobDAO extends AbstractDAO<CrawlJob> {

    public CrawlJobDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CrawlJob getByDomain(String domain) {
        return get(domain);
    }

    public CrawlJob save(CrawlJob crawlJob) {
        return persist(crawlJob);
    }

    public long count(String domainFilter) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<CrawlJob> crawlJob = query.from(CrawlJob.class);
        query.select(builder.count(crawlJob));

        if (domainFilter != null) {
            query = query.where(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
        }

        return this.currentSession().createQuery(query).getSingleResult();
    }

    public List<CrawlJob> find(String domainFilter, Integer start, Integer limit) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlJob> criteriaQuery = builder.createQuery(CrawlJob.class);
        Root<CrawlJob> crawlJob = criteriaQuery.from(CrawlJob.class);

        if (domainFilter != null) {
            criteriaQuery.where(builder.like(crawlJob.get(CrawlJob_.domain), "%" + domainFilter + "%"));
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
        criteriaQuery = criteriaQuery.where(crawlJob.get(CrawlJob_.domain).in(domains));
        return list(criteriaQuery);
    }

}

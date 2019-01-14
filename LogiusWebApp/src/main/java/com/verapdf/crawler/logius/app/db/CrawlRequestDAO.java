package com.verapdf.crawler.logius.app.db;

import com.verapdf.crawler.logius.app.crawling.CrawlJob;
import com.verapdf.crawler.logius.app.crawling.CrawlJob_;
import com.verapdf.crawler.logius.app.crawling.CrawlRequest;
import com.verapdf.crawler.logius.app.crawling.CrawlRequest_;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.util.List;

@Repository
@Transactional
public class CrawlRequestDAO extends AbstractDAO<CrawlRequest> {

    public CrawlRequestDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CrawlRequest save(CrawlRequest request) {
        return persist(request);
    }

    public List<CrawlRequest> findActiveRequestsWithoutActiveJobs() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<CrawlRequest> criteriaQuery = builder.createQuery(CrawlRequest.class);
        Root<CrawlRequest> request = criteriaQuery.from(CrawlRequest.class);

        Subquery<String> subquery = criteriaQuery.subquery(String.class);
        Root<CrawlRequest> requestWithActiveJobs = subquery.from(CrawlRequest.class);
        Join<CrawlRequest, CrawlJob> job = requestWithActiveJobs.join(CrawlRequest_.crawlJobs);
        subquery.select(requestWithActiveJobs.get(CrawlRequest_.id));
        subquery.where(builder.and(
                builder.equal(request.get(CrawlRequest_.finished), false),
                builder.equal(job.get(CrawlJob_.finished), false)
        ));

        criteriaQuery.where(builder.and(
                request.get(CrawlRequest_.id).in(subquery).not(),
                builder.equal(request.get(CrawlRequest_.finished), false)
        ));
        return currentSession().createQuery(criteriaQuery).list();
    }
}

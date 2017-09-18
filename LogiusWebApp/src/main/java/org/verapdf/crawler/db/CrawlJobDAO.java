package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.verapdf.crawler.api.crawling.CrawlJob;

import java.util.List;

public class CrawlJobDAO extends AbstractDAO<CrawlJob> {

    public CrawlJobDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<CrawlJob> findByDomain(List<String> domains) {
        Criteria query = criteria().add(Restrictions.in("domain", domains));
        return list(query);
    }

    public CrawlJob save(CrawlJob crawlJob) {
        return persist(crawlJob);
    }
}

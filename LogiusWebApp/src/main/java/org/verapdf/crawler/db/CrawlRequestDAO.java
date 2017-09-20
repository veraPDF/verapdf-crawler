package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.crawling.CrawlRequest;

public class CrawlRequestDAO extends AbstractDAO<CrawlRequest> {
    public CrawlRequestDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CrawlRequest save(CrawlRequest request) {
        return persist(request);
    }
}

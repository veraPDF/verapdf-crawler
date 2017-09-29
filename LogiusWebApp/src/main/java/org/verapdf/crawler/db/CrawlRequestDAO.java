package org.verapdf.crawler.db;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.verapdf.crawler.api.crawling.CrawlRequest;

import java.util.Collections;
import java.util.List;

public class CrawlRequestDAO extends AbstractDAO<CrawlRequest> {
    public CrawlRequestDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public CrawlRequest save(CrawlRequest request) {
        return persist(request);
    }

    public List<CrawlRequest> getUncheckedFinishedCrawlRequestContainingDomain(String domain) {
        //TODO: implement me
        return Collections.emptyList();
    }
}

package org.verapdf.crawler.logius.db.specifications;


import org.springframework.data.jpa.domain.Specification;
import org.verapdf.crawler.logius.crawling.CrawlJob;

import static org.verapdf.crawler.logius.db.specifications.SpecificationUtils.*;

public class CrawlJobSpecification {

    public static Specification<CrawlJob> findByStatusSpecification(CrawlJob.Status status,
                                                                    CrawlJob.CrawlService crawlService,
                                                                    String afterDomain) {
        return Specification.where(equal("status", status))
                .and(equal("crawlService", crawlService))
                .and(greaterThan("afterDomain", afterDomain));
    }

    public static Specification<CrawlJob> findSpecification(String domainFilter, Boolean finished) {
        return Specification.where(like("domainFilter", domainFilter))
                .and(equal("finished", finished));
    }
}
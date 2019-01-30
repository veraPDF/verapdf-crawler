package org.verapdf.crawler.logius.db.specifications;

import org.springframework.data.jpa.domain.Specification;
import org.verapdf.crawler.logius.crawling.CrawlJob;

class SpecificationUtils {

    static Specification<CrawlJob> like(String name, String value) {
        return value == null ? null : (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.like(root.get(name), value);
    }

    static Specification<CrawlJob> equal(String name, Object value) {
        return value == null ? null : (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(name), value);
    }

    static Specification<CrawlJob> greaterThan(String name, String value) {

        return value == null ? null : (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get(name), value);
    }
}

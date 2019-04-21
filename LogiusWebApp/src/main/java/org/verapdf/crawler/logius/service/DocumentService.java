package org.verapdf.crawler.logius.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.verapdf.crawler.logius.crawling.CrawlJob;
import org.verapdf.crawler.logius.db.DocumentDAO;
import org.verapdf.crawler.logius.model.Role;

import java.util.List;

@Service
public class DocumentService {
    private final DocumentDAO documentDAO;

    public DocumentService(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
    }

    @Transactional
    public List<String> findNotFinishedJobsByUserRoleAndStatusAndDownloadCount(Role role, CrawlJob.CrawlService service, int limit) {
        return documentDAO.findJobsByStatusAndDownloadCount(role, service, limit);
    }
}

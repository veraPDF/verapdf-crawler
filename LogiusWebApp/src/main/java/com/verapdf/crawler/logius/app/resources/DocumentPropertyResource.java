package com.verapdf.crawler.logius.app.resources;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.verapdf.crawler.logius.app.db.DocumentDAO;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import java.util.List;

@RestController
@RequestMapping(value = "/document-properties", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentPropertyResource {

    private final DocumentDAO documentDAO;

    @Autowired
    public DocumentPropertyResource(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
    }

    @GetMapping("/{propertyName}/values")
    @Transactional
    public List<String> getDocumentPropertyValues(@PathVariable("propertyName") String propertyName,
                                                  @RequestParam("domain") @NotNull String domain,
                                                  @RequestParam("propertyValueFilter") @NotNull String propertyValueFilter,
                                                  @RequestParam("limit") Integer limit) {
        return documentDAO.getDocumentPropertyValues(propertyName, domain, propertyValueFilter, limit);
    }
}

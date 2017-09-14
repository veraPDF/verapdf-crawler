package org.verapdf.crawler.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.office.OfficeDocumentData;
import org.verapdf.crawler.api.validation.ValidationJobData;
import org.verapdf.crawler.db.document.InsertDocumentDao;
import org.verapdf.crawler.db.jobs.CrawlJobDao;
import org.verapdf.crawler.core.validation.ValidationService;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

@Path("/heritrix")
@Produces(MediaType.APPLICATION_JSON)
public class HeritrixDataResource {

    private static final String[] ODF_SUFFIXES = {".odt", ".ods", ".odp"};
    private static final String[] OFFICE_SUFFIXES = {".doc", ".xls", ".ppt"};
    private static final String[] OOXML_SUFFIXES = {".docx", ".xlsx", ".pptx"};

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final ValidationService service;
    private final CrawlJobDao crawlJobDao;
    private final InsertDocumentDao insertDocumentDao;

    public HeritrixDataResource(ValidationService service, CrawlJobDao crawlJobDao, DataSource dataSource) {
        this.service = service;
        this.crawlJobDao = crawlJobDao;
        this.insertDocumentDao = new InsertDocumentDao(dataSource);
    }

    @POST
    @Path("/pdf")
    public void addValidationJob(ValidationJobData data) {
        logger.info("Received information about PDF file");
        try {
            service.addJob(data);
        }
        catch (IOException e) {
            logger.error("Error on adding file for validation", e);
        }
    }

    @POST
    @Path("/office_document")
    public void addMicrosoftOfficeFile(OfficeDocumentData data) {
        logger.info("Received information about office document " + data.getFileUrl());
        String domain = crawlJobDao.getCrawlUrl(data.getJobId());
        if(stringEndsWithItemFromList(data.getFileUrl(), ODF_SUFFIXES)) {
            insertDocumentDao.addOdfFile(data.getFileUrl(), domain, data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OFFICE_SUFFIXES)) {
            insertDocumentDao.addMicrosoftOfficeFile(data.getFileUrl(), domain, data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OOXML_SUFFIXES)) {
            insertDocumentDao.addOpenOfficeXMLFile(data.getFileUrl(), domain, data.getLastModified());
        }
    }

    private boolean stringEndsWithItemFromList(String string, String[] suffixes) {
        return Arrays.stream(suffixes).parallel().anyMatch(string::endsWith);
    }
}

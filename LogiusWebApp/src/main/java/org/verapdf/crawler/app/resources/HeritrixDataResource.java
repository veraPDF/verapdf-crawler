package org.verapdf.crawler.app.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.crawling.*;
import org.verapdf.crawler.domain.email.EmailAddress;
import org.verapdf.crawler.domain.email.EmailServer;
import org.verapdf.crawler.domain.office.OfficeDocumentData;
import org.verapdf.crawler.domain.report.CrawlJobSummary;
import org.verapdf.crawler.domain.validation.ValidationJobData;
import org.verapdf.crawler.app.email.SendEmail;
import org.verapdf.crawler.app.engine.HeritrixClient;
import org.verapdf.crawler.report.HeritrixReporter;
import org.verapdf.crawler.repository.document.InsertDocumentDao;
import org.verapdf.crawler.repository.jobs.CrawlRequestDao;
import org.verapdf.crawler.repository.jobs.CrawlJobDao;
import org.verapdf.crawler.validation.ValidationService;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Path("/heritrix")
public class HeritrixDataResource {

    private static final String[] ODF_SUFFIXES = {".odt", ".ods", ".odp"};
    private static final String[] OFFICE_SUFFIXES = {".doc", ".xls", ".ppt"};
    private static final String[] OOXML_SUFFIXES = {".docx", ".xlsx", ".pptx"};

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");

    private final ValidationService service;
    private final InsertDocumentDao insertDocumentDao;

    HeritrixDataResource(ValidationService service, DataSource dataSource) {
        this.service = service;
        this.insertDocumentDao = new InsertDocumentDao(dataSource);
    }

    @POST
    @Path("/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
    public void addMicrosoftOfficeFile(OfficeDocumentData data) {
        logger.info("Received information about office document " + data.getFileUrl());
        if(stringEndsWithItemFromList(data.getFileUrl(), ODF_SUFFIXES)) {
            insertDocumentDao.addOdfFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OFFICE_SUFFIXES)) {
            insertDocumentDao.addMicrosoftOfficeFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
        if(stringEndsWithItemFromList(data.getFileUrl(), OOXML_SUFFIXES)) {
            insertDocumentDao.addOpenOfficeXMLFile(data.getFileUrl(), data.getJobId(), data.getLastModified());
        }
    }

    private boolean stringEndsWithItemFromList(String string, String[] suffixes) {
        return Arrays.stream(suffixes).parallel().anyMatch(string::endsWith);
    }
}

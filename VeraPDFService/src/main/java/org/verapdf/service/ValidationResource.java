package org.verapdf.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationSettings;
import org.verapdf.crawler.api.validation.VeraPDFServiceStatus;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ValidationResource {

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private final ExecutorService service = Executors.newFixedThreadPool(1);
    private final String veraPDFPath;
    private VeraPDFProcessor veraPDFProcessor;
    private ValidationSettings validationSettings;
    private VeraPDFValidationResult validationResult;

    ValidationResource(String veraPDFPath, ValidationSettings validationSettings) throws IOException {
        this.validationSettings = validationSettings;
        this.veraPDFPath = veraPDFPath;
    }

    @POST
    @Path("/settings")
    @Timed
    public void setValidationSettings(ValidationSettings validationSettings) {
        this.validationSettings = validationSettings;
    }

    @POST
    @Timed
    public Response processValidateRequest(String filename) throws InterruptedException {
        logger.info("Starting processing of " + filename);
        synchronized (this) {
            if (evaluateStatus() == VeraPDFServiceStatus.ProcessorStatus.ACTIVE) {
                return Response.status(HttpStatus.SC_LOCKED).build();
            }
        }
        validate(filename);
        return Response.accepted().build();
    }

    @GET
    @Timed
    public VeraPDFServiceStatus getStatus() {
        VeraPDFServiceStatus.ProcessorStatus processorStatus = evaluateStatus();
        logger.info("Status requested, processorStatus is " + processorStatus);
        return new VeraPDFServiceStatus(processorStatus, validationResult);
    }

    @DELETE
    @Timed
    public void discardCurrentJob() {
        logger.info("Terminating current job");
        if (this.veraPDFProcessor != null) {
            this.veraPDFProcessor.stopProcess();
            this.veraPDFProcessor = null;
        }
        validationResult = null;
    }

    private void validate(String filename) {
        this.veraPDFProcessor = new VeraPDFProcessor(veraPDFPath, filename, this, this.validationSettings);
        service.submit(veraPDFProcessor);
    }

    void validationFinished(VeraPDFValidationResult result) {
        this.validationResult = result;
        this.veraPDFProcessor = null;
        //TODO: send message to main service
    }

    private VeraPDFServiceStatus.ProcessorStatus evaluateStatus() {
        if (this.veraPDFProcessor != null) {
            return VeraPDFServiceStatus.ProcessorStatus.ACTIVE;
        } else {
            return validationResult == null ? VeraPDFServiceStatus.ProcessorStatus.IDLE : VeraPDFServiceStatus.ProcessorStatus.FINISHED;
        }
    }
}

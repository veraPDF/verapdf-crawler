package org.verapdf.service;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.api.validation.ValidationSettings;
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
            if (evaluateStatus() == Status.ACTIVE) {
                return Response.status(102).build();
            }
        }
        validate(filename);
        return Response.accepted().build();
    }

    @GET
    @Timed
    public Response getStatus() {
        Status status = evaluateStatus();
        logger.info("Status requested, status is " + status);
        if(status == Status.ACTIVE) {
            return Response.status(102).build();
        }
        if(status == Status.FINISHED) {
            return Response.ok(validationResult).build();
        }
        return Response.status(100).build();
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

    private Status evaluateStatus() {
        if (this.veraPDFProcessor != null) {
            return Status.ACTIVE;
        } else {
            return validationResult == null ? Status.IDLE : Status.FINISHED;
        }
    }

    private enum Status {
        IDLE,
        ACTIVE,
        FINISHED
    }
}

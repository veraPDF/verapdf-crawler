package org.verapdf.service;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.VeraPDFValidationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ValidationResource {

    private final static String STATUS_IDLE = "idle";
    private final static String STATUS_ACTIVE = "active";
    private final static String STATUS_FINISHED = "finished";

    private static final Logger logger = LoggerFactory.getLogger("CustomLogger");
    private Map<String, String> validationSettings;
    private String status;
    private VeraPDFValidationResult validationResult;

    ValidationResource() {
        validationSettings = new HashMap<>();
        status = STATUS_IDLE;
    }

    @POST
    @Path("/properties")
    @Timed
    public void setValidationSettings(Map<String, String> validationSettings) {
        this.validationSettings = validationSettings;
    }

    @POST
    @Timed
    public void processValidateRequest(String filename) throws InterruptedException {
        logger.info("Starting processing of " + filename);
        if(status.equals(STATUS_ACTIVE)) {
            discardCurrentJob();
        }
        validate(filename);
    }

    @GET
    @Timed
    public Response getStatus() {
        if(status.equals(STATUS_ACTIVE)) {
            return Response.status(102).build();
        }
        if(status.equals(STATUS_FINISHED)) {
            return Response.ok(validationResult).build();
        }
        return Response.status(100).build();
    }

    @DELETE
    @Timed
    public void discardCurrentJob() {
        logger.info("Terminating current job");
        //?? veraPDF.stop();
        status = STATUS_IDLE;
        validationResult = null;
    }

    private void validate(String filename) throws InterruptedException {
        status = STATUS_ACTIVE;

        validationResult = new VeraPDFValidationResult();
        validationResult.setValidationErrors(new ArrayList<>());
        validationResult.setValid(true);
        for(Map.Entry<String, String> property: validationSettings.entrySet()) {
            validationResult.addProperty(property.getKey(), property.getValue());
        }
        Thread.sleep(1000);

        status = STATUS_FINISHED;
    }
}

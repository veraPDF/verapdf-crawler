package org.verapdf.service;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.domain.validation.VeraPDFValidationResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
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
        status = STATUS_ACTIVE;
        this.validationResult = validate(filename);
        status = STATUS_FINISHED;
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

    private VeraPDFValidationResult validate(String filename) {
        File xmlReport;
        try {
            xmlReport = File.createTempFile("veraPDF-tempXMLReport", ".xml"); //$NON-NLS-1$//$NON-NLS-2$
            xmlReport.deleteOnExit();
        } catch (IOException e) {
            return generateProcessingErrorResult("Can not generate temp file for report. Error: " + e.getMessage());
        }
        try (OutputStream mrrReport = new FileOutputStream(xmlReport)) {
            ProcessType processType = veraAppConfig.getProcessType();
            EnumSet<TaskType> tasks = processType.getTasks();
            ValidatorConfig validatorConfig = this.configManager.getValidatorConfig();
            FeatureExtractorConfig featuresConfig = this.configManager.getFeaturesConfig();
            ProcessorConfig resultConfig = ProcessorFactory.fromValues(validatorConfig, featuresConfig,
                    this.configManager.getPluginsCollectionConfig(), this.configManager.getFixerConfig(), tasks,
                    veraAppConfig.getFixesFolder())
            try (BatchProcessor processor = ProcessorFactory.fileBatchProcessor(resultConfig);) {
                VeraAppConfig applicationConfig = this.configManager.getApplicationConfig();
                BatchSummary batchSummary = processor.process(this.pdfs,
                        ProcessorFactory.getHandler(FormatOption.MRR, applicationConfig.isVerbose(), mrrReport,
                                applicationConfig.getMaxFailsDisplayed(), validatorConfig.isRecordPasses()));
            }
        } catch (IOException e) {

        } catch (VeraPDFException e) {

        }
    }

    private static VeraPDFValidationResult generateProcessingErrorResult(String errorMessage) {
        VeraPDFValidationResult res = new VeraPDFValidationResult();
        res.setValid(false);
        res.setProcessingError(errorMessage);
        return res;
    }
}

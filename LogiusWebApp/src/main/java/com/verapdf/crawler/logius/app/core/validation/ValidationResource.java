package com.verapdf.crawler.logius.app.core.validation;


import com.verapdf.crawler.logius.app.validation.VeraPDFServiceStatus;
import com.verapdf.crawler.logius.app.validation.settings.Namespace;
import com.verapdf.crawler.logius.app.validation.settings.PdfProperty;
import com.verapdf.crawler.logius.app.validation.settings.ValidationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.verapdf.crawler.logius.app.validation.VeraPDFValidationResult;
import com.verapdf.crawler.logius.app.db.NamespaceDAO;
import com.verapdf.crawler.logius.app.db.PdfPropertyDAO;


import javax.transaction.Transactional;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ValidationResource {

    private static final Logger logger = LoggerFactory.getLogger(ValidationResource.class);

    private final ExecutorService service = Executors.newFixedThreadPool(1);
    @Value("${path}")
    private String veraPDFPath;
    private final File veraPDFErrorLog;
    private VeraPDFProcessor veraPDFProcessor;
    private VeraPDFValidationResult validationResult;
    @Value("${path}")
    private String veraPDFErrorFilePath;
    private boolean isAborted = false;

    private final PdfPropertyDAO pdfPropertyDAO;
    private final NamespaceDAO namespaceDAO;

    public ValidationResource(@Value("${veraPDFService.verapdfPath}") String veraPDFPath, @Value("${veraPDFService.verapdfPath}") String veraPDFErrorFilePath,
                              PdfPropertyDAO pdfPropertyDAO, NamespaceDAO namespaceDAO) {
        this.veraPDFPath = veraPDFPath;
        this.veraPDFErrorFilePath = veraPDFErrorFilePath;
        this.pdfPropertyDAO = pdfPropertyDAO;
        this.namespaceDAO = namespaceDAO;
        this.veraPDFErrorLog = new File(veraPDFErrorFilePath);
    }

    @Transactional
    public boolean processValidateRequest(String filename) {
        logger.info("Starting processing of " + filename);
        synchronized (this) {
            if (evaluateStatus() == VeraPDFServiceStatus.ProcessorStatus.ACTIVE) {
                return false;
            }
        }
        isAborted = false;
        validate(filename);
        return true;
    }


    public VeraPDFServiceStatus getStatus() {
        VeraPDFServiceStatus.ProcessorStatus processorStatus = evaluateStatus();
        logger.info("Status requested, processorStatus is " + processorStatus);
        return new VeraPDFServiceStatus(processorStatus, validationResult);
    }

    public void discardCurrentJob() {
        logger.info("Terminating current job");
        if (this.veraPDFProcessor != null) {
            this.veraPDFProcessor.stopProcess();
            this.veraPDFProcessor = null;
        }
        validationResult = null;
        isAborted = true;
    }


    public void validate(String filename) {
        this.veraPDFProcessor = new VeraPDFProcessor(veraPDFPath, veraPDFErrorLog, filename, this, generateValidationSettings());
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
        } else if (validationResult != null) {
            return VeraPDFServiceStatus.ProcessorStatus.FINISHED;
        } else {
            return isAborted ? VeraPDFServiceStatus.ProcessorStatus.ABORTED : VeraPDFServiceStatus.ProcessorStatus.IDLE;
        }
    }

    public ValidationSettings generateValidationSettings() {
        ValidationSettings validationSettings = new ValidationSettings();
        validationSettings.setProperties(pdfPropertyDAO.getEnabledPropertiesMap().stream().collect(Collectors.toMap(PdfProperty::getName, PdfProperty::getXpathList)));
        validationSettings.setNamespaces(namespaceDAO.getNamespaces().stream().collect(Collectors.toMap(Namespace::getPrefix, Namespace::getUrl)));
        return validationSettings;
    }
}

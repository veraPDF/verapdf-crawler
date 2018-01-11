package org.verapdf.crawler.core.validation;

import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPDateTimeFactory;
import com.adobe.xmp.XMPException;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.api.document.DomainDocument;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.api.validation.error.ValidationError;
import org.verapdf.crawler.configurations.PDFProcessorsConfiguration;
import org.verapdf.crawler.tools.AbstractService;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ValidationService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private static final String PROPERTY_NAME_MOD_DATE_XMP = "modDateXMP";
	private static final String PROPERTY_NAME_MOD_DATE_INFO_DICT = "modDateInfoDict";

	private static final long SLEEP_DURATION = 60*1000;

	private List<PDFProcessorAdapter> pdfProcessors;
    private final ResourceManager resourceManager;
    private final PDFValidator validator;
    private ValidationJob currentJob;

    public ValidationService(ResourceManager resourceManager, PDFValidator validator) {
        super("ValidationService", SLEEP_DURATION);
    	this.resourceManager = resourceManager;
        this.validator = validator;
        this.pdfProcessors = new ArrayList<>();
		PDFProcessorsConfiguration pdfProcessorsConfiguration = resourceManager.getPDFProcessorsConfiguration();
		if (pdfProcessorsConfiguration != null) {
			String pdfwamChecker = pdfProcessorsConfiguration.getPdfwamChecker();
			if (pdfwamChecker != null) {
				this.pdfProcessors.add(new PDFWamProcessor(pdfwamChecker));
			}
		}
    }

	public ValidationJob getCurrentJob() {
		return currentJob;
	}

	private void setCurrentJob(ValidationJob currentJob) {
    	synchronized (ValidationService.class) {
			this.currentJob = currentJob;
		}
	}

    public void abortCurrentJob() {
		try {
			logger.info("Aborting current job");
			currentJob.setStatus(ValidationJob.Status.ABORTED);
			validator.terminateValidation();
		} catch (IOException e) {
			logger.error("Can't terminate current job", e);
		}
	}

	@Override
	protected void onStart() throws InterruptedException, ValidationDeadlockException {
		setCurrentJob(retrieveCurrentJob());
		if (currentJob != null) {
			try {
				processStartedJob();
			} catch (IOException e) {
				saveErrorResult(e);
			}
		}
	}

	@Override
	protected boolean onRepeat() throws ValidationDeadlockException, InterruptedException {
		setCurrentJob(retrieveNextJob());
		if (currentJob != null) {
			logger.info("Validating " + currentJob.getId());
			try {
				validator.startValidation(currentJob);
				processStartedJob();
			} catch (IOException e) {
				saveErrorResult(e);
			}
			return false;
		}
		return true;
	}

	private void processStartedJob() throws IOException, ValidationDeadlockException, InterruptedException {
        VeraPDFValidationResult result = validator.getValidationResult(currentJob);
        // additional processors logic
        for (PDFProcessorAdapter pdfProcessor : this.pdfProcessors) {
			Map<String, String> properties = pdfProcessor.evaluateProperties(currentJob);
			for (Map.Entry<String, String> property : properties.entrySet()) {
				result.addProperty(property.getKey(), property.getValue());
			}
		}
        saveResult(result);
    }

    private void saveErrorResult(Throwable e) {
        VeraPDFValidationResult result = new VeraPDFValidationResult(e.getMessage());
        saveResult(result);
    }

    @SuppressWarnings("WeakerAccess") // @UnitOfWork works only with public methods
    @UnitOfWork
    public ValidationJob retrieveNextJob() {
		logger.debug("Getting next job");
        ValidationJob job = resourceManager.getValidationJobDAO().next();
        if (job != null) {
            job.setStatus(ValidationJob.Status.IN_PROGRESS);
        }
        return job;
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public ValidationJob retrieveCurrentJob() {
		logger.debug("Getting current job");
        return resourceManager.getValidationJobDAO().current();
    }

    @SuppressWarnings("WeakerAccess")
    @UnitOfWork
    public void saveResult(VeraPDFValidationResult result) {
		boolean shouldCleanDB = false;
    	try {
            if (!currentJob.getStatus().equals(ValidationJob.Status.ABORTED)) {
            	shouldCleanDB = true;
				logger.debug("Saving validation job results");
                DomainDocument document = currentJob.getDocument();
                document.setBaseTestResult(result.getTestResult());

                // Save errors where needed
                List<ValidationError> validationErrors = result.getValidationErrors();
                for (int index = 0; index < validationErrors.size(); index++) {
                    validationErrors.set(index, resourceManager.getValidationErrorDAO().save(validationErrors.get(index)));
                }
                document.setValidationErrors(validationErrors);

                // Link properties and modification date
				Map<String, String> properties = result.getProperties();
				String modDateXMP = null;
				String modDateInfoDict = null;
				if (properties.containsKey(PROPERTY_NAME_MOD_DATE_XMP)) {
					modDateXMP = properties.get(PROPERTY_NAME_MOD_DATE_XMP);
					properties.remove(PROPERTY_NAME_MOD_DATE_XMP);
				}
				if (properties.containsKey(PROPERTY_NAME_MOD_DATE_INFO_DICT)) {
					modDateInfoDict = properties.get(PROPERTY_NAME_MOD_DATE_INFO_DICT);
					properties.remove(PROPERTY_NAME_MOD_DATE_INFO_DICT);
				}
				Date modDate = getModDate(modDateXMP, modDateInfoDict);
				if (modDate != null) {
					document.setLastModified(modDate);
				}
				document.setProperties(properties);

                // And update document (note that document was detached from hibernate context, thus we need to save explicitly)
				resourceManager.getDocumentDAO().save(document);
            } else {
            	logger.debug("Validation job was aborted, don't save any results");
			}
        } finally {
            cleanJob(currentJob, shouldCleanDB);
        }
    }

    private static Date getModDate(String fromXMP, String fromInfoDict) {
		if (fromXMP != null) {
			try {
				XMPDateTime fromISO8601 = XMPDateTimeFactory.createFromISO8601(fromXMP);
				return fromISO8601.getCalendar().getTime();
			} catch (XMPException e) {
				return null;
			}
		} else if (fromInfoDict != null) {
			try {
				XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(fromInfoDict);
				return xmlGregorianCalendar.toGregorianCalendar().getTime();
			} catch (DatatypeConfigurationException e) {
				return null;
			}
		} else {
			return null;
		}
	}

    private void cleanJob(ValidationJob job, boolean shouldCleanDB) {
    	logger.debug("Cleanup validation job");
		if (job == null) {
			return;
		}
    	if (job.getFilePath() != null) {
            if (!new File(job.getFilePath()).delete()) {
                logger.warn("Failed to clean validation job file " + job.getFilePath());
            }
        }
        if (shouldCleanDB) {
			resourceManager.getValidationJobDAO().remove(job);
		}
    }
}
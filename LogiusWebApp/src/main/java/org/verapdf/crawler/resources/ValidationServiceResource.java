package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.verapdf.crawler.ResourceManager;
import org.verapdf.crawler.api.monitoring.ValidationQueueStatus;
import org.verapdf.crawler.api.validation.ValidationJob;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.api.validation.settings.Namespace;
import org.verapdf.crawler.api.validation.settings.PdfProperty;
import org.verapdf.crawler.api.validation.settings.ValidationSettings;
import org.verapdf.crawler.db.NamespaceDAO;
import org.verapdf.crawler.db.PdfPropertyDAO;
import org.verapdf.crawler.db.ValidationJobDAO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maksim Bezrukov
 */
@Path("/validation-service")
@Produces(MediaType.APPLICATION_JSON)
public class ValidationServiceResource {

	private final ResourceManager resourceManager;

	public ValidationServiceResource(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@GET
	@Path("/settings")
	@UnitOfWork
	public ValidationSettings getValidationSettings() {
		ValidationSettings validationSettings = new ValidationSettings();
        validationSettings.setProperties(resourceManager.getPdfPropertyDAO().getEnabledPropertiesMap().stream().collect(Collectors.toMap(PdfProperty::getName, PdfProperty::getXpathList)));
        validationSettings.setNamespaces(resourceManager.getNamespaceDAO().getNamespaces().stream().collect(Collectors.toMap(Namespace::getPrefix, Namespace::getUrl)));
		return validationSettings;
	}

	@GET
	@Path("/queue-status")
	@UnitOfWork
	public ValidationQueueStatus getQueueStatus() {
		Long count = resourceManager.getValidationJobDAO().count(null);
		List<ValidationJob> documents = resourceManager.getValidationJobDAO().getDocuments(null, 10);
		return new ValidationQueueStatus(count, documents);
	}

	@POST
	@Path("/result")
	public void setValidationResult(@NotNull @Valid VeraPDFValidationResult result) {
		// todo: interrupt sleep
	}
}

package org.verapdf.crawler.resources;

import io.dropwizard.hibernate.UnitOfWork;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.api.validation.settings.Namespace;
import org.verapdf.crawler.api.validation.settings.PdfProperty;
import org.verapdf.crawler.api.validation.settings.ValidationSettings;
import org.verapdf.crawler.db.NamespaceDAO;
import org.verapdf.crawler.db.PdfPropertyDAO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.stream.Collectors;

/**
 * @author Maksim Bezrukov
 */
@Path("/verapdf-service")
@Produces(MediaType.APPLICATION_JSON)
public class VeraPDFServiceResource {

	private final PdfPropertyDAO pdfPropertyDAO;
    private final NamespaceDAO namespaceDAO;

	public VeraPDFServiceResource(PdfPropertyDAO pdfPropertyDAO, NamespaceDAO namespaceDAO) {
		this.pdfPropertyDAO = pdfPropertyDAO;
        this.namespaceDAO = namespaceDAO;
	}

	@GET
	@Path("/settings")
	@UnitOfWork
	public ValidationSettings getValidationSettings() {
		ValidationSettings validationSettings = new ValidationSettings();
        validationSettings.setProperties(pdfPropertyDAO.getEnabledPropertiesMap().stream().collect(Collectors.toMap(PdfProperty::getName, PdfProperty::getXpathList)));
        validationSettings.setNamespaces(namespaceDAO.getNamespaces().stream().collect(Collectors.toMap(Namespace::getPrefix, Namespace::getUrl)));
		return validationSettings;
	}

	@POST
	@Path("/result")
	public void setValidationResult(@NotNull @Valid VeraPDFValidationResult result) {
		// todo: interrupt sleep
	}
}

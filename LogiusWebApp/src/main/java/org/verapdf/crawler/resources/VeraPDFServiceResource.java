package org.verapdf.crawler.resources;

import org.verapdf.crawler.api.validation.ValidationSettings;
import org.verapdf.crawler.api.validation.VeraPDFValidationResult;
import org.verapdf.crawler.db.document.ValidatedPDFDao;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Maksim Bezrukov
 */
@Path("/verapdf-service")
@Produces(MediaType.APPLICATION_JSON)
public class VeraPDFServiceResource {

	private final ValidatedPDFDao validatedPDFDao;

	public VeraPDFServiceResource(ValidatedPDFDao validatedPDFDao) {
		this.validatedPDFDao = validatedPDFDao;
	}

	@GET
	@Path("/settings")
	public ValidationSettings getValidationSettings() {
		return new ValidationSettings(validatedPDFDao.getPdfPropertiesWithXpath(), validatedPDFDao.getNamespaceMap());
	}

	@POST
	@Path("/result")
	public void setValidationResult(VeraPDFValidationResult result) {
		// todo: interrupt sleep
	}
}

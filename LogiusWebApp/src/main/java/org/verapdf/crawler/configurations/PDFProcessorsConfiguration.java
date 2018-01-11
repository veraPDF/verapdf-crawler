package org.verapdf.crawler.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Maksim Bezrukov
 */
public class PDFProcessorsConfiguration {

	@NotEmpty
	private String pdfwamChecker;

	public PDFProcessorsConfiguration() {
	}

	@JsonProperty
	public String getPdfwamChecker() {
		return pdfwamChecker;
	}

	@JsonProperty
	public void setPdfwamChecker(String pdfwamChecker) {
		this.pdfwamChecker = pdfwamChecker;
	}
}

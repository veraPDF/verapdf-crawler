package com.verapdf.crawler.logius.app.core.validation;

import com.verapdf.crawler.logius.app.validation.ValidationJob;

import java.util.Collections;
import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
public abstract class PDFProcessorAdapter {

	public Map<String, String> evaluateProperties(ValidationJob job) {
		return Collections.emptyMap();
	}
}

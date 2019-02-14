package org.verapdf.crawler.logius.core.validation;

import org.verapdf.crawler.logius.validation.ValidationJob;

import java.util.Collections;
import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
public abstract class PDFProcessorAdapter {

    public abstract Map<String, String> evaluateProperties(String filepath);
}

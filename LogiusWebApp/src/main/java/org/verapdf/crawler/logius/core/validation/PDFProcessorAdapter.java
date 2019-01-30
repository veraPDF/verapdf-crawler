package org.verapdf.crawler.logius.core.validation;

import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
public abstract class PDFProcessorAdapter {

    public abstract Map<String, String> evaluateProperties(String filePath);
}

package org.verapdf.crawler.logius.exception;

public class HeritrixException extends RuntimeException {

    public HeritrixException(String message) {
        super(message);
    }

    public HeritrixException(Throwable e) {
        super(e);
    }
}

package org.verapdf.crawler.logius.exception;

public class AlreadyExistsException extends BadRequestException{

    public AlreadyExistsException(String message) {
        super(message);
    }
}

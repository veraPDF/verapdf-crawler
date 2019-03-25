package org.verapdf.crawler.logius.resources;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.verapdf.crawler.logius.dto.ApiErrorDto;
import org.verapdf.crawler.logius.exception.AlreadyExistsException;
import org.verapdf.crawler.logius.exception.IncorrectPasswordException;
import org.verapdf.crawler.logius.exception.NotFoundException;

import java.util.stream.Collectors;

@Configuration
@ControllerAdvice
public class ExceptionHander extends ResponseEntityExceptionHandler {

    @ExceptionHandler({NotFoundException.class, AlreadyExistsException.class, IncorrectPasswordException.class})
    public ResponseEntity handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiErrorDto(ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return ResponseEntity.badRequest().body(new ApiErrorDto(ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        ApiErrorDto dto = new ApiErrorDto(ex.getBindingResult().getAllErrors()
                .stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList()));
        return ResponseEntity.status(status).body(dto);
    }
}
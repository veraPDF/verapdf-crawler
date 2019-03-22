package org.verapdf.crawler.logius.resources;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.verapdf.crawler.logius.dto.ApiErrorDto;
import org.verapdf.crawler.logius.exception.AlreadyExistsException;
import org.verapdf.crawler.logius.exception.IncorrectPasswordException;
import org.verapdf.crawler.logius.exception.NotFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

@Configuration
@ControllerAdvice
public class ExceptionHander extends ResponseEntityExceptionHandler {


    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity handleNotFound(NotFoundException ex) {
        return new ResponseEntity<>(new ApiErrorDto(ex.getMessage()), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler({AlreadyExistsException.class, IncorrectPasswordException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity handleBadRequest(Exception ex) {
        return new ResponseEntity<>(new ApiErrorDto(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return new ResponseEntity<>(new ApiErrorDto(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        ApiErrorDto dto = new ApiErrorDto(ex.getBindingResult().getAllErrors()
                .stream().map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList()));
        return new ResponseEntity<>(dto, headers, status);
    }
}
package org.verapdf.crawler.logius.resources;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.dto.ApiErrorDto;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@RestController
public class ErrorControllerImpl implements ErrorController {
    private final static String ERROR_PATH = "/error";
    private final static URI ROOT_PAGE_PATH = URI.create("/");

    @RequestMapping(ERROR_PATH)
    @ResponseBody
    public ResponseEntity handleError(HttpServletRequest request) {
        String path = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
        return path.startsWith("/api/") ? restResponse(request) : htmlResponse();
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    private ResponseEntity restResponse(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String exception = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new ApiErrorDto(exception), headers, HttpStatus.valueOf(statusCode));
    }


    private ResponseEntity htmlResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setLocation(ROOT_PAGE_PATH);
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }
}
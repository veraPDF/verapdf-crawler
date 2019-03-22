package org.verapdf.crawler.logius.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.verapdf.crawler.logius.dto.ApiErrorDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class AuthHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    public AuthHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        handleError(response, HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        handleError(response, HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

    private void handleError(HttpServletResponse response, int responseType, String message) throws IOException {
        response.resetBuffer();
        OutputStream out = response.getOutputStream();
        response.setStatus(responseType);
        mapper.writeValue(out, new ApiErrorDto(message));
        response.flushBuffer();
    }
}

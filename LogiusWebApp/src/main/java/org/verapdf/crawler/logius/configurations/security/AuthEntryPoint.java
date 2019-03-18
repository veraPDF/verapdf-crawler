package org.verapdf.crawler.logius.configurations.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.verapdf.crawler.logius.dto.ApiErrorDto;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    public AuthEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        OutputStream out = httpServletResponse.getOutputStream();
        httpServletResponse.setStatus(401);
        mapper.writeValue(out, new ApiErrorDto(e.getMessage()));
        out.flush();
    }
}
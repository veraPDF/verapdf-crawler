package org.verapdf.crawler.logius.configurations.security;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class TokenAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return getToken(request.getHeader("Authorization"));
    }

    @Override
    protected String getPreAuthenticatedCredentials(HttpServletRequest request) {
        return getToken(request.getHeader("Authorization"));
    }

    private String getToken(String header) {
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
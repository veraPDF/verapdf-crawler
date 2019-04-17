package org.verapdf.crawler.logius.resources.util;


import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;

import java.util.UUID;

@Service
public class ControllerHelper {

    public UUID getUserUUID(TokenUserDetails principal) {
        return principal == null ? null : principal.getUuid();
    }
}

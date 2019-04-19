package org.verapdf.crawler.logius.resources;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.verapdf.crawler.logius.dto.TokenDto;
import org.verapdf.crawler.logius.dto.user.TokenUserDetails;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/token")
    public TokenDto getToken(@AuthenticationPrincipal TokenUserDetails principal) {
        return new TokenDto(principal.getToken());
    }
}
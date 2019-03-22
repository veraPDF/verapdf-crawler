package org.verapdf.crawler.logius.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.verapdf.crawler.logius.model.User;

import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {
    @Value("${logius.jwtExpirationInMs}")
    private int jwtExpirationInMs;
    private byte[] jwtSecret;

    @Autowired
    public TokenService(@Value("${logius.jwtSecret}") String jwtSecret) {
        this.jwtSecret = jwtSecret.getBytes();
    }

    public DecodedJWT verify(String token, byte[] userSecret) {
        Algorithm algorithm = Algorithm.HMAC256(mergeSecrets(jwtSecret, userSecret));
        return JWT.require(algorithm).build().verify(token);
    }

    public String encode(User user) {
        try {
            Instant now = Instant.now();
            Algorithm algorithm = Algorithm.HMAC256(mergeSecrets(jwtSecret, user.getSecret()));
            return JWT.create()
                    .withSubject(user.getEmail())
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(jwtExpirationInMs)))
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new IllegalArgumentException("Cannot properly create token", ex);
        }
    }

    public String getUserEmailFromJWT(String token) {
        return JWT.decode(token).getSubject();
    }

    private byte[] mergeSecrets(byte[] secret, byte[] userSecret) {
        byte[] concatBytes = ArrayUtils.addAll(secret, userSecret);
        return DigestUtils.sha256(concatBytes);
    }
}
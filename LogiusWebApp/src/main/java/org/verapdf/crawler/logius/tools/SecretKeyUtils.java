package org.verapdf.crawler.logius.tools;

import java.security.SecureRandom;

public class SecretKeyUtils {

    public static byte[] generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytes;
    }
}

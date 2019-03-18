package org.verapdf.crawler.logius.tools;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SecretKeyUtils {

    public static String generateSecret() {
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[32];
        random.nextBytes(r);
        return Base64.encodeBase64String(r);
    }
}

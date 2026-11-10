package org.esercizi.expensetrackerapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Token opachi monouso: in email va il valore grezzo, nel database solo l'hash SHA-256. */
public final class OpaqueTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OpaqueTokens() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 è garantito su ogni JVM (JCA): non può accadere.
            throw new IllegalStateException(e);
        }
    }
}

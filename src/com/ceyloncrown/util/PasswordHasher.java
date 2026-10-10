package com.ceyloncrown.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PBKDF2-HMAC-SHA256 with a random salt per password. Stored format: pbkdf2$iterations$salt$hash */
public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] h = derive(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder e = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + e.encodeToString(salt) + "$" + e.encodeToString(h);
    }

    public static boolean verify(String password, String stored) {
        try {
            String[] p = stored.split("\\$");
            if (p.length != 4 || !p[0].equals("pbkdf2")) return false;
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            byte[] actual = derive(password.toCharArray(), salt, Integer.parseInt(p[1]));
            return MessageDigest.isEqual(expected, actual); // constant-time compare
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] pw, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(pw, salt, iterations, KEY_BITS)).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        }
    }
}
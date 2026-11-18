package com.f52124014.todoozi.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash password (PBKDF2 + salt acak) agar tidak disimpan sebagai teks biasa. */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 10000;
    private static final int KEY_BITS = 256;

    private PasswordUtil() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt);
        return PREFIX + Base64.encodeToString(salt, Base64.NO_WRAP)
                + "$" + Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean verify(String password, String stored) {
        if (!isHashed(stored)) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 3) return false;
        byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] expected = Base64.decode(parts[2], Base64.NO_WRAP);
        return MessageDigest.isEqual(expected, derive(password, salt));
    }

    private static byte[] derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }
}

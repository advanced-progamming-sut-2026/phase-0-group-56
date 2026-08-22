package models.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/** SHA-256 helper with backward-compatible verification of old plain-text saves. */
public final class CredentialHasher {
    private static final String ALGORITHM = "SHA-256";
    private static final int SHA_256_HEX_LENGTH = 64;

    private CredentialHasher() {
    }

    public static String hash(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Credential cannot be null.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public static boolean matches(String rawValue, String savedValue) {
        if (rawValue == null || savedValue == null) {
            return false;
        }

        String expected = isSha256Hash(savedValue)
            ? savedValue.toLowerCase(Locale.ROOT)
            : savedValue;
        String actual = isSha256Hash(savedValue) ? hash(rawValue) : rawValue;

        return MessageDigest.isEqual(
            actual.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static boolean isSha256Hash(String value) {
        if (value == null || value.length() != SHA_256_HEX_LENGTH) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            char character = Character.toLowerCase(value.charAt(index));
            boolean hexadecimal = character >= '0' && character <= '9'
                || character >= 'a' && character <= 'f';
            if (!hexadecimal) {
                return false;
            }
        }
        return true;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}

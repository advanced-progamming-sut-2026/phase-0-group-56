package network;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Small, line-oriented protocol shared by the game client and server. */
public final class Protocol {
    public static final String RESPONSE = "RESP";
    public static final String EVENT = "EVENT";

    private Protocol() {
    }

    public static String encode(String value) {
        String safe = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    public static String[] split(String line) {
        return line == null ? new String[0] : line.split("\\|", -1);
    }
}

package models;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;

/**
 * A display-friendly news entry which is stored as a versioned String.
 *
 * Keeping the persisted representation as String preserves compatibility with
 * older serialized User saves and with existing code that still appends a
 * plain message to User#getUnreadNews().
 */
public final class NewsItem {
    private static final String STORAGE_PREFIX = "PVZ_NEWS_V1|";
    private static final String STORAGE_SEPARATOR = "\\|";

    private final String date;
    private final String title;
    private final String body;
    private final boolean unread;

    public NewsItem(
        String date,
        String title,
        String body,
        boolean unread
    ) {
        this.date = normalizeDate(date);
        this.title = normalizeTitle(title);
        this.body = normalizeBody(body);
        this.unread = unread;
    }

    public static NewsItem create(
        String title,
        String body,
        boolean unread
    ) {
        return new NewsItem(
            LocalDate.now().toString(),
            title,
            body,
            unread
        );
    }

    public static NewsItem fromStorage(
        String storedValue,
        boolean unread
    ) {
        if (storedValue == null || storedValue.isBlank()) {
            return create("GAME NEWS", "News details are unavailable.", unread);
        }

        if (!storedValue.startsWith(STORAGE_PREFIX)) {
            return create(
                inferTitle(storedValue),
                storedValue,
                unread
            );
        }

        try {
            String encoded = storedValue.substring(STORAGE_PREFIX.length());
            String[] parts = encoded.split(STORAGE_SEPARATOR, -1);

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid news field count.");
            }

            return new NewsItem(
                decode(parts[0]),
                decode(parts[1]),
                decode(parts[2]),
                unread
            );
        } catch (RuntimeException exception) {
            /*
             * A damaged entry must never crash the News screen. Preserve the
             * original text as a legacy message so it can be re-encoded.
             */
            return create(
                "RECOVERED NEWS",
                storedValue,
                unread
            );
        }
    }

    public String toStorage() {
        return STORAGE_PREFIX
            + encode(date)
            + "|"
            + encode(title)
            + "|"
            + encode(body);
    }

    public boolean isStructuredStorage(String storedValue) {
        return storedValue != null
            && storedValue.startsWith(STORAGE_PREFIX)
            && toStorage().equals(storedValue);
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isUnread() {
        return unread;
    }

    public static String inferTitle(String message) {
        String normalized = message == null
            ? ""
            : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("quest")) {
            return "QUEST UPDATE";
        }

        if (normalized.contains("plant")) {
            return "PLANT UPDATE";
        }

        if (normalized.contains("zombie")) {
            return "ZOMBIE UPDATE";
        }

        if (normalized.contains("level") || normalized.contains("chapter")) {
            return "ADVENTURE UPDATE";
        }

        if (normalized.contains("minigame") || normalized.contains("mini-game")) {
            return "MINIGAME UPDATE";
        }

        return "GAME NEWS";
    }

    private static String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now().toString();
        }

        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (RuntimeException exception) {
            return LocalDate.now().toString();
        }
    }

    private static String normalizeTitle(String value) {
        return value == null || value.isBlank()
            ? "GAME NEWS"
            : value.trim();
    }

    private static String normalizeBody(String value) {
        return value == null || value.isBlank()
            ? "News details are unavailable."
            : value.trim();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
            );
    }

    private static String decode(String value) {
        return new String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }
}

package models.utils;

import java.util.regex.Pattern;

/** Shared validation rules for sign-up, password recovery and profile editing. */
public final class AccountValidator {
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9-]+$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?@"
            + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+"
            + "[A-Za-z]{2,}$"
    );

    private static final String PASSWORD_SPECIALS =
        "!#$%^&*()=+}{[]|/\\:;'\",><?";

    private AccountValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null
            && nickname.length() >= 3
            && nickname.length() <= 30;
    }

    public static boolean isValidEmail(String email) {
        return email != null
            && !email.contains("..")
            && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidGender(String gender) {
        return "male".equalsIgnoreCase(gender)
            || "female".equalsIgnoreCase(gender);
    }

    public static boolean isValidPassword(String password) {
        return getPasswordError(password) == null;
    }

    public static String getPasswordError(String password) {
        if (password == null || password.length() < 8) {
            return "Error: weak password; at least 8 characters are required.";
        }
        if (containsInvalidPasswordCharacter(password)) {
            return "Error: password contains a character that is not allowed.";
        }
        if (!containsRange(password, 'a', 'z')) {
            return "Error: weak password; add a lowercase English letter.";
        }
        if (!containsRange(password, 'A', 'Z')) {
            return "Error: weak password; add an uppercase English letter.";
        }
        if (!containsRange(password, '0', '9')) {
            return "Error: weak password; add a digit.";
        }
        if (!containsSpecialCharacter(password)) {
            return "Error: weak password; add an allowed special character.";
        }
        return null;
    }

    private static boolean containsInvalidPasswordCharacter(String password) {
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            boolean letter = character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z';
            boolean digit = character >= '0' && character <= '9';
            boolean special = PASSWORD_SPECIALS.indexOf(character) >= 0;

            if (!letter && !digit && !special) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsRange(String value, char first, char last) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= first && character <= last) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSpecialCharacter(String password) {
        for (int index = 0; index < password.length(); index++) {
            if (PASSWORD_SPECIALS.indexOf(password.charAt(index)) >= 0) {
                return true;
            }
        }
        return false;
    }
}

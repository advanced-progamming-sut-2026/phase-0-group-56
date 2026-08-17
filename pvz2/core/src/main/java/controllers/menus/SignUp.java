package controllers.menus;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.utils.RegexHelper;
import view.LogInView;
import controllers.menus.Menu;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public class SignUp implements Menu {
    public static final String[] SECURITY_QUESTIONS = {
        "What was the name of your first pet?",
        "What city were you born in?",
        "What is the name of your favorite teacher?",
        "What is your favorite childhood game?",
        "What is your favorite plant?"
    };

    @Override
    public String ChangeMenu(String menuName) {
        if (menuName != null && menuName.equalsIgnoreCase("Login menu")) {
            App.setScreen(new LogInView());
            return "Changed menu successfully to Login menu";
        }
        return "Invalid menu transition from Sign Up menu.";
    }

    @Override
    public String exitMenu() {
        return "Exit requested.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Sign Up Menu ---";
    }

    public String register(String username, String password, String passwordConfirm,
                           String nickname, String email, String gender) {
        if (!isValidUsername(username)) {
            return "Error: username may only contain English letters, digits and '-'.";
        }
        if (Data.isUsernameExists(username)) {
            return "Error: username is already taken.";
        }
        if (!password.equals(passwordConfirm)) {
            return "Error: password and confirmation do not match.";
        }
        String passwordError = passwordError(password);
        if (passwordError != null) {
            return passwordError;
        }
        if (!isValidNickname(nickname)) {
            return "Error: nickname must contain between 3 and 30 characters.";
        }
        if (!isValidEmail(email)) {
            return "Error: email format is invalid.";
        }
        if (!isValidGender(gender)) {
            return "Error: gender must be Male or Female.";
        }

        User newUser = new User(username, hashPassword(password), nickname, email, gender);
        Data.setTempUser(newUser);
        return "Data valid. Please choose a security question.";
    }

    public String pickQuestion(int questionNumber, String answer, String answerConfirm) {
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            return "Error: invalid security question.";
        }
        if (answer == null || answer.isBlank()) {
            return "Error: security answer cannot be empty.";
        }
        if (!answer.equals(answerConfirm)) {
            return "Error: security answers do not match.";
        }

        User tempUser = Data.getTempUser();
        if (tempUser == null) {
            return "Error: enter valid registration data first.";
        }

        tempUser.setSecurityQuestion(questionNumber, answer.trim());
        Data.addUser(tempUser);
        Data.setTempUser(null);
        Data.saveUser();
        return "User created successfully.";
    }

    public static String getQuestionText(int questionNumber) {
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            return "Unknown security question";
        }
        return SECURITY_QUESTIONS[questionNumber - 1];
    }

    private boolean isValidUsername(String username) {
        return username != null && Pattern.matches(RegexHelper.USERNAME_PATTERN, username);
    }

    private boolean isValidPassword(String password) {
        return password != null && Pattern.matches(RegexHelper.PASSWORD_PATTERN, password);
    }

    private String passwordError(String password) {
        if (password == null || password.length() < 8) {
            return "Error: weak password; at least 8 characters are required.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Error: weak password; add a lowercase letter.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Error: weak password; add an uppercase letter.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Error: weak password; add a digit.";
        }
        if (!isValidPassword(password)) {
            return "Error: weak password; add an allowed special character and remove invalid characters.";
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.length() - email.replace("@", "").length() != 1) {
            return false;
        }
        return Pattern.matches(RegexHelper.EMAIL_PATTERN, email);
    }

    private boolean isValidNickname(String nickname) {
        return nickname != null && nickname.length() >= 3 && nickname.length() <= 30;
    }

    private boolean isValidGender(String gender) {
        return "male".equalsIgnoreCase(gender) || "female".equalsIgnoreCase(gender);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte value : encodedHash) {
                String hex = Integer.toHexString(0xff & value);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash password.", exception);
        }
    }
}

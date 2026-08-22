package controllers.menus;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.utils.AccountValidator;
import models.utils.CredentialHasher;
import view.LogInView;

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
            Data.setTempUser(null);
            App.setScreen(new LogInView());
            return "Changed menu successfully to Login menu";
        }
        return "Invalid menu transition from Sign Up menu.";
    }

    @Override
    public String exitMenu() {
        Data.setTempUser(null);
        return "Exit requested.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Sign Up Menu ---";
    }

    public String register(
        String username,
        String password,
        String passwordConfirm,
        String nickname,
        String email,
        String gender
    ) {
        Data.setTempUser(null);

        String validationError = validateRegistration(
            username,
            password,
            passwordConfirm,
            nickname,
            email,
            gender
        );

        if (validationError != null) {
            return validationError;
        }

        User newUser = new User(
            username,
            CredentialHasher.hash(password),
            nickname,
            email,
            gender
        );
        Data.setTempUser(newUser);
        return "Data valid. Please choose a security question.";
    }

    public String pickQuestion(
        int questionNumber,
        String answer,
        String answerConfirm
    ) {
        User tempUser = Data.getTempUser();

        if (tempUser == null) {
            return "Error: enter valid registration data first.";
        }
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            return "Error: invalid security question.";
        }

        String normalizedAnswer = answer == null ? "" : answer.trim();
        String normalizedConfirm = answerConfirm == null ? "" : answerConfirm.trim();

        if (normalizedAnswer.isEmpty()) {
            return "Error: security answer cannot be empty.";
        }
        if (!normalizedAnswer.equals(normalizedConfirm)) {
            return "Error: security answers do not match.";
        }
        if (Data.isUsernameExists(tempUser.getName())) {
            Data.setTempUser(null);
            return "Error: username is already taken.";
        }

        tempUser.setSecurityQuestion(questionNumber, normalizedAnswer);
        Data.addUser(tempUser);
        Data.setTempUser(null);
        return "User created successfully.";
    }

    public static String getQuestionText(int questionNumber) {
        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.length) {
            return "Unknown security question";
        }
        return SECURITY_QUESTIONS[questionNumber - 1];
    }

    public static String hashPassword(String password) {
        return CredentialHasher.hash(password);
    }

    private String validateRegistration(
        String username,
        String password,
        String passwordConfirm,
        String nickname,
        String email,
        String gender
    ) {
        if (!AccountValidator.isValidUsername(username)) {
            return "Error: username may only contain English letters, digits and '-'.";
        }
        if (Data.isUsernameExists(username)) {
            return "Error: username is already taken.";
        }
        if (password == null || !password.equals(passwordConfirm)) {
            return "Error: password and confirmation do not match.";
        }

        String passwordError = AccountValidator.getPasswordError(password);
        if (passwordError != null) {
            return passwordError;
        }
        if (!AccountValidator.isValidNickname(nickname)) {
            return "Error: nickname must contain between 3 and 30 characters.";
        }
        if (!AccountValidator.isValidEmail(email)) {
            return "Error: email format is invalid.";
        }
        if (!AccountValidator.isValidGender(gender)) {
            return "Error: gender must be Male or Female.";
        }
        return null;
    }
}

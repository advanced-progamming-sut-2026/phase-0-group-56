package controllers.menus;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.utils.AccountValidator;
import models.utils.CredentialHasher;
import network.AccountSnapshot;
import network.NetworkClient;
import network.NetworkResponse;
import network.NetworkService;
import view.HomeView;
import view.SignUpView;

public class LogIn implements Menu {
    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Log In menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new SignUpView());
        return "Returned to Sign Up Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Log In Menu ---";
    }

    public String login(String username, String password, boolean stayLoggedIn) {
        if (NetworkService.isConnected()) {
            NetworkClient client = NetworkService.getClient();
            NetworkResponse response = client.login(username, password);
            if (!response.success()) {
                return "Error: " + response.message();
            }
            AccountSnapshot account = NetworkClient.accountFrom(response);
            if (account == null) {
                return "Error: server returned an invalid account.";
            }
            User localUser = Data.getUserByUsername(account.username());
            if (localUser == null) {
                localUser = new User(
                    account.username(),
                    CredentialHasher.hash(password),
                    account.nickname(),
                    account.email(),
                    account.gender()
                );
            } else {
                localUser.setPasswordHash(CredentialHasher.hash(password));
                localUser.setNickname(account.nickname());
                localUser.setEmail(account.email());
                localUser.setGender(account.gender());
            }
            localUser.setHighestScore(account.highestScore());
            clearPersistentSessions();
            localUser.setStayLoggedIn(stayLoggedIn);
            Data.upsertUser(localUser);
            Data.setCurrentUser(localUser);
            App.setScreen(new HomeView());
            return "Logged in successfully.";
        }
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (!CredentialHasher.matches(password, user.getPasswordHash())) {
            return "Error: incorrect password.";
        }

        migratePasswordHash(user, password);
        clearPersistentSessions();
        user.setStayLoggedIn(stayLoggedIn);
        Data.setCurrentUser(user);
        Data.saveUser();
        App.setScreen(new HomeView());
        return "Logged in successfully.";
    }

    public String getSecurityQuestion(String username, String email) {
        if (NetworkService.isConnected()) {
            NetworkClient client = NetworkService.getClient();
            NetworkResponse response = client == null
                ? null
                : client.getSecurityQuestion(username, email);
            if (response != null && response.success()) {
                return response.message();
            }
            // If the network account is also mirrored locally, retain the
            // offline recovery path while the server is unavailable.
            if (response != null && !"CONNECTION_LOST".equals(response.code())
                && !"NOT_CONNECTED".equals(response.code())
                && !"UNKNOWN_COMMAND".equals(response.code())) {
                return "Error: " + response.message();
            }
        }
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (!AccountValidator.isValidEmail(email)) {
            return "Error: email format is invalid.";
        }
        if (!email.equalsIgnoreCase(user.getEmail())) {
            return "Error: email does not match this account.";
        }
        if (user.getSecurityQuestionNumber() < 1
            || user.getSecurityQuestionNumber() > SignUp.SECURITY_QUESTIONS.length) {
            return "Error: this account has no valid security question.";
        }
        return SignUp.getQuestionText(user.getSecurityQuestionNumber());
    }

    public String resetPassword(
        String username,
        String email,
        String answer,
        String newPassword,
        String confirmPassword
    ) {
        if (NetworkService.isConnected()) {
            NetworkClient client = NetworkService.getClient();
            NetworkResponse response = client == null
                ? null
                : client.resetPassword(username, email, answer, newPassword);
            if (response != null && response.success()) {
                // Update a local mirror too, so offline login works after the
                // next reconnect.
                User mirrored = Data.getUserByUsername(username);
                if (mirrored != null) {
                    mirrored.setPasswordHash(CredentialHasher.hash(newPassword));
                    Data.saveUser();
                }
                return "Password reset successfully. You can now log in.";
            }
            if (response != null && !"CONNECTION_LOST".equals(response.code())
                && !"NOT_CONNECTED".equals(response.code())
                && !"UNKNOWN_COMMAND".equals(response.code())) {
                return "Error: " + response.message();
            }
        }
        User user = Data.getUserByUsername(username);
        String identityError = validateRecoveryIdentity(user, email);
        if (identityError != null) {
            return identityError;
        }
        if (!user.checkSecurityAnswer(answer)) {
            return "Error: incorrect security answer.";
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return "Error: password and confirmation do not match.";
        }

        String passwordError = AccountValidator.getPasswordError(newPassword);
        if (passwordError != null) {
            return passwordError;
        }
        if (CredentialHasher.matches(newPassword, user.getPasswordHash())) {
            return "Error: new password must be different from the old password.";
        }

        user.setPasswordHash(CredentialHasher.hash(newPassword));
        Data.saveUser();
        return "Password reset successfully. You can now log in.";
    }

    /** Backward-compatible method used by the terminal view. */
    public String resetPassword(String username, String answer, String newPassword) {
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        return resetPassword(
            username,
            user.getEmail(),
            answer,
            newPassword,
            newPassword
        );
    }

    private String validateRecoveryIdentity(User user, String email) {
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (!AccountValidator.isValidEmail(email)) {
            return "Error: email format is invalid.";
        }
        if (!email.equalsIgnoreCase(user.getEmail())) {
            return "Error: email does not match this account.";
        }
        return null;
    }

    private void clearPersistentSessions() {
        for (User savedUser : Data.getAllUsers()) {
            if (savedUser != null) {
                savedUser.setStayLoggedIn(false);
            }
        }
    }

    private void migratePasswordHash(User user, String password) {
        if (!CredentialHasher.isSha256Hash(user.getPasswordHash())) {
            user.setPasswordHash(CredentialHasher.hash(password));
        }
    }
}

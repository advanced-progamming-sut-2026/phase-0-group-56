package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.utils.AccountValidator;
import models.utils.CredentialHasher;
import network.NetworkClient;
import network.NetworkResponse;
import network.NetworkService;
import view.HomeView;

public class Profile implements Menu {
    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from this menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Profile Menu ---";
    }

    public String showProfile() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }

        StringBuilder result = new StringBuilder();
        result.append("Username: ").append(user.getName()).append('\n');
        result.append("Nickname: ").append(user.getNickname()).append('\n');
        result.append("Email: ").append(user.getEmail()).append('\n');
        result.append("Games Played: ").append(user.getGamesPlayed()).append('\n');
        result.append("Coins: ").append(user.getCoins()).append('\n');
        result.append("Diamonds: ").append(user.getDiamonds()).append('\n');
        result.append("Levels Passed: ").append(user.getLevelsPassed()).append('\n');
        result.append("Highest MeowPoint: ").append(user.getHighestScore());
        return result.toString();
    }

    public String changeUserName(String newUsername) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        if (newUsername != null && user.getName().equalsIgnoreCase(newUsername)) {
            return "Error: new username cannot be the same as the current one.";
        }
        if (!AccountValidator.isValidUsername(newUsername)) {
            return "Error: username format is invalid.";
        }
        if (Data.isUsernameExists(newUsername)) {
            return "Error: username is already taken.";
        }

        String syncError = syncAccount(user, newUsername, user.getNickname(),
            user.getEmail(), user.getGender(), "");
        if (syncError != null) {
            return syncError;
        }

        user.setName(newUsername);
        Data.saveUser();
        return "Username changed successfully.";
    }

    public String changeNickName(String newNickname) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        if (!AccountValidator.isValidNickname(newNickname)) {
            return "Error: nickname must contain between 3 and 30 characters.";
        }
        if (newNickname.equals(user.getNickname())) {
            return "Error: new nickname cannot be the same as the current one.";
        }

        String syncError = syncAccount(user, user.getName(), newNickname,
            user.getEmail(), user.getGender(), "");
        if (syncError != null) {
            return syncError;
        }

        user.setNickname(newNickname);
        Data.saveUser();
        return "Nickname changed successfully.";
    }

    public String changeEmail(String newEmail) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        if (!AccountValidator.isValidEmail(newEmail)) {
            return "Error: email format is invalid.";
        }
        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(newEmail)) {
            return "Error: new email cannot be the same as the current one.";
        }

        String syncError = syncAccount(user, user.getName(), user.getNickname(),
            newEmail, user.getGender(), "");
        if (syncError != null) {
            return syncError;
        }

        user.setEmail(newEmail);
        Data.saveUser();
        return "Email changed successfully.";
    }

    public String changePassword(String oldPassword, String newPassword) {
        return changePassword(oldPassword, newPassword, newPassword);
    }

    public String changePassword(
        String oldPassword,
        String newPassword,
        String confirmPassword
    ) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        if (!CredentialHasher.matches(oldPassword, user.getPasswordHash())) {
            return "Error: incorrect old password.";
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return "Error: password and confirmation do not match.";
        }

        String passwordError = AccountValidator.getPasswordError(newPassword);
        if (passwordError != null) {
            return passwordError;
        }
        if (CredentialHasher.matches(newPassword, user.getPasswordHash())) {
            return "Error: new password cannot be the same as the old password.";
        }

        String syncError = syncAccount(user, user.getName(), user.getNickname(),
            user.getEmail(), user.getGender(), newPassword);
        if (syncError != null) {
            return syncError;
        }

        user.setPasswordHash(CredentialHasher.hash(newPassword));
        Data.saveUser();
        return "Password changed successfully.";
    }

    /** Synchronizes a profile edit with the authoritative server when present. */
    private String syncAccount(User user, String newUsername, String nickname,
                               String email, String gender, String newPassword) {
        if (!NetworkService.isConnected()) {
            return null;
        }
        NetworkClient client = NetworkService.getClient();
        if (client == null) {
            return null;
        }
        NetworkResponse response = client.updateAccount(
            user.getName(), newUsername, nickname, email, gender,
            newPassword, user.getPasswordHash()
        );
        if (response.success()) {
            return null;
        }
        return "Error: " + response.message();
    }
}

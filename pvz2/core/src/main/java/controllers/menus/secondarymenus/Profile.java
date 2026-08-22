package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import models.utils.AccountValidator;
import models.utils.CredentialHasher;
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
        if (user.getNickname().equals(newNickname)) {
            return "Error: new nickname cannot be the same as the current one.";
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
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            return "Error: new email cannot be the same as the current one.";
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

        user.setPasswordHash(CredentialHasher.hash(newPassword));
        Data.saveUser();
        return "Password changed successfully.";
    }
}

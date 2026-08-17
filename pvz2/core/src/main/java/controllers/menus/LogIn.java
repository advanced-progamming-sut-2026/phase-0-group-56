package controllers.menus;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.utils.RegexHelper;
import view.HomeView;
import view.SignUpView;
import controllers.menus.Menu;

import java.util.regex.Pattern;

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
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (!user.getPasswordHash().equals(controllers.menus.SignUp.hashPassword(password))) {
            return "Error: incorrect password.";
        }

        user.setStayLoggedIn(stayLoggedIn);
        Data.setCurrentUser(user);
        Data.saveUser();
        App.setScreen(new HomeView());
        return "Logged in successfully.";
    }

    public String getSecurityQuestion(String username, String email) {
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (email == null || !email.equalsIgnoreCase(user.getEmail())) {
            return "Error: email does not match this account.";
        }
        return controllers.menus.SignUp.getQuestionText(user.getSecurityQuestionNumber());
    }

    public String resetPassword(String username, String email, String answer,
                                String newPassword, String confirmPassword) {
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        if (email == null || !email.equalsIgnoreCase(user.getEmail())) {
            return "Error: email does not match this account.";
        }
        if (!user.checkSecurityAnswer(answer == null ? "" : answer.trim())) {
            return "Error: incorrect security answer.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "Error: password and confirmation do not match.";
        }
        if (!Pattern.matches(RegexHelper.PASSWORD_PATTERN, newPassword)) {
            return "Error: new password is weak or contains invalid characters.";
        }
        if (user.getPasswordHash().equals(controllers.menus.SignUp.hashPassword(newPassword))) {
            return "Error: new password must be different from the old password.";
        }

        user.setPasswordHash(controllers.menus.SignUp.hashPassword(newPassword));
        Data.saveUser();
        return "Password reset successfully. You can now log in.";
    }

    /** Backward-compatible method used by the old terminal view. */
    public String resetPassword(String username, String answer, String newPassword) {
        User user = Data.getUserByUsername(username);
        if (user == null) {
            return "Error: username does not exist.";
        }
        return resetPassword(username, user.getEmail(), answer, newPassword, newPassword);
    }
}

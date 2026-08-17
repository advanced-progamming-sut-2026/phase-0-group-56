package controllers.menus;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import view.NewsView;
import view.PlayView;
import view.ProfileView;
import view.SettingsView;

public class Home implements Menu {
    @Override
    public String ChangeMenu(String menuName) {
        if (menuName == null) {
            return "Invalid menu.";
        }
        if (menuName.equalsIgnoreCase("Play menu")) {
            App.setScreen(new PlayView());
            return "Changed menu successfully to Play menu";
        }
        if (menuName.equalsIgnoreCase("Setting menu") || menuName.equalsIgnoreCase("Settings menu")) {
            App.setScreen(new SettingsView());
            return "Changed menu successfully to Settings menu";
        }
        if (menuName.equalsIgnoreCase("News menu")) {
            App.setScreen(new NewsView());
            return "Changed menu successfully to News menu";
        }
        if (menuName.equalsIgnoreCase("Profile menu")) {
            App.setScreen(new ProfileView());
            return "Changed menu successfully to Profile menu";
        }
        return "The selected menu is not available from Home menu.";
    }

    @Override
    public String exitMenu() {
        return "Error: use logout to exit Home menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Home Menu ---";
    }

    public String LogOut() {
        User user = Data.getCurrentUser();
        if (user != null) {
            user.setStayLoggedIn(false);
        }
        Data.saveUser();
        Data.setCurrentUser(null);
        return "Logged out successfully.";
    }
}

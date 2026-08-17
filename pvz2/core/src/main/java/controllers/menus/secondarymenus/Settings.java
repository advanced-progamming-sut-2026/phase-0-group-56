package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import view.HomeView;

public class Settings implements Menu {
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
        return "--- Settings Menu ---";
    }

    public String ChangeHardness(int difficultyLevel) {
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            return "Error: difficulty level must be between 1 and 5.";
        }
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        user.setDifficultyLevel(difficultyLevel);
        Data.saveUser();
        return "Difficulty changed to " + difficultyLevel + ".";
    }

    public String changeGameSpeed(int speed) {
        if (speed < 1 || speed > 3) {
            return "Error: game speed must be between 1 and 3.";
        }
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        user.setGameSpeed(speed);
        Data.saveUser();
        return "Game speed changed to " + speed + ".";
    }

    public String setGridVisible(boolean visible) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        user.setShowGrid(visible);
        Data.saveUser();
        return "Grid display " + (visible ? "enabled." : "disabled.");
    }

    public String setDebugMode(boolean enabled) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return "Error: User not found.";
        }
        user.setDebugMode(enabled);
        Data.saveUser();
        return "Debug mode " + (enabled ? "enabled." : "disabled.");
    }
}

package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import view.HomeView;

public class Settings implements Menu {
    public static final int MIN_DIFFICULTY = 1;
    public static final int MAX_DIFFICULTY = 5;
    public static final int DEFAULT_DIFFICULTY = 3;

    public static final int MIN_GAME_SPEED = 1;
    public static final int MAX_GAME_SPEED = 3;
    public static final int DEFAULT_GAME_SPEED = 1;

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
        if (!isDifficultyValid(difficultyLevel)) {
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
        if (!isGameSpeedValid(speed)) {
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

    public String applySettings(
        int difficultyLevel,
        int gameSpeed,
        boolean showGrid,
        boolean debugMode
    ) {
        String validationError = validate(difficultyLevel, gameSpeed);

        if (validationError != null) {
            return validationError;
        }

        User user = Data.getCurrentUser();

        if (user == null) {
            return "Error: User not found.";
        }

        boolean changed =
            user.getDifficultyLevel() != difficultyLevel
                || user.getGameSpeed() != gameSpeed
                || user.isShowGrid() != showGrid
                || user.isDebugMode() != debugMode;

        if (!changed) {
            return "Settings are already up to date.";
        }

        /*
         * Validate every value before mutating the user. This keeps the
         * operation all-or-nothing and avoids four separate save writes.
         */
        user.setDifficultyLevel(difficultyLevel);
        user.setGameSpeed(gameSpeed);
        user.setShowGrid(showGrid);
        user.setDebugMode(debugMode);

        Data.saveUser();

        return "Settings saved successfully.";
    }

    public String restoreDefaults() {
        String result = applySettings(
            DEFAULT_DIFFICULTY,
            DEFAULT_GAME_SPEED,
            false,
            false
        );

        if (result.startsWith("Error:")) {
            return result;
        }

        if ("Settings are already up to date.".equals(result)) {
            return "Default settings are already active.";
        }

        return "Default settings restored.";
    }

    private String validate(int difficultyLevel, int gameSpeed) {
        if (!isDifficultyValid(difficultyLevel)) {
            return "Error: difficulty level must be between 1 and 5.";
        }

        if (!isGameSpeedValid(gameSpeed)) {
            return "Error: game speed must be between 1 and 3.";
        }

        return null;
    }

    private boolean isDifficultyValid(int difficultyLevel) {
        return difficultyLevel >= MIN_DIFFICULTY
            && difficultyLevel <= MAX_DIFFICULTY;
    }

    private boolean isGameSpeedValid(int gameSpeed) {
        return gameSpeed >= MIN_GAME_SPEED
            && gameSpeed <= MAX_GAME_SPEED;
    }
}

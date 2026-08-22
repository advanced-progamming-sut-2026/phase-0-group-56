package controllers.menus.gamecontroller;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.games.minigames.IZombie;
import models.utils.Result;

/** Controller gateway for the reverse-role I, Zombie minigame. */
public final class IZombieController implements Controller {
    private final IZombie game;
    private boolean resultSaved;

    public IZombieController(int levelNumber) {
        game = new IZombie(levelNumber);
    }

    @Override
    public String playGame(float delta) {
        String log = game.playGame(delta);
        Result end = game.check_endGame();
        if (end.success() && !resultSaved) {
            resultSaved = true;
            saveResult("Won".equals(end.message()));
        }
        return log == null ? "" : log;
    }

    public String placeZombie(String type, int column, int row) {
        IZombie.PlacementResult result = game.placeZombie(type, column, row);
        return switch (result) {
            case PLACED -> pretty(type) + " deployed.";
            case INVALID_TILE -> "Place zombies to the right of the red line.";
            case BRAIN_ALREADY_EATEN -> "That row's brain is already eaten.";
            case NOT_AVAILABLE -> "That zombie is not available in this level.";
            case NOT_ENOUGH_SUN -> "Not enough sun.";
            case OCCUPIED -> "Another deployed zombie is already on that tile.";
            case GAME_OVER -> "The game is over.";
        };
    }

    private void saveResult(boolean won) {
        User user = App.getCurrentuser();
        if (user == null) {
            return;
        }
        user.incrementGamesPlayed();
        if (won) {
            if (game.getLevelNumber() >= user.getIZombie()) {
                user.setIZombie(user.getIZombie() + 1);
            }
            user.incrementMinigamesWon();
            user.updateQuestProgress("WIN_MINIGAME", 1);
        }
        Data.saveUser();
    }

    public IZombie getGame() {
        return game;
    }

    private static String pretty(String value) {
        return value == null ? "Zombie" : value.toLowerCase().replace('_', ' ');
    }

    @Override
    public String GameStart(String input) {
        return "I, Zombie is already running.";
    }
}

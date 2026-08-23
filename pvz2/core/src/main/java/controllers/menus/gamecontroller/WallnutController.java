package controllers.menus.gamecontroller;

import controllers.datacontroller.MiniGameLevelManager;
import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import models.games.minigames.MinigameLevel;
import models.games.minigames.WallnutBowling;
import models.utils.Result;
import java.util.ArrayList;
import java.util.List;

/** Controller gateway for the graphical Wall-Nut Bowling screen. */
public class WallnutController implements Controller{
    private final int progress;
    private boolean resultSaved;

    public WallnutController(int level){
        progress = Math.max(1, Math.min(3, level));
        this.level = loadLevel(progress);
        game = new WallnutBowling(this.level);
    }
    private final MinigameLevel level;
    private final WallnutBowling game;


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

    public boolean plant(String name , int x , int y) {
        try {
            return game.plant(name ,  x , y);
        }catch (Exception e){
            return false;
        }
    }

    public WallnutBowling getGame() {
        return game;
    }

    public int getProgress() {
        return progress;
    }

    public boolean hasNutType(String type) {
        return game.hasNutType(type);
    }

    private void saveResult(boolean won) {
        User user = App.getCurrentuser();
        if (user == null) {
            return;
        }
        if (won) {
            user.setWallNutBowling(user.getWallNutBowling() + 1);
            user.incrementMinigamesWon();
            user.updateQuestProgress("WIN_MINIGAME", 1);
        }
        user.incrementGamesPlayed();
        Data.saveUser();
    }

    private static MinigameLevel loadLevel(int progress) {
        MinigameLevel loaded = null;
        try {
            loaded = MiniGameLevelManager.getLevelById(200 + progress);
        } catch (IllegalStateException ignored) {
            // Main may not have loaded minigames.json in a headless test.
        }
        if (loaded != null) {
            return loaded;
        }

        MinigameLevel fallback = new MinigameLevel();
        fallback.setId(200 + progress);
        ArrayList<PlantType> plants = new ArrayList<>();
        plants.add(PlantType.BOWLING_BULB);
        if (progress >= 2) {
            plants.add(PlantType.EXPLODE_O_NUT);
        }
        fallback.setPlants(plants);
        fallback.setZombiesNames(new ArrayList<>(List.of(
            "normal", "cone", "bucket", "imp"
        )));
        return fallback;
    }

    @Override
    public String GameStart(String input) {
        return "";
    }
}

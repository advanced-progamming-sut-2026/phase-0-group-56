package controllers.menus.gamecontroller;

import controllers.datacontroller.Data;
import models.App;
import models.User;
import models.factory.builder.PlantType;
import models.games.minigames.MinigameLevel;
import models.games.minigames.VaseBreakResult;
import models.games.minigames.VaseBraker;
import models.games.minigames.VaseSeedDrop;
import models.utils.Result;

import java.util.ArrayList;
import java.util.List;

/** Controller for the graphical Vase Breaker screen. */
public class VaseBreakerController implements Controller {
    private final VaseBraker game;
    private final int levelNumber;
    private boolean ended;
    private String resultMessage = "";

    public VaseBreakerController() {
        this(buildCurrentLevel());
    }

    /** Starts the level selected in the minigame menu. */
    public VaseBreakerController(int level) {
        this(buildLevel(level));
    }

    public VaseBreakerController(MinigameLevel level) {
        game = new VaseBraker(level);
        levelNumber = Math.max(1, Math.min(3, level == null ? 1 : level.getId()));
    }

    public VaseBraker getGame() {
        return game;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    @Override
    public String playGame(float delta) {
        if (ended) {
            return "";
        }

        game.playGame(delta);
        Result end = game.check_endGame();

        if (end.success()) {
            ended = true;
            finish(end.message());
            return end.message() == null ? "" : end.message();
        }

        return "";
    }

    public VaseBreakResult breakVase(int column, int row) {
        return game.breakVase(column, row);
    }

    public boolean selectSeedDrop(VaseSeedDrop drop) {
        return game.selectSeedDrop(drop);
    }

    public void cancelSeedSelection() {
        game.cancelSeedSelection();
    }

    public String plantSelectedSeed(int column, int row) {
        PlantType selected = game.getSelectedPlantType();
        if (selected == null) {
            return "Pick a seed packet first.";
        }

        if (game.plantSelectedSeed(column, row)) {
            return pretty(selected) + " planted.";
        }
        return "Cannot plant here.";
    }

    public String pluck(int column, int row) {
        return game.pluck(column, row);
    }

    public List<VaseSeedDrop> getSeedDrops() {
        return game.getSeedDrops();
    }

    @Override
    public String GameStart(String input) {
        return "Vase Breaker is already running.";
    }

    private void finish(String result) {
        resultMessage = result == null ? "" : result;
        User user = App.getCurrentuser();

        if (user != null && "Won".equals(result)) {
            user.setVaseBreaker(user.getVaseBreaker() + 1);
            user.incrementMinigamesWon();
            user.updateQuestProgress("WIN_MINIGAME", 1);
            Data.saveUser();
        }

    }

    private static MinigameLevel buildCurrentLevel() {
        User user = App.getCurrentuser();
        return buildLevel(user == null ? 1 : user.getVaseBreaker());
    }

    private static MinigameLevel buildLevel(int requestedLevel) {
        User user = App.getCurrentuser();
        MinigameLevel level = new MinigameLevel();

        level.setId(Math.max(1, Math.min(3, requestedLevel)));

        ArrayList<PlantType> plants = new ArrayList<>();
        if (user != null) {
            plants.addAll(user.getUnlockedPlants());
        }
        if (plants.isEmpty()) {
            plants.addAll(List.of(
                PlantType.PEASHOOTER,
                PlantType.WALL_NUT,
                PlantType.SNOW_PEA,
                PlantType.REPEATER,
                PlantType.CHOMPER
            ));
        }
        level.setPlants(plants);

        ArrayList<String> zombies = new ArrayList<>();
        zombies.add("normal");
        zombies.add("cone");
        zombies.add("bucket");
        zombies.add("imp");

        if (level.getId() >= 2) {
            zombies.add("ra");
            zombies.add("explorer");
            zombies.add("newspaper");
        }
        if (level.getId() >= 3) {
            zombies.add("tombraiser");
            zombies.add("gargantuar");
        }
        level.setZombiesNames(zombies);

        return level;
    }

    private static String pretty(PlantType type) {
        return type == null
            ? "Plant"
            : type.name().toLowerCase().replace('_', ' ');
    }
}

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
import view.TravelLogView;

import java.util.ArrayList;
import java.util.List;

/** Controller for the graphical Vase Breaker screen. */
public class VaseBreakerController implements Controller {
    private final VaseBraker game;
    private boolean ended;

    public VaseBreakerController() {
        this(buildCurrentLevel());
    }

    public VaseBreakerController(MinigameLevel level) {
        game = new VaseBraker(level);
    }

    public VaseBraker getGame() {
        return game;
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
        User user = App.getCurrentuser();

        if (user != null && "Won".equals(result)) {
            user.setVaseBreaker(user.getVaseBreaker() + 1);
            user.incrementMinigamesWon();
            user.updateQuestProgress("WIN_MINIGAME", 1);
            Data.saveUser();
        }

        App.setScreen(new TravelLogView());
    }

    private static MinigameLevel buildCurrentLevel() {
        User user = App.getCurrentuser();
        MinigameLevel level = new MinigameLevel();

        int id = user == null ? 1 : user.getVaseBreaker();
        level.setId(Math.max(1, Math.min(3, id)));

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

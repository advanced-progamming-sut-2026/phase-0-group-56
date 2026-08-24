package controllers.menus.gamecontroller;

import controllers.datacontroller.Data;
import controllers.datacontroller.LevelProgressService;
import controllers.datacontroller.SeedPackage;
import controllers.menus.Menu;
import models.App;
import models.QuestGameSession;
import models.User;
import models.entity.*;
import models.entity.Projectile;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.gamepanes.Tile;
import models.games.BaseGame;
import models.games.NormalGame;
import models.games.specialgames.ConveyorBelt;
import models.games.specialgames.Deadline;
import models.games.specialgames.LockedPlants;
import models.games.specialgames.LoveYourPlants;
import models.games.specialgames.NightsOps;
import models.games.specialgames.PlantWhatYouGet;
import models.games.specialgames.SaveOurSeeds;
import models.games.specialgames.TimedWar;
import models.utils.Result;
import view.PlayView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Single controller gateway used by GameView.
 *
 * <p>GameView may read state through {@link #getGame()}, but every state-changing
 * action is routed through this controller.</p>
 */
public class GameController implements Controller, Menu {

    private static final int REQUIRED_STARTING_PLANTS = 5;

    private final BaseGame game;
    private final Level level;
    private final Chapters chapter;
    private final QuestGameSession questSession;

    public GameController(Chapters chapter, Level level) {
        if (chapter == null) {
            throw new IllegalArgumentException("chapter cannot be null");
        }
        if (level == null || level.getLevelType() == null) {
            throw new IllegalArgumentException("level and level type cannot be null");
        }

        this.level = level;
        this.chapter = chapter;

        game = switch (level.getLevelType().toLowerCase()) {
            case "night ops" -> new NightsOps(chapter,level);
            case "plant what you get" -> new PlantWhatYouGet(chapter,level);
            case "locked plants by category" -> new LockedPlants(LockedPlants.LockType.ByCategory);
            case "conveyor belt" -> new ConveyorBelt(chapter,level);
            case "deadline" -> new Deadline(chapter,level);
            case "save our seeds" -> new SaveOurSeeds(chapter,level);
            case "timed war" -> new TimedWar(chapter,level);
            case "love your plants" -> new LoveYourPlants(chapter,level);
            default -> new NormalGame(chapter,level);
        };

        game.initGame(chapter, level);
        questSession = new QuestGameSession(game, chapter, level);
        if (game.getState() == BaseGame.GameState.PLAYING) {
            questSession.onGameStarted();
        }
    }

    // -------------------------------------------------------------------------
    // GUI-facing gameplay API
    // -------------------------------------------------------------------------

    public boolean plant(PlantType type, int x, int y) {
        if (type == null) {
            return false;
        }
        return plant(type.name(), x, y);
    }

    public boolean plant(String name, int x, int y) {
        if (game.getState() != BaseGame.GameState.PLAYING) {
            return false;
        }

        PlantType type = parsePlantType(name);
        if (type == null) {
            return false;
        }

        // ConveyorBelt owns availability itself; it does not use SeedPackage cooldowns.
        if (game instanceof ConveyorBelt) {
            int before = game.getPlantsInField().size();
            boolean planted = game.plant(type.name(), x, y);
            if (planted) {
                notifyNewPlants(before);
            }
            return planted;
        }

        SeedPackage packet = game.getAvailable_plants().get(type);
        if (packet == null) {
            return false;
        }

        if (!packet.isAvailable()) {
            return false;
        }

        int before = game.getPlantsInField().size();
        boolean result = game.plant(type.name(), x, y);
        int after = game.getPlantsInField().size();

        /*
         * SeedPackage stores the remaining cooldown rather than its original maximum.
         * The model currently does not reset it after a successful planting, therefore
         * recreating the selected package is the safest controller-level reset without
         * changing model classes.
         */
        if (after > before) {
            SeedPackage recharged = game.getSelection().selectPlant(type.name());
            if (recharged != null) {
                game.getAvailable_plants().put(type, recharged);
            }
            notifyNewPlants(before);
        }

        return result;
    }

    public String pluck(int x, int y) {
        if (game.getState() != BaseGame.GameState.PLAYING) {
            return "The game is not running.";
        }
        return game.pluck(x, y);
    }

    public String collectSun(int x, int y) {
        if (game.getState() != BaseGame.GameState.PLAYING) {
            return null;
        }

        Iterator<Sun> iterator = game.getSuns().iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun.getTileIndex() == x && sun.getLine() == y) {
                return collectMatchedSun(iterator, sun);
            }
        }
        return null;
    }

    public String collectSun(Sun target) {
        if (game.getState() != BaseGame.GameState.PLAYING || target == null) {
            return null;
        }

        Iterator<Sun> iterator = game.getSuns().iterator();
        while (iterator.hasNext()) {
            Sun sun = iterator.next();
            if (sun == target) {
                return collectMatchedSun(iterator, sun);
            }
        }
        return null;
    }

    private String collectMatchedSun(Iterator<Sun> iterator, Sun sun) {
        if (sun.isRadioActive()) {
            sun.dispose(game);
            iterator.remove();
            return "Radioactive sun exploded.";
        }

        game.setSunCount(game.getSunCount() + sun.getPrice());

        questSession.onSunCollected(sun.getPrice());

        int price = sun.getPrice();
        iterator.remove();
        return "Sun collected: +" + price;
    }

    private void notifyNewPlants(int previousCount) {
        List<Plant> plants = game.getPlantsInField();
        for (int i = Math.max(0, previousCount); i < plants.size(); i++) {
            questSession.onPlantPlaced(plants.get(i));
        }
    }


    public String boost(int x, int y) {
        if (game.getState() != BaseGame.GameState.PLAYING) {
            return "The game is not running.";
        }

        if (game.getPlantFoodsCount() <= 0) {
            return "No Plant Food available.";
        }

        Plant plant = game.findByCoordinates(x, y);
        if (plant == null) {
            return "There is no plant on this tile.";
        }

        plant.setPlantFood(true);
        game.setPlantFoodsCount(game.getPlantFoodsCount() - 1);
        return "Plant Food applied to " + plant.getType() + ".";
    }

    public void pauseGame() {
        if (game.getState() == BaseGame.GameState.PLAYING) {
            game.setState(BaseGame.GameState.PAUSE);
        }
    }

    public void resumeGame() {
        if (game.getState() == BaseGame.GameState.PAUSE) {
            game.setState(BaseGame.GameState.PLAYING);
        }
    }

    public void togglePause() {
        if (game.getState() == BaseGame.GameState.PLAYING) {
            pauseGame();
        } else if (game.getState() == BaseGame.GameState.PAUSE) {
            resumeGame();
        }
    }

    // -------------------------------------------------------------------------
    // STARTING / plant selection
    // -------------------------------------------------------------------------

    public List<PlantType> getSelectablePlants() {
        return List.copyOf(game.getSelection().getPlantsToChoose());
    }

    public List<PlantType> getSelectedPlants() {
        return List.copyOf(game.getAvailable_plants().keySet());
    }

    public boolean isPlantSelected(PlantType type) {
        return type != null && game.getAvailable_plants().containsKey(type);
    }

    public boolean canSelectAnotherPlant() {
        return game.getState() == BaseGame.GameState.STARTING
            && game.getAvailable_plants().size() < REQUIRED_STARTING_PLANTS;
    }

    public SeedPackage getPlantPreview(PlantType type) {
        if (type == null || !game.getSelection().getPlantsToChoose().contains(type)) {
            return null;
        }
        return game.getSelection().selectPlant(type.name());
    }

    public String addPlant(PlantType type) {
        if (type == null) {
            return "Plant not found.";
        }
        return addPlant(type.name());
    }

    public String addPlant(String name) {
        if (game.getState() != BaseGame.GameState.STARTING) {
            return "Plant selection is already finished.";
        }

        PlantType type = parsePlantType(name);
        if (type == null) {
            return "Plant not found.";
        }

        if (!game.getSelection().getPlantsToChoose().contains(type)) {
            return "This plant is not available for this level.";
        }

        if (game.getAvailable_plants().containsKey(type)) {
            return "The plant is already selected.";
        }

        if (game.getAvailable_plants().size() >= REQUIRED_STARTING_PLANTS) {
            return "All plant slots are full.";
        }

        SeedPackage seedPackage = game.getSelection().selectPlant(type.name());
        if (seedPackage == null) {
            return "Plant not found.";
        }

        game.getAvailable_plants().put(type, seedPackage);
        return type.name() + " selected.";
    }

    public String removePlant(PlantType type) {
        if (type == null) {
            return "Plant not found.";
        }
        return removePlant(type.name());
    }

    public String removePlant(String name) {
        if (game.getState() != BaseGame.GameState.STARTING) {
            return "You cannot change plant selection after the game starts.";
        }

        PlantType type = parsePlantType(name);
        if (type == null) {
            return "Plant not found.";
        }

        if (!game.getAvailable_plants().containsKey(type)) {
            return "This plant is not selected.";
        }

        return game.getSelection().removePlant(game.getAvailable_plants(), type);
    }

    public boolean canStartGame() {
        return game.getState() == BaseGame.GameState.STARTING
            && game.getAvailable_plants().size() == REQUIRED_STARTING_PLANTS;
    }

    public String startGame() {
        if (!canStartGame()) {
            return "Select exactly " + REQUIRED_STARTING_PLANTS + " plants first.";
        }
        return GameStart("");
    }

    @Override
    public String GameStart(String input) {
        if (game.getState() != BaseGame.GameState.STARTING) {
            return "Game has already started.";
        }

        boolean start = game.startGame(input == null ? "" : input);
        if (!start) {
            return "Cannot start game.";
        }

        game.setState(BaseGame.GameState.PLAYING);
        questSession.onGameStarted();
        return "Game started.";
    }

    // -------------------------------------------------------------------------
    // Main simulation
    // -------------------------------------------------------------------------

    @Override
    public String playGame(float delta) {
        if (game.getState() != BaseGame.GameState.PLAYING) {
            return "";
        }

        questSession.beforeUpdate();
        String log = game.playGame(delta);
        questSession.afterUpdate(delta);
        Result endResult = game.check_endGame();

        if (endResult.success() && "Loss".equals(endResult.message())) {
            questSession.onGameLost();
            App.setScreen(new PlayView());
            return "You lost the level.";
        }

        if (game.isWon()) {
            questSession.onGameWon();
            end();
            return "Level completed.";
        }

        return log;
    }

    private void end() {
        User user = Data.getCurrentUser();
        if (user == null) {
            App.setScreen(new PlayView());
            return;
        }

        /*
         * Level ids are global (1..16).  Keep all unlock, chapter-transition
         * and replay protection in one service instead of maintaining a
         * second, chapter-local counter here.
         */
        LevelProgressService.completeLevel(user, chapter, level);

        Data.saveUser();
        App.setScreen(new PlayView());
    }

    private PlantType parsePlantType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        try {
            return PlantType.valueOf(
                name.trim()
                    .replace(' ', '_')
                    .toUpperCase()
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Existing CLI/debug API kept compatible with the project
    // -------------------------------------------------------------------------

    public String gameEndCheat() {
        questSession.markCheatUsed();
        end();
        return "game ended. you won!";
    }

    public String showSunAmount() {
        return "-->> Suns : " + game.getSunCount();
    }

    public String cheatSunAmount(int amount) {
        questSession.markCheatUsed();
        game.setSunCount(game.getSunCount() + amount);
        return "==== >> Suns added by Cheat code : " + amount + "\n now " + showSunAmount();
    }

    /** Applies the in-game debug resource controls without exposing the model to the view. */
    public String debugAddResources(int suns, int plantFood) {
        if (suns <= 0 && plantFood <= 0) {
            return "Debug values must be positive.";
        }
        questSession.markCheatUsed();
        if (suns > 0) {
            game.addSun(suns);
        }
        if (plantFood > 0) {
            game.setPlantFoodsCount(game.getPlantFoodsCount() + plantFood);
        }
        return "DEBUG: +" + Math.max(0, suns) + " sun, +"
            + Math.max(0, plantFood) + " plant food";
    }

    public String cheatZombieKiller() {
        questSession.markCheatUsed();
        for (Zombie zombie : game.getZombies()) {
            zombie.setHp(0);
        }
        return "All active zombies were defeated.";
    }

    public String showPlantsStatus() {
        StringBuilder output = new StringBuilder();

        if (game instanceof ConveyorBelt) {
            return belt();
        }

        try {
            for (SeedPackage packet : game.getAvailable_plants().values()) {
                output.append(packet.getPlant().name()).append("\n")
                    .append("recharge remaining time = ").append(packet.getRecharge()).append("\n")
                    .append("cost = ").append(packet.getCost()).append("\n");
            }
        } catch (RuntimeException e) {
            return "Something went wrong during showing plants! try again!...";
        }

        return output.toString();
    }

    private String belt() {
        ConveyorBelt conveyorBelt = (ConveyorBelt) game;
        StringBuilder output = new StringBuilder();
        for (PlantType type : conveyorBelt.getBelt()) {
            output.append(type.name()).append(" is ready on the belt\n");
        }
        return output.toString();
    }

    public String tileStatus(int x, int y) {
        ArrayList<Plant> plants = new ArrayList<>();
        ArrayList<Zombie> zombies = new ArrayList<>();

        for (Plant plant : game.getPlantsInField()) {
            if (plant.getLine() == y && plant.getTileIndex() == x) {
                plants.add(plant);
            }
        }

        for (Zombie zombie : game.getZombies()) {
            if (zombie.getLine() == y && zombie.getTileIndex() == x) {
                zombies.add(zombie);
            }
        }

        Tile tile = game.getField().getTiles().get(y).get(x);
        StringBuilder output = new StringBuilder("═════════════════TILE STATUS════════════════════\n");
        output.append("Tile Type : ").append(tile.getTileType().name()).append("\n");
        output.append("hp : ").append(tile.getHp()).append("\n");
        output.append("Is this tile empty ? ").append(tile.isEmpty()).append("\n")
            .append("Is it underwater ? ").append(tile.isWater()).append("\n")
            .append("Is it plantable ? ").append(tile.isPlantable()).append("\n");

        boolean lilyPad = tile.isWater() && tile.isPlantable();
        output.append("══════\nIs there a lily pad here? ").append(lilyPad).append("\n");
        output.append("Plants on tile: ").append(plants.size()).append("\n");
        output.append("Zombies on tile: ").append(zombies.size()).append("\n");
        return output.toString();
    }

    public String cheat(String content) {
        String output = null;

        switch (content) {
            case "remove-cooldown" -> removeCooldown();
            case "add-plant-food" -> addPlantFood();
            case "end" -> output = gameEndCheat();
            case "add-plant", "add-sun" -> {
                // Existing command placeholders intentionally kept.
            }
            default -> {
            }
        }

        return "Cheat executed.\n" + output;
    }

    private void removeCooldown() {
        for (SeedPackage packet : game.getAvailable_plants().values()) {
            packet.setRecharge(0);
            packet.setAvailable(true);
        }
    }

    public String availablePlants() {
        StringBuilder output = new StringBuilder();
        for (PlantType type : game.getSelection().getPlantsToChoose()) {
            output.append(type.name()).append("\n");
        }
        return output.toString();
    }

    public String allPlants() {
        StringBuilder output = new StringBuilder();
        for (PlantType type : App.getCurrentuser().getUnlockedPlants()) {
            output.append(type.name()).append("\n");
        }
        return output.toString();
    }

    private void addPlantFood() {
        game.setPlantFoodsCount(game.getPlantFoodsCount() + 1);
    }

    public BaseGame getGame() {
        return game;
    }

    public Level getLevel() {
        return level;
    }

    public Chapters getChapter() {
        return chapter;
    }

    public String showAllZombies() {
        List<Zombie> zombies = game.getZombies();
        if (zombies.isEmpty()) {
            return "No active zombies in the game.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- Active Zombies (").append(zombies.size()).append(") ---\n");

        for (int i = 0; i < zombies.size(); i++) {
            sb.append(i + 1).append(". ").append(formatZombieInfo(zombies.get(i)));
            if (i < zombies.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    public String showZombie(String zombieName) {
        Zombie target = null;

        for (Zombie zombie : game.getZombies()) {
            if (zombie.getId().equalsIgnoreCase(zombieName)
                || zombie.getType().equalsIgnoreCase(zombieName)) {
                target = zombie;
                break;
            }
        }

        if (target == null) {
            return "Zombie \"" + zombieName + "\" not found in the current game.";
        }

        return formatZombieInfo(target);
    }

    private String formatZombieInfo(Zombie zombie) {
        StringBuilder sb = new StringBuilder();

        sb.append(zombie.getType());
        int col = zombie.getTileIndex();
        int row = zombie.getLine();
        sb.append("  position: (").append(row).append(", ").append(col).append(")\n");
        sb.append("x : ").append(zombie.getX()).append(" , y : ").append(zombie.getY()).append("\n");
        sb.append("  health: ").append(zombie.getHp()).append("/").append(zombie.getMaxHp());

        List<Armor> armors = zombie.getArmors();
        if (!armors.isEmpty()) {
            sb.append("  armors: ");
            for (int i = 0; i < armors.size(); i++) {
                Armor armor = armors.get(i);
                sb.append(armor.getType()).append(": ").append(armor.getHealth());
                if (armor.isBroken()) {
                    sb.append("(BROKEN)");
                }
                if (i < armors.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        List<Effect> effects = zombie.getEffects();
        if (!effects.isEmpty()) {
            sb.append("  effects: ");
            for (int i = 0; i < effects.size(); i++) {
                Effect effect = effects.get(i);
                sb.append(effect.getType().name().toLowerCase());
                float remaining = effect.getRemainingTime();
                if (remaining > 0) {
                    sb.append(": ").append(String.format("%.1f", remaining)).append("s");
                }
                if (i < effects.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        if (zombie.isHypnotized()) {
            sb.append("  hypnotized: YES");
        }
        if (zombie.isFrozen()) {
            sb.append("  frozen: YES");
        }

        return sb.toString();
    }

    public String showPlants() {
        if (game.getPlantsInField() == null || game.getPlantsInField().isEmpty()) {
            return "No plants currently on the field.\n";
        }

        StringBuilder sb = new StringBuilder();
        for (Plant plant : game.getPlantsInField()) {
            if (plant == null || plant.getHp() <= 0) {
                continue;
            }

            sb.append("=====\n")
                .append("type : ").append(plant.getType()).append("\n")
                .append("hp : ").append(plant.getHp()).append("\n")
                .append("location : x = ").append(plant.getTileIndex())
                .append(" , y = ").append(plant.getLine()).append("\n");
        }
        return sb.toString();
    }

    public String showBullets() {
        StringBuilder sb = new StringBuilder();
        for (Projectile projectile : game.getBullets()) {
            sb.append("=====\n")
                .append("type : ").append(projectile.getType()).append("\n")
                .append("location (").append(projectile.getX()).append(",").append(projectile.getY()).append(")\n");
        }
        return sb.toString();
    }

    public String showSuns() {
        if (game.getSuns().isEmpty()) {
            return "No suns in the game.";
        }

        StringBuilder sb = new StringBuilder();
        for (Sun sun : game.getSuns()) {
            sb.append(" price : ").append(sun.getPrice()).append("\n")
                .append("remainingTime : ").append(sun.getRemainingTime()).append("\n")
                .append(" is radio active ? ").append(sun.isRadioActive()).append("\n");
        }
        return sb.toString();
    }

    public String nuke() {
        if (game instanceof NormalGame normalGame) {
            return normalGame.nuke();
        }
        return "Nuke is unavailable in this game mode.";
    }

    public String showMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("Wave id : ").append(game.getWaveID()).append("\n");
        sb.append(showSunAmount()).append("\n");
        sb.append("zombies remained on the yard : ").append(game.getZombies().size()).append("\n");
        sb.append(showPlantsStatus()).append("\n");
        sb.append(showAllZombies()).append("\n");
        sb.append(showPlants()).append("\n");
        sb.append(showSuns()).append("\n");
        return sb.toString();
    }

    @Override
    public String ChangeMenu(String menuName) {
        return "";
    }
}

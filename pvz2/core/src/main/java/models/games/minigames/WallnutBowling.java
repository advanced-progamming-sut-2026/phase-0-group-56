package models.games.minigames;

import models.Constants;
import models.entity.Zombie;
import models.gameadventure.Chapters;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.gamepanes.Wave;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Wall-Nut Bowling gameplay model.
 *
 * <p>The available nuts are presented through a real conveyor belt. A used
 * slot is immediately replaced with a weighted random choice: Bowling Nut
 * (60%), Explosive Bowling Nut (30%), and Big Nut (10%). Earlier levels can
 * intentionally expose only the nut types they have unlocked.</p>
 */
public class WallnutBowling extends NormalGame {
    public static final String BOWLING_NUT = "Bowling Nut";
    public static final String EXPLOSIVE_BOWLING_NUT = "Explosive Bowling Nut";
    public static final String BIG_NUT = "Big Nut";

    private static final int CONVEYOR_CAPACITY = 5;
    private static final float BOWLING_WEIGHT = 60f;
    private static final float EXPLOSIVE_WEIGHT = 30f;
    private static final float BIG_WEIGHT = 10f;

    private final ArrayList<String> belt = new ArrayList<>();
    private final ArrayList<BowlingNut> nuts = new ArrayList<>();
    private final Random random = new Random();
    private final ArrayList<String> availableNutTypes = new ArrayList<>();

    public WallnutBowling(MinigameLevel level) {
        field = new Field().initField(Chapters.AncientEgypt, 0);
        field.getMoaners().clear();
        initialiseBelt(level);
        initialiseWaves(level);
        state = GameState.PLAYING;
    }

    private void initialiseBelt(MinigameLevel level) {
        // Minigame JSON uses BOWLING_WALLNUT and GIANT_WALLNUT, which are
        // minigame names rather than regular PlantType values. Use the level
        // id for progression and accept EXPLODE_O_NUT from hand-built levels.
        int progress = level == null ? 1 : Math.max(1, Math.min(3, level.getId() % 100));
        availableNutTypes.add(BOWLING_NUT);
        if (progress >= 2 || containsExplosive(level)) {
            availableNutTypes.add(EXPLOSIVE_BOWLING_NUT);
        }
        if (progress >= 3 || containsBig(level)) {
            availableNutTypes.add(BIG_NUT);
        }

        // Guarantee one card for every unlocked nut type. This keeps an
        // explosive/big nut plantable immediately while the remaining slots
        // still follow the weighted random distribution.
        belt.addAll(availableNutTypes);
        while (belt.size() < CONVEYOR_CAPACITY) {
            belt.add(randomNutType());
        }
        Collections.shuffle(belt, random);
    }

    private boolean containsExplosive(MinigameLevel level) {
        if (level == null || level.getPlants() == null) {
            return false;
        }
        return level.getPlants().stream().anyMatch(type ->
            type != null && type.name().equalsIgnoreCase("EXPLODE_O_NUT"));
    }

    private boolean containsBig(MinigameLevel level) {
        if (level == null || level.getPlants() == null) {
            return false;
        }
        return level.getPlants().stream().anyMatch(type ->
            type != null && type.name().equalsIgnoreCase("GIANT_WALLNUT"));
    }

    private String randomNutType() {
        if (availableNutTypes.size() == 1) {
            return availableNutTypes.get(0);
        }

        float roll = random.nextFloat() * (BOWLING_WEIGHT + EXPLOSIVE_WEIGHT + BIG_WEIGHT);
        if (roll < BOWLING_WEIGHT && availableNutTypes.contains(BOWLING_NUT)) {
            return BOWLING_NUT;
        }
        if (roll < BOWLING_WEIGHT + EXPLOSIVE_WEIGHT
            && availableNutTypes.contains(EXPLOSIVE_BOWLING_NUT)) {
            return EXPLOSIVE_BOWLING_NUT;
        }
        if (availableNutTypes.contains(BIG_NUT)) {
            return BIG_NUT;
        }
        if (availableNutTypes.contains(EXPLOSIVE_BOWLING_NUT)) {
            return EXPLOSIVE_BOWLING_NUT;
        }
        return BOWLING_NUT;
    }

    private void replenishConveyor() {
        while (belt.size() < CONVEYOR_CAPACITY) {
            belt.add(randomNutType());
        }
    }

    private void initialiseWaves(MinigameLevel level) {
        ArrayList<String> allowed = new ArrayList<>();
        if (level != null && level.getZombiesNames() != null) {
            for (String name : level.getZombiesNames()) {
                String normalised = normaliseZombieName(name);
                if (normalised != null && !normalised.isBlank()) {
                    allowed.add(normalised);
                }
            }
        }
        if (allowed.isEmpty()) {
            Collections.addAll(allowed, "normal", "cone", "bucket");
        }

        int progress = level == null ? 1 : Math.max(1, Math.min(3, level.getId() % 100));
        int waveCount = 2 + progress;
        for (int index = 0; index < waveCount; index++) {
            Wave wave = new Wave();
            wave.setId(index + 1);
            wave.setCost(350f + progress * 100f + index * 180f);
            wave.initWave(allowed);
            waves.add(wave);
        }

        if (!waves.isEmpty()) {
            currentWave = waves.get(0);
            waveID = 1;
            spawnWave(currentWave);
        }
    }

    private void spawnWave(Wave wave) {
        if (wave == null) {
            return;
        }

        int index = 0;
        for (Zombie zombie : wave.getZombies()) {
            if (zombie == null) {
                continue;
            }

            int row = index % 5;
            zombie.setLine(row);
            zombie.setTileIndex(8);
            // Wall-Nut Bowling is played from right to left.
            zombie.setSpeed(-Math.abs(zombie.getSpeed()));
            zombie.setPosition(
                9f * Tile.getWidth() + 100f + index * 18f,
                row * Tile.getHeight()
            );
            zombies.add(zombie);
            index++;
        }
    }

    @Override
    public Result check_endGame() {
        for (Zombie zombie : zombies) {
            if (zombie != null
                && !zombie.isDead()
                && zombie.getX() < Constants.WALLNUT_LIMIT_LINE * Tile.getWidth()) {
                state = GameState.END;
                return new Result(true, "Loss", null);
            }
        }
        if (won) {
            state = GameState.END;
            return new Result(true, "Won", null);
        }
        return new Result(false, null, null);
    }

    @Override
    public String playGame(float delta) {
        if (state != GameState.PLAYING) {
            return "";
        }

        float safeDelta = Math.max(0f, delta);
        for (BowlingNut nut : new ArrayList<>(nuts)) {
            nut.go(safeDelta, this);
        }
        updateZombies(safeDelta);
        Result result = attack(safeDelta);
        return result != null && result.message() != null ? result.message() : "";
    }

    @Override
    public boolean plant(String plantName, int x, int y) {
        if (x < 0 || x >= Constants.WALLNUT_LIMIT_LINE || y < 0 || y >= 5) {
            return false;
        }

        NutKind kind = NutKind.from(plantName);
        if (kind == null) {
            return false;
        }

        int slot = findBeltSlot(kind.displayName);
        if (slot < 0) {
            return false;
        }

        nuts.add(makeNut(kind, x, y));
        belt.remove(slot);
        replenishConveyor();
        return true;
    }

    private int findBeltSlot(String requested) {
        for (int index = 0; index < belt.size(); index++) {
            if (belt.get(index).equalsIgnoreCase(requested)) {
                return index;
            }
        }
        return -1;
    }

    private BowlingNut makeNut(NutKind kind, int x, int y) {
        BowlingNut bowling = kind == NutKind.BIG
            ? new BigNut(1500f)
            : new BowlingNut(1000f, kind == NutKind.EXPLOSIVE);
        bowling.setVelocityX(Constants.BOWLING_WALLNUT_VELOCITY);
        bowling.setVelocityY(
            Constants.BOWLING_WALLNUT_VELOCITY * 0.4f
                * (random.nextBoolean() ? 1f : -1f)
        );
        bowling.setTileIndex(x);
        bowling.setLine(y);
        return bowling;
    }

    public ArrayList<BowlingNut> getNuts() {
        return nuts;
    }

    /** Current conveyor contents, from left to right. */
    public List<String> getBelt() {
        return Collections.unmodifiableList(belt);
    }

    public List<String> getConveyorBelt() {
        return getBelt();
    }

    public boolean hasNutType(String type) {
        NutKind kind = NutKind.from(type);
        return kind != null && findBeltSlot(kind.displayName) >= 0;
    }

    private static String normaliseZombieName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT).replace(" ", "")) {
            case "regularzombie", "zombiedefault", "normal" -> "normal";
            case "coneheadzombie", "zombiearmor1", "cone" -> "cone";
            case "bucketheadzombie", "zombiearmor2", "bucket" -> "bucket";
            case "newspaperzombie", "newspaper" -> "newspaper";
            case "allstarzombie", "allstar" -> "allstar";
            case "knightzombie", "knight" -> "knight";
            case "gargantuar" -> "gargantuar";
            case "imp" -> "imp";
            case "wizardzombie", "wizard" -> "wizard";
            case "pianistzombie", "piano" -> "piano";
            case "impdragon", "dragonimp" -> "dragon_imp";
            case "kingzombie", "king" -> "king";
            default -> value;
        };
    }

    @Override
    protected Result attack(float delta) {
        if (currentWave == null || !waveCleared(currentWave)) {
            return new Result(false, null, null);
        }

        if (waveID >= waves.size()) {
            won = zombies.isEmpty();
            return new Result(false, null, null);
        }

        previousWave = currentWave;
        currentWave = waves.get(waveID);
        waveID += 1;
        spawnWave(currentWave);
        return new Result(true, "New wave", null);
    }

    private boolean waveCleared(Wave wave) {
        if (wave.getZombies() == null || wave.getZombies().isEmpty()) {
            return true;
        }
        for (Zombie zombie : wave.getZombies()) {
            if (zombie != null && !zombie.isDead()) {
                return false;
            }
        }
        return true;
    }

    private enum NutKind {
        BOWLING(BOWLING_NUT),
        EXPLOSIVE(EXPLOSIVE_BOWLING_NUT),
        BIG(BIG_NUT);

        private final String displayName;

        NutKind(String displayName) {
            this.displayName = displayName;
        }

        private static NutKind from(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
            return switch (normalized) {
                case "wallnut", "bowlingnut", "bowlingwallnut" -> BOWLING;
                case "explodonut", "explosivenut", "explosivebowlingnut",
                     "explosivewallnut", "explodingnut" -> EXPLOSIVE;
                case "bignut", "giantwallnut", "giantnut" -> BIG;
                default -> null;
            };
        }
    }
}

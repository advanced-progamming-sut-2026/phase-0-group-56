package models.games.minigames;

import models.entity.Plant;
import models.entity.Zombie;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gamepanes.Field;
import models.gamepanes.Tile;
import models.games.NormalGame;
import models.utils.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Reverse-role minigame in which the player buys and deploys zombies. */
public final class IZombie extends NormalGame {
    public static final int COLUMN_COUNT = 9;
    public static final int ROW_COUNT = 5;
    public static final int FIRST_ZOMBIE_COLUMN = 5;
    public static final int STARTING_SUN = 150;

    private static final int PRODUCED_SUN = 25;
    private static final float INITIAL_PRODUCTION_INTERVAL = 11f;
    private static final float MIN_PRODUCTION_INTERVAL = 3.5f;

    private final int levelNumber;
    private final Random random;
    private final List<ZombieCard> zombieCards;
    private final boolean[] brainsEaten = new boolean[ROW_COUNT];
    private final Set<Zombie> sunProducers = Collections.newSetFromMap(
        new IdentityHashMap<>()
    );
    private final Map<Zombie, Float> producerElapsed = new IdentityHashMap<>();
    private final Map<Zombie, Float> producerTimers = new IdentityHashMap<>();

    private boolean wonGame;
    private boolean lostGame;

    public IZombie(int levelNumber) {
        this(levelNumber, new Random());
    }

    IZombie(int levelNumber, Random random) {
        this.levelNumber = Math.max(1, Math.min(3, levelNumber));
        this.random = random == null ? new Random() : random;
        zombieCards = List.copyOf(cardsForLevel(this.levelNumber));
        initialiseGame();
    }

    private void initialiseGame() {
        plantsInField.clear();
        zombies.clear();
        projectiles.clear();
        suns.clear();
        sunProducers.clear();
        producerElapsed.clear();
        producerTimers.clear();
        for (int row = 0; row < ROW_COUNT; row++) {
            brainsEaten[row] = false;
        }
        wonGame = false;
        lostGame = false;
        won = false;
        chapter = Chapters.AncientEgypt;
        field = new Field().initField(chapter, 0);
        field.getMoaners().clear();
        sunCount = STARTING_SUN;
        state = GameState.PLAYING;
        createDefendingPlants();
        createSunProducers();
    }

    @Override
    public void initGame(Chapters ignoredChapter, models.gameadventure.levels.Level ignoredLevel) {
        initialiseGame();
    }

    @Override
    public boolean startGame(String ignored) {
        state = GameState.PLAYING;
        return true;
    }

    private void createDefendingPlants() {
        List<PlantType> types = plantTypesForLevel(levelNumber);
        for (int row = 0; row < ROW_COUNT; row++) {
            int count = 3 + (row + levelNumber) % 2;
            for (int index = 0; index < count; index++) {
                int column = 1 + index;
                PlantType type = types.get((row + index + random.nextInt(types.size())) % types.size());
                addPlant(type, column, row);
            }
        }
    }

    private void addPlant(PlantType type, int column, int row) {
        Plant plant = plantFactory.createPlant(type);
        plant.setTileIndex(column);
        plant.setLine(row);
        plantsInField.add(plant);
        field.getTileByCoordinats(column, row).setEmpty(false);
    }

    private void createSunProducers() {
        for (int row = 0; row < ROW_COUNT; row++) {
            Zombie producer = ZombieFactory.createZombie("bucket");
            producer.setSpeed(0f);
            producer.setLine(row);
            producer.setTileIndex(COLUMN_COUNT - 1);
            producer.setPosition(
                (COLUMN_COUNT - 0.5f) * Tile.getWidth(),
                row * Tile.getHeight()
            );
            zombies.add(producer);
            sunProducers.add(producer);
            producerElapsed.put(producer, 0f);
            producerTimers.put(producer, INITIAL_PRODUCTION_INTERVAL + row * 0.35f);
        }
    }

    @Override
    public String playGame(float delta) {
        if (state != GameState.PLAYING) {
            return "";
        }

        float safeDelta = Math.max(0f, delta);
        updatePlants(safeDelta);
        updateProjectiles(safeDelta);
        updateZombies(safeDelta);
        updateProducers(safeDelta);
        consumeReachedBrains();
        updateScene(safeDelta);
        evaluateEndState();

        if (wonGame) {
            return "All five brains were eaten.";
        }
        if (lostGame) {
            return "No sun producers or affordable zombies remain.";
        }
        return "";
    }

    public PlacementResult placeZombie(String type, int column, int row) {
        if (state != GameState.PLAYING) {
            return PlacementResult.GAME_OVER;
        }
        if (!isPlacementCell(column, row)) {
            return PlacementResult.INVALID_TILE;
        }
        if (brainsEaten[row]) {
            return PlacementResult.BRAIN_ALREADY_EATEN;
        }

        ZombieCard card = findCard(type);
        if (card == null) {
            return PlacementResult.NOT_AVAILABLE;
        }
        if (sunCount < card.cost()) {
            return PlacementResult.NOT_ENOUGH_SUN;
        }
        if (hasDeployedZombieAt(column, row)) {
            return PlacementResult.OCCUPIED;
        }

        Zombie zombie = ZombieFactory.createZombie(card.type());
        zombie.setLine(row);
        zombie.setTileIndex(column);
        zombie.setPosition(
            (column + 0.5f) * Tile.getWidth(),
            row * Tile.getHeight()
        );
        zombies.add(zombie);
        sunCount -= card.cost();
        return PlacementResult.PLACED;
    }

    private boolean hasDeployedZombieAt(int column, int row) {
        float targetX = (column + 0.5f) * Tile.getWidth();
        for (Zombie zombie : zombies) {
            if (sunProducers.contains(zombie) || zombie.isDead() || zombie.getLine() != row) {
                continue;
            }
            if (Math.abs(zombie.getX() - targetX) < Tile.getWidth() * 0.45f) {
                return true;
            }
        }
        return false;
    }

    private ZombieCard findCard(String type) {
        if (type == null) {
            return null;
        }
        for (ZombieCard card : zombieCards) {
            if (card.type().equalsIgnoreCase(type)) {
                return card;
            }
        }
        return null;
    }

    private void updateProducers(float delta) {
        sunProducers.removeIf(zombie -> zombie == null || zombie.isDead() || !zombies.contains(zombie));
        producerElapsed.keySet().removeIf(zombie -> !sunProducers.contains(zombie));
        producerTimers.keySet().removeIf(zombie -> !sunProducers.contains(zombie));

        for (Zombie producer : sunProducers) {
            float elapsed = producerElapsed.getOrDefault(producer, 0f) + delta;
            float timer = producerTimers.getOrDefault(producer, INITIAL_PRODUCTION_INTERVAL) - delta;
            producerElapsed.put(producer, elapsed);

            if (timer <= 0f) {
                sunCount += PRODUCED_SUN;
                timer = Math.max(
                    MIN_PRODUCTION_INTERVAL,
                    INITIAL_PRODUCTION_INTERVAL - elapsed * 0.075f
                );
            }
            producerTimers.put(producer, timer);
        }
    }

    private void consumeReachedBrains() {
        ArrayList<Zombie> reached = new ArrayList<>();
        for (Zombie zombie : zombies) {
            int row = zombie.getLine();
            if (sunProducers.contains(zombie) || zombie.isDead() || row < 0 || row >= ROW_COUNT) {
                continue;
            }
            if (zombie.getX() <= Tile.getWidth() * 0.10f) {
                brainsEaten[row] = true;
                reached.add(zombie);
            }
        }
        zombies.removeAll(reached);
    }

    private void evaluateEndState() {
        wonGame = getBrainsEatenCount() == ROW_COUNT;
        if (wonGame) {
            won = true;
            state = GameState.END;
            return;
        }

        boolean canProduceMoreSun = !sunProducers.isEmpty();
        boolean hasDeployedZombie = getDeployedZombieCount() > 0;
        lostGame = !canProduceMoreSun
            && !hasDeployedZombie
            && sunCount < getCheapestZombieCost();
        if (lostGame) {
            state = GameState.END;
        }
    }

    private int getDeployedZombieCount() {
        int count = 0;
        for (Zombie zombie : zombies) {
            if (!sunProducers.contains(zombie) && !zombie.isDead()) {
                count++;
            }
        }
        return count;
    }

    public int getCheapestZombieCost() {
        int cheapest = Integer.MAX_VALUE;
        for (ZombieCard card : zombieCards) {
            cheapest = Math.min(cheapest, card.cost());
        }
        return cheapest == Integer.MAX_VALUE ? 0 : cheapest;
    }

    public boolean isSunProducer(Zombie zombie) {
        return sunProducers.contains(zombie);
    }

    public List<Zombie> getSunProducers() {
        return List.copyOf(sunProducers);
    }

    public List<ZombieCard> getZombieCards() {
        return zombieCards;
    }

    public boolean isBrainEaten(int row) {
        return row >= 0 && row < ROW_COUNT && brainsEaten[row];
    }

    public int getBrainsEatenCount() {
        int count = 0;
        for (boolean eaten : brainsEaten) {
            if (eaten) {
                count++;
            }
        }
        return count;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isWonGame() {
        return wonGame;
    }

    public boolean isLostGame() {
        return lostGame;
    }

    private static boolean isPlacementCell(int column, int row) {
        return column >= FIRST_ZOMBIE_COLUMN
            && column < COLUMN_COUNT
            && row >= 0
            && row < ROW_COUNT;
    }

    private static List<PlantType> plantTypesForLevel(int level) {
        return switch (level) {
            case 2 -> List.of(
                PlantType.REPEATER,
                PlantType.CABBAGE_PULT,
                PlantType.TALL_NUT,
                PlantType.CHOMPER,
                PlantType.SNOW_PEA
            );
            case 3 -> List.of(
                PlantType.WINTER_MELON,
                PlantType.TORCHWOOD,
                PlantType.TALL_NUT,
                PlantType.REPEATER,
                PlantType.MELON_PULT
            );
            default -> List.of(
                PlantType.PEASHOOTER,
                PlantType.CABBAGE_PULT,
                PlantType.WALL_NUT,
                PlantType.POTATO_MINE,
                PlantType.SNOW_PEA
            );
        };
    }

    private static List<ZombieCard> cardsForLevel(int level) {
        return switch (level) {
            case 2 -> List.of(
                new ZombieCard("knight", 150),
                new ZombieCard("brick", 175),
                new ZombieCard("allstar", 150),
                new ZombieCard("parasol", 100),
                new ZombieCard("explorer", 125)
            );
            case 3 -> List.of(
                new ZombieCard("gargantuar", 300),
                new ZombieCard("prospector", 125),
                new ZombieCard("dodo", 150),
                new ZombieCard("snorkel", 125),
                new ZombieCard("juggler", 150)
            );
            default -> List.of(
                new ZombieCard("normal", 50),
                new ZombieCard("cone", 75),
                new ZombieCard("imp", 50),
                new ZombieCard("newspaper", 100),
                new ZombieCard("prospector", 125)
            );
        };
    }

    @Override
    public Result check_endGame() {
        if (wonGame) {
            return new Result(true, "Won", null);
        }
        if (lostGame) {
            return new Result(true, "Loss", null);
        }
        return new Result(false, null, null);
    }

    public record ZombieCard(String type, int cost) {
        public ZombieCard {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Zombie type cannot be blank.");
            }
            if (cost <= 0) {
                throw new IllegalArgumentException("Zombie cost must be positive.");
            }
        }
    }

    public enum PlacementResult {
        PLACED,
        INVALID_TILE,
        BRAIN_ALREADY_EATEN,
        NOT_AVAILABLE,
        NOT_ENOUGH_SUN,
        OCCUPIED,
        GAME_OVER
    }
}

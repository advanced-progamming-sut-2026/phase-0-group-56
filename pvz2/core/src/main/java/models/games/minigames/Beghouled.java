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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Match-three minigame played on top of the normal plant/zombie simulation. */
public final class Beghouled extends NormalGame {
    public static final int COLUMN_COUNT = 9;
    public static final int ROW_COUNT = 5;

    private static final int SUN_PER_MATCHED_PLANT = 25;
    private static final float FIRST_SPAWN_DELAY = 4f;
    private static final float MIN_SPAWN_INTERVAL = 2.25f;
    private static final int MAX_CASCADE_DEPTH = 20;

    private final int levelNumber;
    private final Random random;
    private final List<PlantType> boardTypes;
    private final Map<PlantType, Upgrade> upgrades;

    private float elapsedTime;
    private float spawnTimer = FIRST_SPAWN_DELAY;
    private int score;
    private int bestCombo;
    private int lastCombo;
    private int totalMatched;
    private boolean lost;

    public Beghouled(int levelNumber) {
        this(levelNumber, new Random());
    }

    Beghouled(int levelNumber, Random random) {
        this.levelNumber = Math.max(1, Math.min(3, levelNumber));
        this.random = random == null ? new Random() : random;
        boardTypes = List.copyOf(typesForLevel(this.levelNumber));
        upgrades = buildUpgrades();
        initialiseBoard();
    }

    private void initialiseBoard() {
        plantsInField.clear();
        zombies.clear();
        projectiles.clear();
        suns.clear();
        elapsedTime = 0f;
        spawnTimer = FIRST_SPAWN_DELAY;
        score = 0;
        bestCombo = 0;
        lastCombo = 0;
        totalMatched = 0;
        lost = false;
        won = false;
        sunCount = 0;
        chapter = Chapters.AncientEgypt;
        field = new Field().initField(chapter, 0);
        field.getMoaners().clear();
        state = GameState.PLAYING;
        rebuildStableBoard();
    }

    @Override
    public void initGame(Chapters ignoredChapter, models.gameadventure.levels.Level ignoredLevel) {
        initialiseBoard();
    }

    @Override
    public boolean startGame(String ignored) {
        state = GameState.PLAYING;
        return true;
    }

    @Override
    public String playGame(float delta) {
        if (state != GameState.PLAYING || lost) {
            return "";
        }

        float safeDelta = Math.max(0f, delta);
        elapsedTime += safeDelta;
        updateProjectiles(safeDelta);
        updatePlants(safeDelta);
        updateZombies(safeDelta);
        spawnEndlessZombie(safeDelta);
        updateScene(safeDelta);

        if (hasZombieReachedHouse()) {
            lost = true;
            state = GameState.END;
            return "The zombies reached the house.";
        }
        return "";
    }

    @Override
    public void updatePlants(float delta) {
        super.updatePlants(delta);
        if (plantsInField.size() < COLUMN_COUNT * ROW_COUNT) {
            refillBoard(true);
            Set<Cell> automaticMatches = findMatches();
            if (!automaticMatches.isEmpty()) {
                resolveCascades(automaticMatches, false);
            }
            ensurePlayableBoard();
        }
    }

    /** Swaps two adjacent plants only when the swap creates at least one match. */
    public SwapResult swap(int firstColumn, int firstRow, int secondColumn, int secondRow) {
        if (state != GameState.PLAYING) {
            return SwapResult.GAME_OVER;
        }
        if (!isInside(firstColumn, firstRow) || !isInside(secondColumn, secondRow)) {
            return SwapResult.OUT_OF_BOUNDS;
        }
        if (Math.abs(firstColumn - secondColumn) + Math.abs(firstRow - secondRow) != 1) {
            return SwapResult.NOT_ADJACENT;
        }

        Plant first = getPlantAt(firstRow, firstColumn);
        Plant second = getPlantAt(secondRow, secondColumn);
        if (first == null || second == null) {
            return SwapResult.EMPTY_TILE;
        }

        swapPositions(first, second);
        Set<Cell> matches = findMatches();
        if (matches.isEmpty()) {
            swapPositions(first, second);
            return SwapResult.NO_MATCH;
        }

        resolveCascades(matches, true);
        ensurePlayableBoard();
        return SwapResult.MATCHED;
    }

    public UpgradeResult upgrade(PlantType from) {
        Upgrade upgrade = upgrades.get(from);
        if (upgrade == null || !containsType(from)) {
            return UpgradeResult.NOT_AVAILABLE;
        }
        if (sunCount < upgrade.cost()) {
            return UpgradeResult.NOT_ENOUGH_SUN;
        }

        sunCount -= upgrade.cost();
        replaceAll(from, upgrade.to());
        Set<Cell> matches = findMatches();
        if (!matches.isEmpty()) {
            resolveCascades(matches, true);
        }
        ensurePlayableBoard();
        return UpgradeResult.UPGRADED;
    }

    public List<Upgrade> getAvailableUpgrades() {
        ArrayList<Upgrade> available = new ArrayList<>();
        for (Upgrade upgrade : upgrades.values()) {
            if (containsType(upgrade.from())) {
                available.add(upgrade);
            }
        }
        return Collections.unmodifiableList(available);
    }

    private void resolveCascades(Set<Cell> firstMatches, boolean reward) {
        Set<Cell> matches = firstMatches;
        int combo = 0;

        while (!matches.isEmpty() && combo < MAX_CASCADE_DEPTH) {
            combo++;
            int removed = removeMatches(matches);
            if (reward) {
                int multiplier = Math.min(combo, 5);
                sunCount += removed * SUN_PER_MATCHED_PLANT;
                score += removed * 100 * multiplier;
                totalMatched += removed;
            }
            refillBoard(false);
            matches = findMatches();
        }

        if (!matches.isEmpty()) {
            rebuildStableBoard();
        }

        if (reward) {
            lastCombo = combo;
            bestCombo = Math.max(bestCombo, combo);
        }
    }

    private int removeMatches(Set<Cell> matches) {
        int before = plantsInField.size();
        plantsInField.removeIf(plant -> matches.contains(
            new Cell(plant.getTileIndex(), plant.getLine())
        ));
        for (Cell cell : matches) {
            field.getTileByCoordinats(cell.column(), cell.row()).setEmpty(true);
        }
        return before - plantsInField.size();
    }

    private void refillBoard(boolean avoidMatches) {
        for (int column = 0; column < COLUMN_COUNT; column++) {
            collapseColumn(column);
            for (int row = columnPlantCount(column); row < ROW_COUNT; row++) {
                PlantType type = avoidMatches
                    ? chooseStableType(column, row)
                    : randomBoardType();
                addPlant(type, column, row);
            }
        }
    }

    private void collapseColumn(int column) {
        ArrayList<Plant> columnPlants = new ArrayList<>();
        for (int row = 0; row < ROW_COUNT; row++) {
            Plant plant = getPlantAt(row, column);
            if (plant != null) {
                columnPlants.add(plant);
            }
        }
        for (int row = 0; row < ROW_COUNT; row++) {
            field.getTileByCoordinats(column, row).setEmpty(true);
        }
        for (int row = 0; row < columnPlants.size(); row++) {
            placePlant(columnPlants.get(row), column, row);
        }
    }

    private int columnPlantCount(int column) {
        int count = 0;
        for (Plant plant : plantsInField) {
            if (plant.getTileIndex() == column) {
                count++;
            }
        }
        return count;
    }

    private void rebuildStableBoard() {
        for (int attempt = 0; attempt < 100; attempt++) {
            plantsInField.clear();
            markAllTilesEmpty();
            refillBoard(true);
            if (findMatches().isEmpty() && hasValidMove()) {
                return;
            }
        }
        forceKnownPlayablePattern();
    }

    private void markAllTilesEmpty() {
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                field.getTileByCoordinats(column, row).setEmpty(true);
            }
        }
    }

    private PlantType chooseStableType(int column, int row) {
        ArrayList<PlantType> candidates = new ArrayList<>(boardTypes);
        Collections.shuffle(candidates, random);
        for (PlantType candidate : candidates) {
            if (!wouldCreateMatch(candidate, column, row)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private boolean wouldCreateMatch(PlantType type, int column, int row) {
        Plant leftOne = getPlantAt(row, column - 1);
        Plant leftTwo = getPlantAt(row, column - 2);
        if (sameType(type, leftOne) && sameType(type, leftTwo)) {
            return true;
        }
        Plant downOne = getPlantAt(row - 1, column);
        Plant downTwo = getPlantAt(row - 2, column);
        return sameType(type, downOne) && sameType(type, downTwo);
    }

    private boolean sameType(PlantType type, Plant plant) {
        return plant != null && plant.getType() == type;
    }

    private void ensurePlayableBoard() {
        if (!hasValidMove()) {
            rebuildStableBoard();
        }
    }

    public boolean hasValidMove() {
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                if (column + 1 < COLUMN_COUNT && createsMatchAfterSwap(
                    column, row, column + 1, row
                )) {
                    return true;
                }
                if (row + 1 < ROW_COUNT && createsMatchAfterSwap(
                    column, row, column, row + 1
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean createsMatchAfterSwap(int firstColumn, int firstRow, int secondColumn, int secondRow) {
        Plant first = getPlantAt(firstRow, firstColumn);
        Plant second = getPlantAt(secondRow, secondColumn);
        if (first == null || second == null || first.getType() == second.getType()) {
            return false;
        }
        swapPositions(first, second);
        boolean match = !findMatches().isEmpty();
        swapPositions(first, second);
        return match;
    }

    private Set<Cell> findMatches() {
        Set<Cell> matches = new HashSet<>();
        findHorizontalMatches(matches);
        findVerticalMatches(matches);
        return matches;
    }

    private void findHorizontalMatches(Set<Cell> matches) {
        for (int row = 0; row < ROW_COUNT; row++) {
            int runStart = 0;
            for (int column = 1; column <= COLUMN_COUNT; column++) {
                if (column < COLUMN_COUNT && sameCellType(column, row, runStart, row)) {
                    continue;
                }
                addRun(matches, runStart, column, row, true);
                runStart = column;
            }
        }
    }

    private void findVerticalMatches(Set<Cell> matches) {
        for (int column = 0; column < COLUMN_COUNT; column++) {
            int runStart = 0;
            for (int row = 1; row <= ROW_COUNT; row++) {
                if (row < ROW_COUNT && sameCellType(column, row, column, runStart)) {
                    continue;
                }
                addRun(matches, runStart, row, column, false);
                runStart = row;
            }
        }
    }

    private boolean sameCellType(int firstColumn, int firstRow, int secondColumn, int secondRow) {
        Plant first = getPlantAt(firstRow, firstColumn);
        Plant second = getPlantAt(secondRow, secondColumn);
        return first != null && second != null && first.getType() == second.getType();
    }

    private void addRun(Set<Cell> matches, int start, int end, int fixed, boolean horizontal) {
        if (end - start < 3) {
            return;
        }
        for (int index = start; index < end; index++) {
            matches.add(horizontal ? new Cell(index, fixed) : new Cell(fixed, index));
        }
    }

    private void swapPositions(Plant first, Plant second) {
        int firstColumn = first.getTileIndex();
        int firstRow = first.getLine();
        placePlant(first, second.getTileIndex(), second.getLine());
        placePlant(second, firstColumn, firstRow);
    }

    private void placePlant(Plant plant, int column, int row) {
        plant.setTileIndex(column);
        plant.setLine(row);
        field.getTileByCoordinats(column, row).setEmpty(false);
    }

    private void addPlant(PlantType type, int column, int row) {
        Plant plant = plantFactory.createPlant(type);
        placePlant(plant, column, row);
        plantsInField.add(plant);
    }

    private void replaceAll(PlantType from, PlantType to) {
        ArrayList<Cell> positions = new ArrayList<>();
        plantsInField.removeIf(plant -> {
            if (plant.getType() != from) {
                return false;
            }
            positions.add(new Cell(plant.getTileIndex(), plant.getLine()));
            return true;
        });
        for (Cell cell : positions) {
            addPlant(to, cell.column(), cell.row());
        }
    }

    private boolean containsType(PlantType type) {
        for (Plant plant : plantsInField) {
            if (plant.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private void spawnEndlessZombie(float delta) {
        spawnTimer -= delta;
        if (spawnTimer > 0f) {
            return;
        }

        Zombie zombie = ZombieFactory.createZombie(chooseZombieType());
        int row = random.nextInt(ROW_COUNT);
        zombie.setLine(row);
        zombie.setTileIndex(COLUMN_COUNT - 1);
        zombie.setPosition(COLUMN_COUNT * Tile.getWidth() + 100f, row * Tile.getHeight());
        zombies.add(zombie);

        float interval = Math.max(MIN_SPAWN_INTERVAL, 7f - elapsedTime * 0.018f);
        spawnTimer = interval * (0.82f + random.nextFloat() * 0.36f);
    }

    private String chooseZombieType() {
        if (elapsedTime > 100f && random.nextFloat() < 0.18f) {
            return "bucket";
        }
        if (elapsedTime > 45f && random.nextFloat() < 0.32f) {
            return "cone";
        }
        return "normal";
    }

    private boolean hasZombieReachedHouse() {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && zombie.getX() <= -35f) {
                return true;
            }
        }
        return false;
    }

    private PlantType randomBoardType() {
        return boardTypes.get(random.nextInt(boardTypes.size()));
    }

    private void forceKnownPlayablePattern() {
        plantsInField.clear();
        markAllTilesEmpty();
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < COLUMN_COUNT; column++) {
                int typeIndex = (column + row * 2) % boardTypes.size();
                addPlant(boardTypes.get(typeIndex), column, row);
            }
        }
    }

    private static boolean isInside(int column, int row) {
        return column >= 0 && column < COLUMN_COUNT && row >= 0 && row < ROW_COUNT;
    }

    private static List<PlantType> typesForLevel(int level) {
        return switch (level) {
            case 2 -> List.of(
                PlantType.PEASHOOTER,
                PlantType.WALL_NUT,
                PlantType.PUFF_SHROOM,
                PlantType.CABBAGE_PULT,
                PlantType.REPEATER
            );
            case 3 -> List.of(
                PlantType.REPEATER,
                PlantType.TALL_NUT,
                PlantType.CABBAGE_PULT,
                PlantType.MELON_PULT,
                PlantType.WINTER_MELON
            );
            default -> List.of(
                PlantType.PEASHOOTER,
                PlantType.WALL_NUT,
                PlantType.CHOMPER,
                PlantType.SNOW_PEA,
                PlantType.POTATO_MINE
            );
        };
    }

    private static Map<PlantType, Upgrade> buildUpgrades() {
        EnumMap<PlantType, Upgrade> result = new EnumMap<>(PlantType.class);
        addUpgrade(result, PlantType.PEASHOOTER, PlantType.REPEATER, 500);
        addUpgrade(result, PlantType.REPEATER, PlantType.MEGA_GATLING_PEA, 1500);
        addUpgrade(result, PlantType.WALL_NUT, PlantType.TALL_NUT, 500);
        addUpgrade(result, PlantType.PUFF_SHROOM, PlantType.FUM_SHROOM, 250);
        addUpgrade(result, PlantType.CABBAGE_PULT, PlantType.MELON_PULT, 1000);
        addUpgrade(result, PlantType.MELON_PULT, PlantType.WINTER_MELON, 750);
        return Collections.unmodifiableMap(result);
    }

    private static void addUpgrade(
        Map<PlantType, Upgrade> map,
        PlantType from,
        PlantType to,
        int cost
    ) {
        map.put(from, new Upgrade(from, to, cost));
    }

    @Override
    public Result check_endGame() {
        return lost
            ? new Result(true, "Loss", null)
            : new Result(false, null, null);
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getScore() {
        return score;
    }

    public int getBestCombo() {
        return bestCombo;
    }

    public int getLastCombo() {
        return lastCombo;
    }

    public int getTotalMatched() {
        return totalMatched;
    }

    public boolean isLost() {
        return lost;
    }

    public record Upgrade(PlantType from, PlantType to, int cost) {
    }

    private record Cell(int column, int row) {
    }

    public enum SwapResult {
        MATCHED,
        NO_MATCH,
        NOT_ADJACENT,
        EMPTY_TILE,
        OUT_OF_BOUNDS,
        GAME_OVER
    }

    public enum UpgradeResult {
        UPGRADED,
        NOT_ENOUGH_SUN,
        NOT_AVAILABLE
    }
}

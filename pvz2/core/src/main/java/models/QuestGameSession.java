package models;

import controllers.datacontroller.Data;
import models.entity.LawnMower;
import models.entity.Plant;
import models.entity.PlantCategory;
import models.entity.PlantTags;
import models.entity.Zombie;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.BaseGame;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Keeps the temporary statistics of one level and converts them to quest events.
 */
public final class QuestGameSession {

    private static final int ROW_COUNT = 5;
    private static final int COLUMN_COUNT = 9;

    private final BaseGame game;
    private final Chapters chapter;
    private final Level level;

    private final Set<Zombie> observedZombies =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<Zombie> countedZombies =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<Plant> observedPlants =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<Plant> countedLostPlants =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private final Set<PlantType> offensivePlantTypes =
        EnumSet.noneOf(PlantType.class);

    private final Set<PlantCategory> offensiveFamilies =
        EnumSet.noneOf(PlantCategory.class);

    private float elapsedTime;
    private float firstWaveStartTime = -1f;
    private int plantsLost;
    private boolean plantedMushroom;
    private boolean winEvaluated;

    public QuestGameSession(
        BaseGame game,
        Chapters chapter,
        Level level
    ) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        this.game = game;
        this.chapter = chapter;
        this.level = level;
    }

    public void onGameStarted() {
        elapsedTime = 0f;
        firstWaveStartTime = -1f;
        plantsLost = 0;
        plantedMushroom = false;
        winEvaluated = false;

        observedZombies.clear();
        countedZombies.clear();
        observedPlants.clear();
        countedLostPlants.clear();
        offensivePlantTypes.clear();
        offensiveFamilies.clear();
    }

    public void beforeUpdate() {
        observedZombies.addAll(game.getZombies());
        observedPlants.addAll(game.getPlantsInField());
    }

    public void afterUpdate(float delta) {
        elapsedTime += Math.max(0f, delta);

        if (firstWaveStartTime < 0f && game.getWaveID() > 0) {
            firstWaveStartTime = elapsedTime;
        }

        countNewZombieDeaths();
        countNewPlantLosses();
    }

    public void onSunCollected(int amount) {
        QuestProgress.add(
            "COLLECT_SUN",
            Math.max(0, amount)
        );
    }

    public void onPlantPlaced(Plant plant) {
        if (plant == null) {
            return;
        }

        observedPlants.add(plant);

        if (plant.getTags() != null) {
            if (plant.getTags().contains(PlantTags.EXPLOSIVE)) {
                QuestProgress.add("USE_EXPLOSIVE_PLANT", 1);
            }

            if (plant.getTags().contains(PlantTags.Shroom)) {
                plantedMushroom = true;
            }
        }

        if (isOffensivePlant(plant)) {
            if (plant.getType() != null) {
                offensivePlantTypes.add(plant.getType());
            }

            if (plant.getCategory() != null) {
                offensiveFamilies.add(plant.getCategory());
            }
        }
    }

    public void onGameWon() {
        if (winEvaluated) {
            return;
        }

        winEvaluated = true;

        if (plantsLost <= 5) {
            QuestProgress.complete("ECONOMIC_WIN");
        }

        if (game.getSunCount() == 0) {
            QuestProgress.complete("FINISH_WITH_ZERO_SUN");
        }

        User user = Data.getCurrentUser();

        if (user != null && user.getDifficultyLevel() == 5) {
            QuestProgress.add("MAX_DIFFICULTY_WIN", 1);
        }

        if (isSymmetricLawn()) {
            QuestProgress.complete("SYMMETRIC_WIN");
        } else if (isAsymmetricLawn()) {
            QuestProgress.complete("ASYMMETRIC_WIN");
        }

        if (offensiveFamilies.size() == 1) {
            QuestProgress.complete("ONLY_FAMILY_WIN");
        }

        if (game.isDay() && plantedMushroom) {
            QuestProgress.complete("DAY_LEVEL_WITH_MUSHROOMS");
        }

        int producerCount = countFinalSunProducers();

        if (producerCount > 0 && producerCount <= 3) {
            QuestProgress.complete("THREE_PRODUCER_WIN");
        }

        boolean emptyRow = hasEmptyRow();
        boolean emptyColumn = hasEmptyColumn();

        if (emptyRow) {
            QuestProgress.complete("EMPTY_ROW_WIN");
        }

        if (emptyColumn) {
            QuestProgress.complete("EMPTY_COLUMN_WIN");
        }

        if (emptyRow && emptyColumn) {
            QuestProgress.complete("EMPTY_CROSS_WIN");
        }
    }

    private void countNewZombieDeaths() {
        for (Zombie zombie : observedZombies) {
            if (zombie == null
                || countedZombies.contains(zombie)
                || (!zombie.isDead() && zombie.getHp() > 0)) {
                continue;
            }

            countedZombies.add(zombie);

            QuestProgress.add("KILL_CHAPTER_ZOMBIE", 1);

            if (isInsideEarlyWaveWindow()) {
                QuestProgress.add("EARLY_WAVE_KILL", 1);
            }

            if (offensivePlantTypes.size() == 1) {
                QuestProgress.add("ONLY_SELECTED_PLANT_KILL", 1);

                if (offensivePlantTypes.contains(PlantType.CACTUS)) {
                    QuestProgress.add("ONLY_CACTUS_KILL", 1);
                }
            }

            if (zombie.getTileIndex() == 0 && isMowerUsed(zombie.getLine())) {
                QuestProgress.add("DANGER_COLUMN_KILL", 1);
            }
        }
    }

    private void countNewPlantLosses() {
        for (Plant plant : observedPlants) {
            if (plant == null
                || countedLostPlants.contains(plant)
                || game.getPlantsInField().contains(plant)
                || plant.getHp() > 0) {
                continue;
            }

            countedLostPlants.add(plant);
            plantsLost++;
        }
    }

    private boolean isInsideEarlyWaveWindow() {
        return firstWaveStartTime >= 0f
            && elapsedTime - firstWaveStartTime <= 30f;
    }

    private boolean isMowerUsed(int row) {
        if (game.getField() == null
            || row < 0
            || row >= game.getField().getMoaners().size()) {
            return false;
        }

        LawnMower mower = game.getField().getMoaners().get(row);
        return mower != null && mower.isUsed();
    }

    private boolean isOffensivePlant(Plant plant) {
        if (plant.getDamage() > 0) {
            return true;
        }

        if (plant.getTags() != null
            && (plant.getTags().contains(PlantTags.EXPLOSIVE)
            || plant.getTags().contains(PlantTags.Insta_kill)
            || plant.getTags().contains(PlantTags.Pea))) {
            return true;
        }

        PlantCategory category = plant.getCategory();

        return category == PlantCategory.SHOOTER
            || category == PlantCategory.Explosive
            || category == PlantCategory.StrikeThrough;
    }

    private PlantType[][] buildFinalGrid() {
        PlantType[][] grid =
            new PlantType[ROW_COUNT][COLUMN_COUNT];

        for (Plant plant : game.getPlantsInField()) {
            if (plant == null || plant.getHp() <= 0) {
                continue;
            }

            int row = plant.getLine();
            int column = plant.getTileIndex();

            if (row >= 0
                && row < ROW_COUNT
                && column >= 0
                && column < COLUMN_COUNT) {
                grid[row][column] = plant.getType();
            }
        }

        return grid;
    }

    private boolean isSymmetricLawn() {
        PlantType[][] grid = buildFinalGrid();

        for (int row = 0; row < ROW_COUNT / 2; row++) {
            int oppositeRow = ROW_COUNT - 1 - row;

            for (int column = 0; column < COLUMN_COUNT; column++) {
                if (grid[row][column] != grid[oppositeRow][column]) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAsymmetricLawn() {
        PlantType[][] grid = buildFinalGrid();

        for (int row = 0; row < ROW_COUNT / 2; row++) {
            int oppositeRow = ROW_COUNT - 1 - row;
            boolean pairHasDifference = false;

            for (int column = 0; column < COLUMN_COUNT; column++) {
                if (grid[row][column] != grid[oppositeRow][column]) {
                    pairHasDifference = true;
                    break;
                }
            }

            if (!pairHasDifference) {
                return false;
            }
        }

        return true;
    }

    private boolean hasEmptyRow() {
        boolean[] occupied = new boolean[ROW_COUNT];

        for (Plant plant : game.getPlantsInField()) {
            if (plant != null
                && plant.getHp() > 0
                && plant.getLine() >= 0
                && plant.getLine() < ROW_COUNT) {
                occupied[plant.getLine()] = true;
            }
        }

        for (boolean value : occupied) {
            if (!value) {
                return true;
            }
        }

        return false;
    }

    private boolean hasEmptyColumn() {
        boolean[] occupied = new boolean[COLUMN_COUNT];

        for (Plant plant : game.getPlantsInField()) {
            if (plant != null
                && plant.getHp() > 0
                && plant.getTileIndex() >= 0
                && plant.getTileIndex() < COLUMN_COUNT) {
                occupied[plant.getTileIndex()] = true;
            }
        }

        for (boolean value : occupied) {
            if (!value) {
                return true;
            }
        }

        return false;
    }

    private int countFinalSunProducers() {
        int count = 0;

        for (Plant plant : game.getPlantsInField()) {
            if (plant != null
                && plant.getHp() > 0
                && plant.getTags() != null
                && plant.getTags().contains(PlantTags.SUN)) {
                count++;
            }
        }

        return count;
    }

}

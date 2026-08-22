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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Collects one level's temporary statistics and emits exact quest events. */
public final class QuestGameSession {
    private static final int ROW_COUNT = 5;
    private static final int COLUMN_COUNT = 9;

    private static final String[] SINGLE_LEVEL_COUNTERS = {
        "USE_EXPLOSIVE_PLANT",
        "EARLY_WAVE_KILL",
        "ONLY_SELECTED_PLANT_KILL",
        "ONLY_CACTUS_KILL"
    };

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
    private final Set<PlantCategory> usedFamilies =
        EnumSet.noneOf(PlantCategory.class);

    private final boolean[] plantedRows = new boolean[ROW_COUNT];
    private final boolean[] plantedColumns = new boolean[COLUMN_COUNT];

    private Map<String, Float> startingQuestProgress = Collections.emptyMap();
    private float elapsedTime;
    private float firstWaveStartTime = -1f;
    private int plantsLost;
    private int sunProducerPlantsUsed;
    private boolean plantedMushroom;
    private boolean winEvaluated;
    private boolean cheatUsed;

    public QuestGameSession(BaseGame game, Chapters chapter, Level level) {
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
        sunProducerPlantsUsed = 0;
        plantedMushroom = false;
        winEvaluated = false;
        cheatUsed = false;

        observedZombies.clear();
        countedZombies.clear();
        observedPlants.clear();
        countedLostPlants.clear();
        offensivePlantTypes.clear();
        offensiveFamilies.clear();
        usedFamilies.clear();
        Arrays.fill(plantedRows, false);
        Arrays.fill(plantedColumns, false);

        QuestProgress.setSuppressed(false);
        QuestProgress.resetActions(SINGLE_LEVEL_COUNTERS);
        startingQuestProgress = QuestProgress.snapshotProgress();
    }

    public void beforeUpdate() {
        if (cheatUsed) {
            return;
        }

        observedZombies.addAll(game.getZombies());
        observedPlants.addAll(game.getPlantsInField());
    }

    public void afterUpdate(float delta) {
        elapsedTime += Math.max(0f, delta);

        if (cheatUsed) {
            return;
        }

        if (firstWaveStartTime < 0f && game.getWaveID() > 0) {
            firstWaveStartTime = elapsedTime;
        }

        countNewZombieDeaths();
        countNewPlantLosses();
    }

    public void onSunCollected(int amount) {
        if (!cheatUsed) {
            QuestProgress.add("COLLECT_SUN", Math.max(0, amount));
        }
    }

    public void onPlantPlaced(Plant plant) {
        if (cheatUsed || plant == null) {
            return;
        }

        observedPlants.add(plant);

        if (plant.getLine() >= 0 && plant.getLine() < ROW_COUNT) {
            plantedRows[plant.getLine()] = true;
        }

        if (plant.getTileIndex() >= 0 && plant.getTileIndex() < COLUMN_COUNT) {
            plantedColumns[plant.getTileIndex()] = true;
        }

        if (plant.getCategory() != null) {
            usedFamilies.add(plant.getCategory());
        }

        if (plant.getTags() != null) {
            if (plant.getTags().contains(PlantTags.EXPLOSIVE)) {
                QuestProgress.add("USE_EXPLOSIVE_PLANT", 1);
            }

            if (plant.getTags().contains(PlantTags.Shroom)) {
                plantedMushroom = true;
            }

            if (plant.getTags().contains(PlantTags.SUN)) {
                sunProducerPlantsUsed++;
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
        if (winEvaluated || cheatUsed) {
            return;
        }

        winEvaluated = true;

        if (plantsLost <= configuredNumber("ECONOMIC_WIN", 5)) {
            QuestProgress.complete("ECONOMIC_WIN");
        }

        if (game.getSunCount() == 0) {
            QuestProgress.complete("FINISH_WITH_ZERO_SUN");
        }

        User user = Data.getCurrentUser();

        if (user != null && user.getDifficultyLevel() == 5) {
            QuestProgress.add("MAX_DIFFICULTY_WIN", 1);
        } else {
            QuestProgress.setProgress("MAX_DIFFICULTY_WIN", 0f);
        }

        if (isSymmetricLawn()) {
            QuestProgress.complete("SYMMETRIC_WIN");
        } else if (isAsymmetricLawn()) {
            QuestProgress.complete("ASYMMETRIC_WIN");
        }

        if (offensiveFamilies.size() == 1
            && matchesConfiguredFamily("ONLY_FAMILY_WIN", offensiveFamilies)) {
            QuestProgress.complete("ONLY_FAMILY_WIN");
        }

        if (!matchesConfiguredFamily("EXCLUDE_FAMILY_WIN", usedFamilies)) {
            QuestProgress.complete("EXCLUDE_FAMILY_WIN");
        }

        if (game.isDay() && plantedMushroom) {
            QuestProgress.complete("DAY_LEVEL_WITH_MUSHROOMS");
        }

        if (sunProducerPlantsUsed > 0 && sunProducerPlantsUsed <= 3) {
            QuestProgress.complete("THREE_PRODUCER_WIN");
        }

        int configuredRow = configuredNumber("EMPTY_ROW_WIN", 1) - 1;
        int configuredColumn = configuredNumber("EMPTY_COLUMN_WIN", 1) - 1;
        int configuredCross = configuredNumber("EMPTY_CROSS_WIN", 1) - 1;

        if (wasRowNeverPlanted(configuredRow)) {
            QuestProgress.complete("EMPTY_ROW_WIN");
        }

        if (wasColumnNeverPlanted(configuredColumn)) {
            QuestProgress.complete("EMPTY_COLUMN_WIN");
        }

        if (wasRowNeverPlanted(configuredCross)
            && wasColumnNeverPlanted(configuredCross)) {
            QuestProgress.complete("EMPTY_CROSS_WIN");
        }
    }

    public void onGameLost() {
        if (!cheatUsed) {
            QuestProgress.resetActions(SINGLE_LEVEL_COUNTERS);
        }

        QuestProgress.setProgress("MAX_DIFFICULTY_WIN", 0f);
    }

    /** Rolls back all quest changes made during a game that used any cheat. */
    public void markCheatUsed() {
        if (cheatUsed) {
            return;
        }

        cheatUsed = true;
        QuestProgress.setSuppressed(true);
        QuestProgress.restoreProgress(startingQuestProgress);
    }

    private void countNewZombieDeaths() {
        for (Zombie zombie : observedZombies) {
            if (zombie == null
                || countedZombies.contains(zombie)
                || (!zombie.isDead() && zombie.getHp() > 0)) {
                continue;
            }

            countedZombies.add(zombie);

            if (matchesConfiguredValue("KILL_CHAPTER_ZOMBIE", chapterName())) {
                QuestProgress.add("KILL_CHAPTER_ZOMBIE", 1);
            }

            if (isInsideEarlyWaveWindow()) {
                QuestProgress.add("EARLY_WAVE_KILL", 1);
            }

            if (offensivePlantTypes.size() == 1
                && matchesConfiguredPlant("ONLY_SELECTED_PLANT_KILL")) {
                QuestProgress.add("ONLY_SELECTED_PLANT_KILL", 1);
            }

            if (offensivePlantTypes.size() == 1
                && offensivePlantTypes.contains(PlantType.CACTUS)) {
                QuestProgress.add("ONLY_CACTUS_KILL", 1);
            }

            if (zombie.getTileIndex() == 0 && isMowerGone(zombie.getLine())) {
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

    private boolean isMowerGone(int row) {
        if (game.getField() == null
            || row < 0
            || row >= game.getField().getMoaners().size()) {
            return false;
        }

        LawnMower mower = game.getField().getMoaners().get(row);
        return mower != null && mower.getState() != LawnMower.State.IDLE;
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
        PlantType[][] grid = new PlantType[ROW_COUNT][COLUMN_COUNT];

        for (Plant plant : game.getPlantsInField()) {
            if (plant == null || plant.getHp() <= 0) {
                continue;
            }

            int row = plant.getLine();
            int column = plant.getTileIndex();

            if (row >= 0 && row < ROW_COUNT
                && column >= 0 && column < COLUMN_COUNT) {
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

    private boolean wasRowNeverPlanted(int targetRow) {
        if (targetRow < 0 || targetRow >= ROW_COUNT) {
            return false;
        }

        return !plantedRows[targetRow];
    }

    private boolean wasColumnNeverPlanted(int targetColumn) {
        if (targetColumn < 0 || targetColumn >= COLUMN_COUNT) {
            return false;
        }

        return !plantedColumns[targetColumn];
    }

    private int configuredNumber(String action, int fallback) {
        Quest quest = QuestProgress.find(action);
        return quest == null || quest.getVariableName().isBlank()
            ? fallback
            : quest.getVariableNumber();
    }

    private boolean matchesConfiguredValue(String action, String actual) {
        Quest quest = QuestProgress.find(action);
        return quest != null
            && actual != null
            && quest.getVariableValue().equalsIgnoreCase(actual);
    }

    private boolean matchesConfiguredPlant(String action) {
        Quest quest = QuestProgress.find(action);

        if (quest == null || offensivePlantTypes.size() != 1) {
            return false;
        }

        PlantType used = offensivePlantTypes.iterator().next();
        return quest.getVariableValue().equalsIgnoreCase(used.name());
    }

    private boolean matchesConfiguredFamily(
        String action,
        Set<PlantCategory> families
    ) {
        Quest quest = QuestProgress.find(action);

        if (quest == null || families == null || families.isEmpty()) {
            return false;
        }

        for (PlantCategory family : families) {
            if (family != null
                && quest.getVariableValue().equalsIgnoreCase(family.name())) {
                return true;
            }
        }

        return false;
    }

    private String chapterName() {
        return chapter == null ? "" : chapter.name();
    }
}

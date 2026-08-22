package controllers.datacontroller;

import models.User;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;

/**
 * Owns the user's global adventure progress.
 *
 * <p>Level ids in levels.json are global and sequential. They must never be
 * reset when the chapter changes.</p>
 */
public final class LevelProgressService {

    private LevelProgressService() {
    }

    public static boolean normalizeUserProgress(User user) {
        if (user == null || !Data.hasLevelsLoaded()) {
            return false;
        }

        int highestLevelId = Data.getHighestLevelId();
        int normalizedPassed = Math.max(
            0,
            Math.min(user.getLevelsPassed(), highestLevelId)
        );

        int currentLevelId = normalizedPassed >= highestLevelId
            ? highestLevelId
            : normalizedPassed + 1;

        Level currentLevel = findNearestLevel(currentLevelId, highestLevelId);
        if (currentLevel == null) {
            return false;
        }

        boolean changed = false;

        if (user.getLevelsPassed() != normalizedPassed) {
            user.setLevelsPassed(normalizedPassed);
            changed = true;
        }

        if (user.getLevelId() != currentLevel.getId()) {
            user.setLevelId(currentLevel.getId());
            changed = true;
        }

        if (user.getChapter() != currentLevel.getChapters()) {
            user.setChapter(currentLevel.getChapters());
            changed = true;
        }

        return changed;
    }

    public static boolean isLevelUnlocked(User user, Level level) {
        if (user == null || level == null || !Data.hasLevelsLoaded()) {
            return false;
        }

        int highestUnlockedId = Math.min(
            Data.getHighestLevelId(),
            Math.max(1, user.getLevelsPassed() + 1)
        );

        return level.getId() > 0 && level.getId() <= highestUnlockedId;
    }

    public static Level getCurrentLevel(User user) {
        if (user == null || !Data.hasLevelsLoaded()) {
            return null;
        }

        normalizeUserProgress(user);
        return Data.getLevelById(user.getLevelId());
    }

    /**
     * Applies progress for a legitimately won adventure level.
     * Replaying an old level is safe and does not advance the campaign twice.
     */
    public static boolean completeLevel(
        User user,
        Chapters playedChapter,
        Level playedLevel
    ) {
        if (user == null
            || playedChapter == null
            || playedLevel == null
            || playedLevel.getChapters() != playedChapter
            || !Data.hasLevelsLoaded()) {
            return false;
        }

        Level registeredLevel = Data.getLevelById(playedLevel.getId());
        if (registeredLevel == null
            || registeredLevel.getChapters() != playedChapter) {
            return false;
        }

        normalizeUserProgress(user);

        int expectedLevelId = user.getLevelsPassed() + 1;
        if (playedLevel.getId() != expectedLevelId) {
            return false;
        }

        unlockLevelRewards(user, registeredLevel);
        user.setLevelsPassed(registeredLevel.getId());

        int highestLevelId = Data.getHighestLevelId();
        int nextLevelId = Math.min(registeredLevel.getId() + 1, highestLevelId);
        Level nextLevel = findNearestLevel(nextLevelId, highestLevelId);

        if (nextLevel != null) {
            user.setLevelId(nextLevel.getId());
            user.setChapter(nextLevel.getChapters());
        }

        return true;
    }

    private static void unlockLevelRewards(User user, Level level) {
        if (level.getUnlockingPlants() == null) {
            return;
        }

        for (PlantType plantType : level.getUnlockingPlants()) {
            if (plantType == null || user.getUnlockedPlants().contains(plantType)) {
                continue;
            }

            user.unlockPlant(plantType);
            user.getUnreadNews().add(
                "Congratulations, you've unlocked a new plant: "
                    + plantType.name()
            );
        }
    }

    private static Level findNearestLevel(
        int requestedLevelId,
        int highestLevelId
    ) {
        for (int id = requestedLevelId; id <= highestLevelId; id++) {
            Level level = Data.getLevelById(id);
            if (level != null) {
                return level;
            }
        }

        for (int id = requestedLevelId - 1; id >= 1; id--) {
            Level level = Data.getLevelById(id);
            if (level != null) {
                return level;
            }
        }

        return null;
    }
}

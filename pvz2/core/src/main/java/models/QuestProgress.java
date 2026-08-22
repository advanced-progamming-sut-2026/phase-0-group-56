package models;

import controllers.datacontroller.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central, persistence-aware entry point for gameplay quest events. */
public final class QuestProgress {
    private static boolean suppressed;

    private QuestProgress() {
    }

    public static boolean add(String action, int amount) {
        if (suppressed) {
            return false;
        }

        User user = currentUser(action);

        if (user == null || amount <= 0) {
            return false;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        boolean changed = false;

        for (Quest quest : user.getActiveQuests()) {
            if (quest != null && quest.addProgress(action, amount)) {
                changed = true;
            }
        }

        saveIfNeeded(catalogChanged || changed);
        return changed;
    }

    public static boolean complete(String action) {
        if (suppressed) {
            return false;
        }

        User user = currentUser(action);

        if (user == null) {
            return false;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        Quest quest = findByAction(user, action);
        boolean changed = false;

        if (quest != null && !quest.isDone()) {
            quest.complete();
            changed = true;
        }

        saveIfNeeded(catalogChanged || changed);
        return changed;
    }

    public static Quest find(String action) {
        User user = currentUser(action);

        if (user == null) {
            return null;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        saveIfNeeded(catalogChanged);
        return findByAction(user, action);
    }

    public static void setSuppressed(boolean value) {
        suppressed = value;
    }

    public static boolean isSuppressed() {
        return suppressed;
    }

    public static boolean setProgress(String action, float value) {
        User user = currentUser(action);

        if (user == null) {
            return false;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        Quest quest = findByAction(user, action);
        boolean changed = setQuestProgress(quest, value);

        saveIfNeeded(catalogChanged || changed);
        return changed;
    }

    /** Resets unfinished, single-level counters in one persisted operation. */
    public static boolean resetActions(String... actions) {
        User user = Data.getCurrentUser();

        if (user == null || actions == null) {
            return false;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        boolean changed = false;

        for (String action : actions) {
            Quest quest = findByAction(user, action);

            if (quest != null && !quest.isDone()) {
                changed |= setQuestProgress(quest, 0f);
            }
        }

        saveIfNeeded(catalogChanged || changed);
        return changed;
    }

    public static Map<String, Float> snapshotProgress() {
        User user = Data.getCurrentUser();
        Map<String, Float> snapshot = new LinkedHashMap<>();

        if (user == null) {
            return snapshot;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);

        for (Quest quest : user.getActiveQuests()) {
            if (quest != null && !quest.getActionType().isBlank()) {
                snapshot.put(quest.getActionType(), quest.getProgress());
            }
        }

        saveIfNeeded(catalogChanged);
        return snapshot;
    }

    /** Restores progress captured at game start when that game used a cheat. */
    public static boolean restoreProgress(Map<String, Float> snapshot) {
        User user = Data.getCurrentUser();

        if (user == null || snapshot == null) {
            return false;
        }

        boolean catalogChanged = QuestCatalog.ensureCurrentQuests(user);
        boolean changed = false;

        for (Map.Entry<String, Float> entry : snapshot.entrySet()) {
            Quest quest = findByAction(user, entry.getKey());

            if (quest != null && !quest.isRewardClaimed()) {
                changed |= setQuestProgress(quest, entry.getValue());
            }
        }

        saveIfNeeded(catalogChanged || changed);
        return changed;
    }

    private static User currentUser(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }

        return Data.getCurrentUser();
    }

    private static Quest findByAction(User user, String action) {
        if (user == null || action == null || action.isBlank()) {
            return null;
        }

        for (Quest quest : user.getActiveQuests()) {
            if (quest != null
                && quest.getActionType().equalsIgnoreCase(action.trim())) {
                return quest;
            }
        }

        return null;
    }

    private static boolean setQuestProgress(Quest quest, float value) {
        if (quest == null || quest.isRewardClaimed()) {
            return false;
        }

        float previous = quest.getProgress();
        boolean previousDone = quest.isDone();
        quest.setProgress(value);

        return previous != quest.getProgress()
            || previousDone != quest.isDone();
    }

    private static void saveIfNeeded(boolean changed) {
        if (changed) {
            Data.saveUser();
        }
    }
}

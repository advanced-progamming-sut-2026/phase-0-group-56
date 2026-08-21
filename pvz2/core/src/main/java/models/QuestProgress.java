package models;

import controllers.datacontroller.Data;

/**
 * Central entry point for every quest event produced by gameplay.
 */
public final class QuestProgress {

    private QuestProgress() {
    }

    public static boolean add(String action, int amount) {
        User user = Data.getCurrentUser();

        if (user == null || action == null || action.isBlank() || amount <= 0) {
            return false;
        }

        boolean changed = false;

        for (Quest quest : user.getActiveQuests()) {
            if (quest == null) {
                continue;
            }

            float previousProgress = quest.getProgress();
            boolean previousDone = quest.isDone();

            quest.updateQuestProgress(action, amount);

            if (quest.getProgress() != previousProgress || quest.isDone() != previousDone) {
                changed = true;
            }
        }

        if (changed) {
            Data.saveUser();
        }

        return changed;
    }

    public static boolean complete(String action) {
        User user = Data.getCurrentUser();

        if (user == null || action == null || action.isBlank()) {
            return false;
        }

        boolean changed = false;

        for (Quest quest : user.getActiveQuests()) {
            if (quest == null
                || quest.isDone()
                || !quest.getActionType().equalsIgnoreCase(action.trim())) {
                continue;
            }

            quest.complete();
            changed = true;
        }

        if (changed) {
            Data.saveUser();
        }

        return changed;
    }
}

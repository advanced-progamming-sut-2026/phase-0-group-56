package models;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.News;
import models.factory.builder.PlantType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Quest implements Serializable, QuestObserver {
    private static final long serialVersionUID = 3349285950453438029L;

    private String questName;
    private String description;
    private String category;
    private int priority;
    private String actionType;
    private float progress;
    private float target;
    private boolean isDone;
    private boolean rewardClaimed;
    private String rewardType;
    private int rewardAmount;

    public Quest(String questName, int priority, String actionType, float target,
                 String rewardType, int rewardAmount) {
        this(questName, "", "MAIN", priority, actionType, target, rewardType, rewardAmount);
    }

    public Quest(String questName, String description, String category, int priority,
                 String actionType, float target, String rewardType, int rewardAmount) {
        this.questName = questName;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.actionType = actionType;
        this.target = Math.max(1f, target);
        this.rewardType = rewardType;
        this.rewardAmount = Math.max(0, rewardAmount);
        this.progress = 0f;
        this.isDone = false;
        this.rewardClaimed = false;
    }

    @Override
    public void updateQuestProgress(String action, int amount) {
        if (isDone || action == null || actionType == null) {
            return;
        }
        if (actionType.equalsIgnoreCase(action)) {
            progress = Math.min(target, progress + Math.max(0, amount));
            if (progress >= target) {
                isDone = true;
                reward();
            }
        }
    }

    public void complete() {
        if (!isDone) {
            progress = target;
            isDone = true;
            reward();
        }
    }

    public void setProgress(float value) {
        if (isDone) {
            return;
        }
        progress = Math.max(0f, Math.min(target, value));
        if (progress >= target) {
            complete();
        }
    }

    public void reward() {
        if (rewardClaimed) {
            return;
        }
        User user = Data.getCurrentUser();
        if (user == null) {
            return;
        }

        if (equalsReward("GEM", "GEMS")) {
            user.addDiamonds(rewardAmount);
        } else if (equalsReward("COIN", "COINS")) {
            user.addCoins(rewardAmount);
        } else if (equalsReward("SEED", "SEEDS", "SEED_PACKET", "SEED_PACKETS")) {
            user.addRandomSeeds(rewardAmount);
        } else if (equalsReward("RANDOM_PLANT", "PLANT")) {
            unlockRandomPlant(user);
        }

        rewardClaimed = true;
        if (isDaily()) {
            user.incrementDailyQuestsCompleted();
        } else {
            user.incrementOtherQuestsCompleted();
        }

        String message = "Quest completed: " + getQuestName()
            + " | Reward: " + rewardAmount + " " + getRewardType();
        News.pushNewsToUser(user, message);
        Data.saveUser();
    }

    private boolean equalsReward(String... values) {
        if (rewardType == null) {
            return false;
        }
        for (String value : values) {
            if (rewardType.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void unlockRandomPlant(User user) {
        List<PlantType> locked = new ArrayList<>();
        Collections.addAll(locked, PlantType.values());
        locked.removeAll(user.getUnlockedPlants());
        locked.remove(PlantType.MARIGOLD);
        if (locked.isEmpty()) {
            user.addCoins(Math.max(500, rewardAmount));
            return;
        }
        PlantType selected = locked.get((int) (Math.random() * locked.size()));
        user.unlockPlant(selected);
    }

    public String getQuestName() {
        return questName == null ? "Quest" : questName;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getCategory() {
        return category == null ? "MAIN" : category;
    }

    public int getPriority() {
        return priority;
    }

    public String getPriorityName() {
        return switch (priority) {
            case 4 -> "CRITICAL";
            case 3 -> "HIGH";
            case 2 -> "MEDIUM";
            default -> "LOW";
        };
    }

    public String getActionType() {
        return actionType == null ? "" : actionType;
    }

    public float getProgress() {
        return progress;
    }

    public float getTarget() {
        return target <= 0f ? 1f : target;
    }

    public boolean isDone() {
        return isDone;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public String getRewardType() {
        return rewardType == null ? "" : rewardType;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public boolean isDaily() {
        return "DAILY".equalsIgnoreCase(getCategory());
    }
}

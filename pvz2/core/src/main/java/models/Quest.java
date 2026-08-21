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

    /*
     * Priority values:
     * 1 = LOW
     * 2 = MEDIUM
     * 3 = HIGH
     * 4 = CRITICAL
     */
    private int priority;

    private String actionType;
    private float progress;
    private float target;

    private boolean isDone;
    private boolean rewardClaimed;

    private String rewardType;
    private int rewardAmount;

    public Quest(
        String questName,
        int priority,
        String actionType,
        float target,
        String rewardType,
        int rewardAmount
    ) {
        this(
            questName,
            "",
            "MAIN",
            priority,
            actionType,
            target,
            rewardType,
            rewardAmount
        );
    }

    public Quest(
        String questName,
        String description,
        String category,
        int priority,
        String actionType,
        float target,
        String rewardType,
        int rewardAmount
    ) {
        this.questName =
            questName == null || questName.isBlank()
                ? "Quest"
                : questName.trim();

        this.description =
            description == null
                ? ""
                : description.trim();

        this.category =
            category == null || category.isBlank()
                ? "MAIN"
                : category.trim().toUpperCase();

        this.priority =
            Math.max(1, Math.min(4, priority));

        this.actionType =
            actionType == null
                ? ""
                : actionType.trim().toUpperCase();

        this.target = Math.max(1f, target);

        this.rewardType =
            rewardType == null
                ? ""
                : rewardType.trim().toUpperCase();

        this.rewardAmount = Math.max(0, rewardAmount);

        this.progress = 0f;
        this.isDone = false;
        this.rewardClaimed = false;
    }

    @Override
    public void updateQuestProgress(
        String action,
        int amount
    ) {
        addProgress(action, amount);
    }

    /*
     * Returns true only when the quest progress was actually changed.
     * This will be useful when gameplay events are connected to quests.
     */
    public boolean addProgress(
        String action,
        int amount
    ) {
        if (
            isDone
                || action == null
                || action.isBlank()
                || actionType == null
                || actionType.isBlank()
                || amount <= 0
        ) {
            return false;
        }

        if (!actionType.equalsIgnoreCase(action.trim())) {
            return false;
        }

        float previousProgress = progress;

        progress =
            Math.min(
                getTarget(),
                progress + amount
            );

        if (progress >= getTarget()) {
            complete();
        }

        return progress != previousProgress;
    }

    public void complete() {
        if (isDone) {
            return;
        }

        progress = getTarget();
        isDone = true;

        /*
         * Reward is intentionally not given here.
         * The player must claim it from Travel Log.
         */
    }

    public void setProgress(float value) {
        if (rewardClaimed) {
            return;
        }

        progress =
            Math.max(
                0f,
                Math.min(
                    getTarget(),
                    value
                )
            );

        isDone = progress >= getTarget();
    }

    public String claimReward() {
        return claimReward(
            Data.getCurrentUser()
        );
    }

    public String claimReward(User user) {
        if (!isDone) {
            return "Quest is not completed yet.";
        }

        if (rewardClaimed) {
            return "This quest reward has already been claimed.";
        }

        if (user == null) {
            return "Error: no user is currently logged in.";
        }

        boolean rewardApplied =
            applyReward(user);

        if (!rewardApplied) {
            return "Error: unknown quest reward type: " + getRewardType();
        }

        rewardClaimed = true;

        if (isDaily()) {
            user.incrementDailyQuestsCompleted();
        } else {
            user.incrementOtherQuestsCompleted();
        }

        String newsMessage =
            "Quest reward claimed: "
                + getQuestName()
                + " | "
                + getRewardAmount()
                + " "
                + getRewardType();

        News.pushNewsToUser(
            user,
            newsMessage
        );

        Data.saveUser();

        return "Reward claimed successfully: "
            + getRewardAmount()
            + " "
            + getRewardType()
            + ".";
    }

    /*
     * Kept for compatibility with any older code which calls reward().
     * New code should use claimReward().
     */
    public void reward() {
        claimReward();
    }

    private boolean applyReward(User user) {
        if (equalsReward("GEM", "GEMS")) {
            user.addDiamonds(rewardAmount);
            return true;
        }

        if (equalsReward("COIN", "COINS")) {
            user.addCoins(rewardAmount);
            return true;
        }

        if (
            equalsReward(
                "SEED",
                "SEEDS",
                "SEED_PACKET",
                "SEED_PACKETS"
            )
        ) {
            user.addRandomSeeds(rewardAmount);
            return true;
        }

        if (
            equalsReward(
                "RANDOM_PLANT",
                "PLANT"
            )
        ) {
            unlockRandomPlants(
                user,
                rewardAmount
            );

            return true;
        }

        return false;
    }

    private boolean equalsReward(String... values) {
        if (
            rewardType == null
                || rewardType.isBlank()
        ) {
            return false;
        }

        for (String value : values) {
            if (rewardType.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    private void unlockRandomPlants(
        User user,
        int amount
    ) {
        int plantsToUnlock =
            Math.max(0, amount);

        for (
            int i = 0;
            i < plantsToUnlock;
            i++
        ) {
            if (!unlockOneRandomPlant(user)) {
                /*
                 * If every plant has already been unlocked,
                 * give coins instead.
                 */
                user.addCoins(500);
            }
        }
    }

    private boolean unlockOneRandomPlant(User user) {
        List<PlantType> lockedPlants =
            new ArrayList<>();

        Collections.addAll(
            lockedPlants,
            PlantType.values()
        );

        lockedPlants.removeAll(
            user.getUnlockedPlants()
        );

        lockedPlants.remove(
            PlantType.MARIGOLD
        );

        if (lockedPlants.isEmpty()) {
            return false;
        }

        int randomIndex =
            (int) (
                Math.random()
                    * lockedPlants.size()
            );

        PlantType selectedPlant =
            lockedPlants.get(randomIndex);

        user.unlockPlant(selectedPlant);

        return true;
    }

    public String getQuestName() {
        return questName == null
            ? "Quest"
            : questName;
    }

    public String getDescription() {
        return description == null
            ? ""
            : description;
    }

    public String getCategory() {
        return category == null
            ? "MAIN"
            : category;
    }

    public int getPriority() {
        if (priority < 1 || priority > 4) {
            priority = 1;
        }

        return priority;
    }

    public String getPriorityName() {
        return switch (getPriority()) {
            case 4 -> "CRITICAL";
            case 3 -> "HIGH";
            case 2 -> "MEDIUM";
            default -> "LOW";
        };
    }

    public String getActionType() {
        return actionType == null
            ? ""
            : actionType;
    }

    public float getProgress() {
        return Math.max(
            0f,
            Math.min(
                getTarget(),
                progress
            )
        );
    }

    public float getTarget() {
        return target <= 0f
            ? 1f
            : target;
    }

    public float getProgressPercent() {
        return Math.min(
            100f,
            getProgress() / getTarget() * 100f
        );
    }

    public boolean isDone() {
        return isDone
            || getProgress() >= getTarget();
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public boolean isClaimable() {
        return isDone()
            && !isRewardClaimed();
    }

    public String getStatusText() {
        if (isRewardClaimed()) {
            return "CLAIMED";
        }

        if (isClaimable()) {
            return "READY TO CLAIM";
        }

        return "IN PROGRESS";
    }

    public String getRewardType() {
        return rewardType == null
            ? ""
            : rewardType;
    }

    public int getRewardAmount() {
        return Math.max(0, rewardAmount);
    }

    public boolean isDaily() {
        return "DAILY".equalsIgnoreCase(
            getCategory()
        );
    }
}

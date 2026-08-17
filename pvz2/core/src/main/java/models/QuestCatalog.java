package models;

import java.util.ArrayList;


public final class QuestCatalog {
    private QuestCatalog() {
    }

    public static ArrayList<Quest> createDefaultQuests() {
        ArrayList<Quest> quests = new ArrayList<>();

        quests.add(q("Daily Sun Catcher",
            "Collect 4000 sun during a day.", "DAILY", 2,
            "COLLECT_SUN", 4000, "COINS", 40));

        quests.add(q("Chapter Hunter",
            "Defeat 50 zombies from the selected chapter.", "MAIN", 3,
            "KILL_CHAPTER_ZOMBIE", 50, "SEED_PACKETS", 10));

        quests.add(q("Plant Pro",
            "Kill 10 zombies using only the selected plant.", "DAILY", 3,
            "ONLY_SELECTED_PLANT_KILL", 10, "RANDOM_PLANT", 1));

        quests.add(q("Only Cactus",
            "Kill 10 zombies using only Cactus.", "DAILY", 3,
            "ONLY_CACTUS_KILL", 10, "GEMS", 20));

        quests.add(q("Economic Gardener",
            "Win a level while losing no more than the configured number of plants.", "MAIN", 3,
            "ECONOMIC_WIN", 1, "SEED_PACKETS", 20));

        quests.add(q("Defense Master",
            "Finish a level with exactly zero sun remaining.", "EPIC", 4,
            "FINISH_WITH_ZERO_SUN", 1, "GEMS", 200));

        quests.add(q("Quick Hands",
            "Kill 10 zombies in the first 30 seconds after the first wave starts.", "MAIN", 2,
            "EARLY_WAVE_KILL", 10, "COINS", 500));

        quests.add(q("Professional Demolition",
            "Use 3 explosive plants in a single level.", "DAILY", 1,
            "USE_EXPLOSIVE_PLANT", 3, "COINS", 100));

        quests.add(q("Symmetry",
            "Win with a symmetric final lawn layout.", "DAILY", 3,
            "SYMMETRIC_WIN", 1, "COINS", 500));

        quests.add(q("Family Massacre",
            "Use only one configured plant family to kill zombies.", "DAILY", 2,
            "ONLY_FAMILY_WIN", 1, "COINS", 1000));

        quests.add(q("Bloom Under Limits",
            "Win without using any plant from the configured family.", "DAILY", 3,
            "EXCLUDE_FAMILY_WIN", 1, "GEMS", 100));

        quests.add(q("Night or Morning",
            "Finish a daytime level using mushroom plants.", "EPIC", 3,
            "DAY_LEVEL_WITH_MUSHROOMS", 1, "GEMS", 20));

        quests.add(q("Win Streak",
            "Win 5 levels in a row on maximum difficulty.", "DAILY", 2,
            "MAX_DIFFICULTY_WIN", 5, "COINS", 5000));

        quests.add(q("Almost Won",
            "Kill 10 zombies in column one of a row whose lawn mower is already gone.", "DAILY", 2,
            "DANGER_COLUMN_KILL", 10, "COINS", 300));

        quests.add(q("No OCD",
            "Win with a non-symmetric lawn, except the middle row.", "DAILY", 2,
            "ASYMMETRIC_WIN", 1, "COINS", 800));

        quests.add(q("Cloudy Day",
            "Win a level using only 3 sun-producing plants.", "DAILY", 3,
            "THREE_PRODUCER_WIN", 1, "GEMS", 10));

        quests.add(q("One Less Column",
            "Win while keeping the configured column empty.", "DAILY", 3,
            "EMPTY_COLUMN_WIN", 1, "GEMS", 10));

        quests.add(q("Undefended Row",
            "Win while keeping the configured row empty.", "DAILY", 3,
            "EMPTY_ROW_WIN", 1, "GEMS", 20));

        quests.add(q("Undefended Cross",
            "Win while keeping the configured row and column empty.", "DAILY", 3,
            "EMPTY_CROSS_WIN", 1, "GEMS", 25));

        quests.add(q("Mowing Time",
            "Kill at least 20 zombies with lawn mowers.", "EPIC", 2,
            "LAWNMOWER_KILL", 20, "GEMS", 20));

        return quests;
    }

    private static Quest q(String name, String description, String category, int priority,
                           String action, float target, String rewardType, int rewardAmount) {
        return new Quest(name, description, category, priority, action, target, rewardType, rewardAmount);
    }
}

package models;

import models.entity.PlantCategory;
import models.factory.builder.PlantType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Creates and migrates the official quest catalogue for one user. */
public final class QuestCatalog {
    public static final int CURRENT_VERSION = 2;

    private static final int[] SUN_TARGETS = {3000, 4000, 5000};
    private static final int[] MOWER_TARGETS = {10, 20, 30, 40, 50};

    private QuestCatalog() {
    }

    /** Compatibility entry point used by older code and tests. */
    public static ArrayList<Quest> createDefaultQuests() {
        return createDefaultQuests(null);
    }

    public static ArrayList<Quest> createDefaultQuests(User user) {
        int sunTarget = pick(SUN_TARGETS);
        int lostPlantLimit = randomInclusive(0, 5);
        int mowerTarget = pick(MOWER_TARGETS);
        int emptyColumn = randomInclusive(1, 9);
        int emptyRow = randomInclusive(1, 5);
        int emptyCross = randomInclusive(1, 5);

        String chapter = chooseChapter(user);
        String selectedPlant = chooseOffensivePlant(user);
        String onlyFamily = chooseFamily();
        String excludedFamily = chooseDifferentFamily(onlyFamily);

        ArrayList<Quest> quests = new ArrayList<>();

        quests.add(q(
            "Daily Sun Catcher",
            "Collect " + sunTarget + " sun during one day.",
            "DAILY", 2, "COLLECT_SUN", sunTarget,
            "COINS", sunTarget / 100
        ).configure("SUN_AMOUNT", "", sunTarget));

        quests.add(q(
            "Chapter Hunter",
            "Defeat 50 zombies from " + display(chapter) + ".",
            "MAIN", 3, "KILL_CHAPTER_ZOMBIE", 50,
            "SEED_PACKETS", 10
        ).configure("CHAPTER", chapter, 0));

        quests.add(q(
            "Plant Pro",
            "Kill 10 zombies while " + display(selectedPlant)
                + " is your only offensive plant.",
            "DAILY", 3, "ONLY_SELECTED_PLANT_KILL", 10,
            "RANDOM_PLANT", 1
        ).configure("PLANT", selectedPlant, 0));

        quests.add(q(
            "Only Cactus",
            "Kill 10 zombies while Cactus is your only offensive plant.",
            "DAILY", 3, "ONLY_CACTUS_KILL", 10,
            "GEMS", 20
        ).configure("PLANT", PlantType.CACTUS.name(), 0));

        quests.add(q(
            "Economic Gardener",
            "Win a level without losing more than " + lostPlantLimit + " plants.",
            "MAIN", 3, "ECONOMIC_WIN", 1,
            "SEED_PACKETS", 20 - lostPlantLimit
        ).configure("N", "", lostPlantLimit));

        quests.add(q(
            "Defense Master",
            "Finish a level with exactly zero sun remaining.",
            "EPIC", 4, "FINISH_WITH_ZERO_SUN", 1,
            "GEMS", 200
        ));

        quests.add(q(
            "Quick Hands",
            "Kill 10 zombies within 30 seconds after the first wave starts.",
            "MAIN", 2, "EARLY_WAVE_KILL", 10,
            "COINS", 500
        ));

        quests.add(q(
            "Professional Demolition",
            "Use 3 explosive plants in a single level.",
            "DAILY", 1, "USE_EXPLOSIVE_PLANT", 3,
            "COINS", 100
        ));

        quests.add(q(
            "Symmetry",
            "Win with a symmetric final lawn layout.",
            "DAILY", 3, "SYMMETRIC_WIN", 1,
            "COINS", 500
        ));

        quests.add(q(
            "Family Massacre",
            "Use only offensive plants from the " + display(onlyFamily)
                + " family to kill zombies and win.",
            "DAILY", 2, "ONLY_FAMILY_WIN", 1,
            "COINS", 1000
        ).configure("FAMILY_TYPE", onlyFamily, 0));

        quests.add(q(
            "Bloom Under Limits",
            "Win without planting any member of the " + display(excludedFamily)
                + " family.",
            "DAILY", 3, "EXCLUDE_FAMILY_WIN", 1,
            "GEMS", 100
        ).configure("FAMILY_TYPE", excludedFamily, 0));

        quests.add(q(
            "Night or Morning",
            "Finish a daytime level after using mushroom plants.",
            "EPIC", 3, "DAY_LEVEL_WITH_MUSHROOMS", 1,
            "GEMS", 20
        ));

        quests.add(q(
            "Win Streak",
            "Win 5 consecutive levels on maximum difficulty.",
            "DAILY", 2, "MAX_DIFFICULTY_WIN", 5,
            "COINS", 5000
        ));

        quests.add(q(
            "Almost Won",
            "Kill 10 zombies in column one of a row whose lawn mower is gone.",
            "DAILY", 2, "DANGER_COLUMN_KILL", 10,
            "COINS", 300
        ));

        quests.add(q(
            "No OCD",
            "Win with no mirrored row pair except the middle row.",
            "DAILY", 2, "ASYMMETRIC_WIN", 1,
            "COINS", 800
        ));

        quests.add(q(
            "Cloudy Day",
            "Win while using at most 3 sun-producing plants.",
            "DAILY", 3, "THREE_PRODUCER_WIN", 1,
            "GEMS", 10
        ));

        quests.add(q(
            "One Less Column",
            "Win without planting in column " + emptyColumn + ".",
            "DAILY", 3, "EMPTY_COLUMN_WIN", 1,
            "GEMS", 10
        ).configure("COLUMN", "", emptyColumn));

        quests.add(q(
            "Undefended Row",
            "Win without planting in row " + emptyRow + ".",
            "DAILY", 3, "EMPTY_ROW_WIN", 1,
            "GEMS", 20
        ).configure("ROW", "", emptyRow));

        quests.add(q(
            "Undefended Cross",
            "Win while row and column " + emptyCross + " are both empty.",
            "DAILY", 3, "EMPTY_CROSS_WIN", 1,
            "GEMS", 25
        ).configure("CROSS", "", emptyCross));

        quests.add(q(
            "Mowing Time",
            "Kill at least " + mowerTarget + " zombies with lawn mowers.",
            "EPIC", 2, "LAWNMOWER_KILL", mowerTarget,
            "GEMS", mowerTarget
        ).configure("N", "", mowerTarget));

        return quests;
    }

    /**
     * Migrates old catalogues and replaces only DAILY quests after midnight.
     * MAIN and EPIC progress/claim state are preserved by action identifier.
     */
    public static boolean ensureCurrentQuests(User user) {
        if (user == null) {
            return false;
        }

        String today = LocalDate.now().toString();
        ArrayList<Quest> existing = user.getStoredActiveQuests();
        boolean dailyExpired = !today.equals(user.getQuestDailyDate());
        boolean incomplete = !hasCompleteCatalog(existing);
        boolean versionChanged =
            user.getQuestCatalogVersion() != CURRENT_VERSION;

        if (!dailyExpired && !incomplete && !versionChanged) {
            return false;
        }

        Map<String, Quest> previousByAction = new HashMap<>();

        if (existing != null) {
            for (Quest quest : existing) {
                if (quest != null && !quest.getActionType().isBlank()) {
                    previousByAction.putIfAbsent(
                        quest.getActionType().toUpperCase(),
                        quest
                    );
                }
            }
        }

        ArrayList<Quest> refreshed = createDefaultQuests(user);

        for (int i = 0; i < refreshed.size(); i++) {
            Quest generated = refreshed.get(i);
            Quest previous = previousByAction.get(generated.getActionType());

            if (previous == null) {
                continue;
            }

            if (generated.isDaily()) {
                if (!dailyExpired && !versionChanged && !incomplete) {
                    refreshed.set(i, previous);
                }
            } else if (!versionChanged && !incomplete) {
                refreshed.set(i, previous);
            } else if (sameProgressDefinition(previous, generated)) {
                generated.copyStateFrom(previous);
            }
        }

        user.setStoredActiveQuests(refreshed);
        user.setQuestDailyDate(today);
        user.setQuestCatalogVersion(CURRENT_VERSION);
        return true;
    }

    private static boolean hasCompleteCatalog(List<Quest> quests) {
        if (quests == null || quests.size() != 20) {
            return false;
        }

        Map<String, Boolean> actions = new HashMap<>();

        for (Quest quest : quests) {
            if (quest == null || quest.getActionType().isBlank()) {
                return false;
            }

            if (actions.put(quest.getActionType(), Boolean.TRUE) != null) {
                return false;
            }
        }

        return actions.containsKey("EXCLUDE_FAMILY_WIN")
            && actions.containsKey("LAWNMOWER_KILL")
            && actions.containsKey("COLLECT_SUN");
    }

    /**
     * Progress is meaningful only for the same quest definition.  In
     * particular, generated chapter/plant/family/number variables must not
     * carry kills from an older assignment into a new assignment during
     * migration or after a catalogue version bump.
     */
    private static boolean sameProgressDefinition(Quest previous, Quest current) {
        if (previous == null || current == null
            || !previous.getActionType().equalsIgnoreCase(current.getActionType())) {
            return false;
        }

        return Float.compare(previous.getTarget(), current.getTarget()) == 0
            && previous.getCategory().equalsIgnoreCase(current.getCategory())
            && previous.getVariableName().equalsIgnoreCase(current.getVariableName())
            && previous.getVariableValue().equalsIgnoreCase(current.getVariableValue())
            && previous.getVariableNumber() == current.getVariableNumber()
            && previous.getRewardType().equalsIgnoreCase(current.getRewardType())
            && previous.getRewardAmount() == current.getRewardAmount();
    }

    private static Quest q(
        String name,
        String description,
        String category,
        int priority,
        String action,
        float target,
        String rewardType,
        int rewardAmount
    ) {
        return new Quest(
            name, description, category, priority,
            action, target, rewardType, rewardAmount
        );
    }

    private static String chooseChapter(User user) {
        return user == null || user.getChapter() == null
            ? "ANCIENT_EGYPT"
            : user.getChapter().name();
    }

    private static String chooseOffensivePlant(User user) {
        ArrayList<PlantType> candidates = new ArrayList<>();

        if (user != null) {
            for (PlantType type : user.getUnlockedPlants()) {
                if (isOffensive(type) && !candidates.contains(type)) {
                    candidates.add(type);
                }
            }
        }

        if (candidates.isEmpty()) {
            candidates.add(PlantType.PEASHOOTER);
        }

        return candidates.get(
            ThreadLocalRandom.current().nextInt(candidates.size())
        ).name();
    }

    private static boolean isOffensive(PlantType type) {
        if (type == null) {
            return false;
        }

        return switch (type) {
            case SUNFLOWER, TWIN_SUNFLOWER, SUN_SHROOM, PRIMAL_SUNFLOWER,
                 GOLD_BLOOM, WALL_NUT, TALL_NUT, ENDURIAN, GARLIC,
                 SWEET_POTATO, PUMPKIN, SUN_BEAN, TORCHWOOD, HYPNO_SHROOM,
                 IMITATER, LILY_PAD, MAGNET_SHROOM, ENLIGHTEN_MINT,
                 APPEASE_MINT, ARMA_MINT, BOMBARD_MINT, ENFORCE_MINT,
                 REINFORCE_MINT, ENCHANT_MINT, PIERCE_MINT, CATTAIL_MINT,
                 MARIGOLD -> false;
            default -> true;
        };
    }

    private static String chooseFamily() {
        PlantCategory[] families = PlantCategory.values();
        return families[
            ThreadLocalRandom.current().nextInt(families.length)
            ].name();
    }

    private static String chooseDifferentFamily(String excluded) {
        PlantCategory[] families = PlantCategory.values();

        if (families.length <= 1) {
            return chooseFamily();
        }

        String selected;

        do {
            selected = chooseFamily();
        } while (selected.equalsIgnoreCase(excluded));

        return selected;
    }

    private static int randomInclusive(int minimum, int maximum) {
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }

    private static int pick(int[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private static String display(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        String lower = value.toLowerCase().replace('_', ' ');
        StringBuilder result = new StringBuilder(lower.length());
        boolean upperNext = true;

        for (int i = 0; i < lower.length(); i++) {
            char current = lower.charAt(i);
            result.append(upperNext ? Character.toUpperCase(current) : current);
            upperNext = current == ' ';
        }

        return result.toString();
    }
}

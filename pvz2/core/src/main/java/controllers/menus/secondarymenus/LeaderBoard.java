package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import network.LeaderboardEntry;
import network.NetworkClient;
import network.NetworkService;
import view.HomeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LeaderBoard implements Menu {
    private static final int LEVELS_PER_CHAPTER = 4;
    private static final int TOTAL_ADVENTURE_LEVELS = 16;

    public enum SortCriterion {
        USERNAME,
        PROGRESS,
        MINIGAMES,
        DAILY_QUESTS,
        OTHER_QUESTS,
        SCORE
    }

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from this menu.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Home Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- LeaderBoard Menu ---";
    }

    public String showLeaderBoard() {
        List<User> users = getSortedUsers(
            SortCriterion.SCORE,
            true
        );

        if (users.isEmpty()) {
            return "No users available.";
        }

        return formatTable(users);
    }

    /** Returns the authoritative server leaderboard when a network session exists. */
    public List<LeaderboardEntry> getNetworkLeaderboard() {
        NetworkClient client = NetworkService.getClient();
        if (client == null || !client.isConnected()) {
            return List.of();
        }
        try {
            return NetworkClient.leaderboardFrom(client.requestLeaderboard());
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public String sortLeaderBoard(String criteria) {
        return sortLeaderBoard(criteria, true);
    }

    public String sortLeaderBoard(
        String criteria,
        boolean descending
    ) {
        SortCriterion criterion = parseCriterion(criteria);

        if (criterion == null) {
            return "Error: invalid sort criterion. Use username, progress, "
                + "minigames, daily, other, or score.";
        }

        List<User> users = getSortedUsers(
            criterion,
            descending
        );

        if (users.isEmpty()) {
            return "No users available.";
        }

        return formatTable(users);
    }

    public List<User> getSortedUsers(
        SortCriterion criterion,
        boolean descending
    ) {
        ArrayList<User> source = Data.getAllUsers();
        ArrayList<User> result = new ArrayList<>();

        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        for (User user : source) {
            if (user != null) {
                result.add(user);
            }
        }

        SortCriterion safeCriterion = criterion == null
            ? SortCriterion.SCORE
            : criterion;

        Comparator<User> comparator =
            comparatorFor(safeCriterion);

        if (descending) {
            comparator = comparator.reversed();
        }

        /*
         * Numeric ties are always resolved by username so the order is stable
         * and does not depend on the serialized list's insertion history.
         */
        if (safeCriterion != SortCriterion.USERNAME) {
            comparator = comparator.thenComparing(
                this::safeUsername,
                String.CASE_INSENSITIVE_ORDER
            );
        }

        result.sort(comparator);

        return Collections.unmodifiableList(result);
    }

    public String getProgressText(User user) {
        if (user == null || user.getLevelsPassed() <= 0) {
            return "Not started";
        }

        int completedLevel = Math.min(
            user.getLevelsPassed(),
            TOTAL_ADVENTURE_LEVELS
        );

        int chapterIndex =
            (completedLevel - 1)
                / LEVELS_PER_CHAPTER;

        int chapterLevel =
            (completedLevel - 1)
                % LEVELS_PER_CHAPTER
                + 1;

        String chapterName = switch (chapterIndex) {
            case 0 -> "Ancient Egypt";
            case 1 -> "Frozen Caves";
            case 2 -> "Big Wave Beach";
            default -> "Dark Ages";
        };

        return chapterName
            + " - Level "
            + chapterLevel;
    }

    private Comparator<User> comparatorFor(
        SortCriterion criterion
    ) {
        return switch (criterion) {
            case USERNAME -> Comparator.comparing(
                this::safeUsername,
                String.CASE_INSENSITIVE_ORDER
            );

            case PROGRESS -> Comparator.comparingInt(
                User::getLevelsPassed
            );

            case MINIGAMES -> Comparator.comparingInt(
                User::getMinigamesWon
            );

            case DAILY_QUESTS -> Comparator.comparingInt(
                User::getDailyQuestsCompleted
            );

            case OTHER_QUESTS -> Comparator.comparingInt(
                User::getOtherQuestsCompleted
            );

            case SCORE -> Comparator.comparingInt(
                User::getHighestScore
            );
        };
    }

    private SortCriterion parseCriterion(String criteria) {
        if (criteria == null || criteria.isBlank()) {
            return null;
        }

        String normalized = criteria
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-')
            .replace(' ', '-');

        return switch (normalized) {
            case "username", "user", "name" ->
                SortCriterion.USERNAME;

            case "progress", "level", "levels" ->
                SortCriterion.PROGRESS;

            case "minigame", "minigames" ->
                SortCriterion.MINIGAMES;

            case "daily", "daily-quest", "daily-quests" ->
                SortCriterion.DAILY_QUESTS;

            case "other", "other-quest", "other-quests",
                 "non-daily", "nondaily" ->
                SortCriterion.OTHER_QUESTS;

            case "score", "meowpoint", "meow-point" ->
                SortCriterion.SCORE;

            default -> null;
        };
    }

    private String formatTable(List<User> users) {
        StringBuilder result = new StringBuilder();

        result.append(
            String.format(
                "%-15s | %-25s | %-9s | %-7s | %-7s | %-9s%n",
                "Username",
                "Progress",
                "Minigames",
                "Daily Q",
                "Other Q",
                "MeowPoint"
            )
        );

        result.append(
            "----------------------------------------------------------------------------------------\n"
        );

        for (User user : users) {
            result.append(
                String.format(
                    "%-15s | %-25s | %-9d | %-7d | %-7d | %-9d%n",
                    safeUsername(user),
                    getProgressText(user),
                    user.getMinigamesWon(),
                    user.getDailyQuestsCompleted(),
                    user.getOtherQuestsCompleted(),
                    user.getHighestScore()
                )
            );
        }

        return result.toString().trim();
    }

    private String safeUsername(User user) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return "Unknown";
        }

        return user.getName().trim();
    }
}

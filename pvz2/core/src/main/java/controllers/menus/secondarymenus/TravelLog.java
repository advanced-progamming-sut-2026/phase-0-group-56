package controllers.menus.secondarymenus;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.Quest;
import models.QuestCatalog;
import models.User;
import view.HomeView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TravelLog implements Menu {

    private String currentPage = "ALL";

    @Override
    public String ChangeMenu(String menuName) {
        return "Invalid menu transition from Travel Log.";
    }

    @Override
    public String exitMenu() {
        App.setScreen(new HomeView());
        return "Returned to Main Menu.";
    }

    @Override
    public String ShowCurrentMenu() {
        return "--- Travel Log / Quests Menu ---";
    }

    public String changePage(String pageName) {
        if (
            pageName == null
                || pageName.isBlank()
        ) {
            return "Error: page name cannot be empty.";
        }

        String normalized =
            pageName.trim().toUpperCase();

        if (
            normalized.equals("ALL")
                || normalized.equals("MAIN")
                || normalized.equals("DAILY")
                || normalized.equals("EPIC")
                || normalized.equals("MINIGAMES")
        ) {
            currentPage = normalized;

            return "Switched to Travel Log page: "
                + currentPage
                + ".";
        }

        return "Error: invalid page. Available pages: "
            + "ALL, MAIN, DAILY, EPIC, MINIGAMES.";
    }

    public List<Quest> getSortedQuests(
        String category
    ) {
        User user =
            Data.getCurrentUser();

        List<Quest> result =
            new ArrayList<>();

        if (user == null) {
            return result;
        }

        ensureQuestCatalog(user);

        String filter =
            category == null
                ? "ALL"
                : category.trim().toUpperCase();

        for (
            Quest quest :
            user.getActiveQuests()
        ) {
            if (quest == null) {
                continue;
            }

            if (
                !"ALL".equals(filter)
                    && !filter.equalsIgnoreCase(
                    quest.getCategory()
                )
            ) {
                continue;
            }

            result.add(quest);
        }

        /*
         * Higher-priority quests appear first.
         * Quests with equal priority are sorted by name.
         */
        result.sort(
            Comparator
                .comparingInt(
                    Quest::getPriority
                )
                .reversed()
                .thenComparing(
                    quest ->
                        quest.isClaimable()
                            ? 0
                            : quest.isRewardClaimed()
                            ? 2
                            : 1
                )
                .thenComparing(
                    Quest::getQuestName,
                    String.CASE_INSENSITIVE_ORDER
                )
        );

        return result;
    }

    public String claimReward(Quest quest) {
        User user =
            Data.getCurrentUser();

        if (user == null) {
            return "Error: no user is currently logged in.";
        }

        if (quest == null) {
            return "Error: quest not found.";
        }

        ensureQuestCatalog(user);

        if (!user.getActiveQuests().contains(quest)) {
            return "Error: this quest does not belong to the current user.";
        }

        return quest.claimReward(user);
    }

    public String claimReward(
        String questName
    ) {
        if (
            questName == null
                || questName.isBlank()
        ) {
            return "Error: quest name cannot be empty.";
        }

        User user =
            Data.getCurrentUser();

        if (user == null) {
            return "Error: no user is currently logged in.";
        }

        ensureQuestCatalog(user);

        for (
            Quest quest :
            user.getActiveQuests()
        ) {
            if (
                quest != null
                    && quest.getQuestName()
                    .equalsIgnoreCase(
                        questName.trim()
                    )
            ) {
                return quest.claimReward(user);
            }
        }

        return "Error: quest not found.";
    }

    public String showQuests() {
        StringBuilder output =
            new StringBuilder(
                "--- Travel Log: "
                    + currentPage
                    + " ---\n"
            );

        List<Quest> quests =
            getSortedQuests(currentPage);

        if (quests.isEmpty()) {
            return output
                .append(
                    "No quests are available in this category."
                )
                .toString();
        }

        for (Quest quest : quests) {
            output
                .append("- [")
                .append(quest.getStatusText())
                .append("] ")
                .append(quest.getQuestName())
                .append(" | Priority: ")
                .append(quest.getPriorityName())
                .append(" | Progress: ")
                .append((int) quest.getProgress())
                .append("/")
                .append((int) quest.getTarget())
                .append(" | Reward: ")
                .append(quest.getRewardAmount())
                .append(" ")
                .append(quest.getRewardType())
                .append("\n");
        }

        return output.toString().trim();
    }

    private void ensureQuestCatalog(User user) {
        if (QuestCatalog.ensureCurrentQuests(user)) {
            Data.saveUser();
        }
    }
}

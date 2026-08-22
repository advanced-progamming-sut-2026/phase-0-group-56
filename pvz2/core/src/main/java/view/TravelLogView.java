package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.TravelLog;
import models.App;
import models.Quest;
import models.User;
import view.MiniGamesView;

import java.util.List;

public class TravelLogView extends View {

    private String selectedCategory = "ALL";

    public TravelLogView() {
        menu = new TravelLog();
        App.setCurrentmenu(menu);
    }

    @Override
    protected String getScreenTitle() {
        return "Travel Log / Quests";
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        User user =
            Data.getCurrentUser();

        if (user == null) {
            table.add(
                mediumTitle(
                    "PLEASE LOG IN"
                )
            );

            return;
        }

        /* Applies daily reset/migration before the summary is calculated. */
        ((TravelLog) menu).getSortedQuests("ALL");

        table.add(
                menuSectionHeader(
                    "hud_quests",
                    "TRAVEL LOG",
                    "Track quests, rewards, priorities, and minigame access."
                )
            )
            .width(800f)
            .padTop(4f)
            .padBottom(12f)
            .row();

        buildQuestSummary(
            table,
            user
        );

        buildTabs(table);

        if (
            "MINIGAMES".equals(
                selectedCategory
            )
        ) {
            buildMinigameSection(table);
            return;
        }

        buildQuestList(table);
    }

    private void buildQuestSummary(
        Table table,
        User user
    ) {
        int inProgress = 0;
        int claimable = 0;
        int claimed = 0;

        for (
            Quest quest :
            user.getActiveQuests()
        ) {
            if (quest == null) {
                continue;
            }

            if (quest.isRewardClaimed()) {
                claimed++;
            } else if (quest.isClaimable()) {
                claimable++;
            } else {
                inProgress++;
            }
        }

        Table summaryPanel =
            pvzInnerPanel();

        Label summary =
            secondaryLabel(
                "IN PROGRESS: "
                    + inProgress
                    + "    |    READY TO CLAIM: "
                    + claimable
                    + "    |    CLAIMED: "
                    + claimed
            );

        summary.setAlignment(Align.center);

        summaryPanel.add(summary)
            .width(720f)
            .center();

        table.add(summaryPanel)
            .width(800f)
            .padBottom(12f)
            .row();
    }

    private void buildTabs(Table table) {
        Table tabPanel =
            pvzInnerPanel();

        String[] categories = {
            "ALL",
            "MAIN",
            "DAILY",
            "EPIC",
            "MINIGAMES"
        };

        for (String category : categories) {
            TextButton button;

            if (
                category.equals(
                    selectedCategory
                )
            ) {
                button =
                    purpleButton(
                        category,
                        null
                    );
            } else {
                button =
                    brownButton(
                        category,
                        () -> selectCategory(category)
                    );
            }

            tabPanel.add(button)
                .width(170f)
                .height(48f)
                .pad(4f);
        }

        table.add(tabPanel)
            .padBottom(16f)
            .row();
    }

    private void selectCategory(
        String category
    ) {
        selectedCategory = category;

        ((TravelLog) menu).changePage(
            category
        );

        rebuild();
    }

    private void buildQuestList(
        Table table
    ) {
        List<Quest> quests =
            ((TravelLog) menu)
                .getSortedQuests(
                    selectedCategory
                );

        if (quests.isEmpty()) {
            Table emptyPanel =
                pvzPanel();

            Label empty =
                mediumTitle(
                    "NO QUESTS IN THIS CATEGORY"
                );

            empty.setAlignment(Align.center);

            emptyPanel.add(empty)
                .width(600f)
                .pad(28f);

            table.add(emptyPanel)
                .width(700f)
                .padTop(30f);

            return;
        }

        for (Quest quest : quests) {
            table.add(
                    buildQuestCard(quest)
                )
                .width(940f)
                .pad(7f)
                .row();
        }
    }

    private Table buildQuestCard(
        Quest quest
    ) {
        Table card = pvzPanel();

        String titleText =
            quest.getQuestName()
                + "  ["
                + quest.getPriorityName()
                + "]";

        Label questName =
            mediumTitle(titleText);

        card.add(questName)
            .left()
            .growX()
            .padBottom(6f)
            .row();

        Label status =
            secondaryLabel(
                "STATUS: "
                    + quest.getStatusText()
                    + "    |    CATEGORY: "
                    + quest.getCategory()
            );

        card.add(status)
            .left()
            .padBottom(8f)
            .row();

        Label description =
            wrappedLabel(
                quest.getDescription(),
                820f
            );

        card.add(description)
            .width(820f)
            .left()
            .padBottom(10f)
            .row();

        float target =
            Math.max(
                1f,
                quest.getTarget()
            );

        ProgressBar progress =
            createProgressBar(
                target,
                quest.isDone()
            );

        progress.setValue(
            Math.min(
                target,
                quest.getProgress()
            )
        );

        card.add(progress)
            .width(700f)
            .height(24f)
            .left()
            .padBottom(9f)
            .row();

        Table footer = new Table();

        Label progressLabel =
            secondaryLabel(
                "PROGRESS: "
                    + (int) quest.getProgress()
                    + " / "
                    + (int) quest.getTarget()
                    + "  ("
                    + (int) quest.getProgressPercent()
                    + "%)"
            );

        Label rewardLabel =
            secondaryLabel(
                "REWARD: "
                    + quest.getRewardAmount()
                    + " "
                    + quest.getRewardType()
            );

        footer.add(progressLabel)
            .left();

        footer.add()
            .expandX();

        footer.add(rewardLabel)
            .right();

        card.add(footer)
            .width(820f)
            .padBottom(8f)
            .row();

        if (quest.isClaimable()) {
            TextButton claimButton =
                greenButton(
                    "CLAIM REWARD",
                    () -> claimReward(quest)
                );

            card.add(claimButton)
                .width(260f)
                .height(50f)
                .right();
        } else if (quest.isRewardClaimed()) {
            Label claimedLabel =
                secondaryLabel(
                    "REWARD CLAIMED"
                );

            claimedLabel.setAlignment(
                Align.center
            );

            card.add(claimedLabel)
                .width(260f)
                .height(40f)
                .right();
        }

        return card;
    }

    private ProgressBar createProgressBar(
        float target,
        boolean completed
    ) {
        String style =
            completed
                ? "xp_green"
                : "xp_teal";

        try {
            return new ProgressBar(
                0f,
                target,
                1f,
                false,
                skin,
                style
            );
        } catch (Exception exception) {
            return new ProgressBar(
                0f,
                target,
                1f,
                false,
                skin
            );
        }
    }

    private void claimReward(
        Quest quest
    ) {
        String result =
            ((TravelLog) menu)
                .claimReward(quest);

        rebuild();
        refreshResourceLabels();
        showMessage(result);
    }

    private void buildMinigameSection(
        Table table
    ) {
        User user = Data.getCurrentUser();

        if (user == null) {
            return;
        }

        Table descriptionPanel =
            pvzInnerPanel();

        Label description =
            wrappedLabel(
                "Your unlocked minigame progress is shown here. "
                    + "Use the button below to open the existing MiniGames menu.",
                720f
            );

        description.setAlignment(Align.center);

        descriptionPanel.add(description)
            .width(720f);

        descriptionPanel.row();

        TextButton openGames = greenButton(
            "OPEN MINI-GAMES",
            () -> App.setScreen(new MiniGamesView())
        );

        descriptionPanel.add(openGames)
            .width(260f)
            .height(50f)
            .padTop(14f);

        table.add(descriptionPanel)
            .width(790f)
            .padBottom(20f)
            .row();

        Table minigames =
            pvzPanel();

        Label title =
            mediumTitle(
                "MINIGAMES"
            );

        title.setAlignment(Align.center);

        minigames.add(title)
            .colspan(4)
            .padBottom(20f)
            .row();

        minigames.add(
                buildMinigameCard(
                    "VASE BREAKER",
                    user.getVaseBreaker()
                )
            )
            .width(205f)
            .pad(10f);

        minigames.add(
                buildMinigameCard(
                    "WALL-NUT BOWLING",
                    user.getWallNutBowling()
                )
            )
            .width(205f)
            .pad(10f);

        minigames.add(
                buildMinigameCard(
                    "I, ZOMBIE",
                    user.getIZombie()
                )
            )
            .width(205f)
            .pad(10f);

        minigames.add(
                buildMinigameCard(
                    "BEGHOULED",
                    1
                )
            )
            .width(205f)
            .pad(10f);

        table.add(minigames)
            .width(900f);
    }

    private Table buildMinigameCard(
        String name,
        int unlockedLevel
    ) {
        Table card = pvzInnerPanel();

        Label nameLabel = mediumTitle(name);
        nameLabel.setAlignment(Align.center);

        Label levelLabel = secondaryLabel(
            "UNLOCKED THROUGH LEVEL "
                + Math.max(1, unlockedLevel)
        );
        levelLabel.setAlignment(Align.center);

        card.add(nameLabel)
            .width(185f)
            .padBottom(10f)
            .row();

        card.add(levelLabel)
            .width(185f);

        return card;
    }

    private void rebuild() {
        if (content == null) {
            return;
        }

        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }
}

package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

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

        summaryPanel.pad(8f, 12f, 8f, 12f);
        summaryPanel.defaults().width(235f).height(48f).pad(4f);
        summaryPanel.add(summaryCell("quest", "IN PROGRESS", inProgress));
        summaryPanel.add(summaryCell("claim", "READY TO CLAIM", claimable));
        summaryPanel.add(summaryCell("check", "CLAIMED", claimed));

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
            boolean selected = category.equals(selectedCategory);
            Stack button = assetTextButton(
                selected ? "purple_button" : "brown_button",
                selected ? "purple_button_down" : "brown_button_down",
                category,
                selected ? null : () -> selectCategory(category)
            );

            tabPanel.add(button)
                .width(170f)
                .height(48f)
                .pad(4f);
        }

        table.add(tabPanel)
            .padBottom(16f)
            .row();
    }

    private Table summaryCell(String iconKey, String label, int value) {
        Table cell = new Table();
        Image icon = MenuVisualAssets.image(iconKey);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            cell.add(icon).size(28f).padRight(5f);
        }
        Table text = new Table();
        text.add(secondaryLabel(label)).left().row();
        text.add(mediumTitle(String.valueOf(value))).left();
        cell.add(text).left();
        return cell;
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
        Table card = pvzInnerPanel();
        card.pad(14f, 18f, 14f, 18f);

        String titleText =
            quest.getQuestName()
                + "  ["
                + quest.getPriorityName()
                + "]";

        Table questHeader = new Table();
        Image questIcon = MenuVisualAssets.image(questIconKey(quest.getCategory()));
        if (questIcon != null) {
            questIcon.setScaling(Scaling.fit);
            questHeader.add(questIcon)
                .size(42f)
                .padRight(8f);
        }
        Label questName = mediumTitle(titleText);
        questHeader.add(questName)
            .left()
            .growX();

        card.add(questHeader)
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

        Label description = secondaryLabel(quest.getDescription());
        description.setWrap(true);

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

        Table rewardBadge = new Table();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable rewardBackground =
            MenuVisualAssets.drawable(rewardFrameKey(quest.getRewardType()));
        if (rewardBackground != null) {
            rewardBadge.setBackground(rewardBackground);
        }
        Image rewardIcon = MenuVisualAssets.image(rewardIconKey(quest.getRewardType()));
        if (rewardIcon != null) {
            rewardIcon.setScaling(Scaling.fit);
            rewardBadge.add(rewardIcon).size(28f).padRight(5f);
        }
        rewardBadge.add(rewardLabel).right();
        footer.add(rewardBadge).right().padLeft(8f);

        card.add(footer)
            .width(820f)
            .padBottom(8f)
            .row();

        if (quest.isClaimable()) {
            card.add(assetTextButton(
                    "green_button",
                    "green_button_down",
                    "CLAIM REWARD",
                    () -> claimReward(quest)
                ))
                .width(260f)
                .height(54f)
                .right();
        } else if (quest.isRewardClaimed()) {
            Table claimed = new Table();
            Image check = MenuVisualAssets.image("check");
            if (check != null) {
                check.setScaling(Scaling.fit);
                claimed.add(check).size(28f).padRight(5f);
            }
            Label claimedLabel = secondaryLabel("REWARD CLAIMED");
            claimedLabel.setAlignment(Align.center);
            claimed.add(claimedLabel).center();
            card.add(claimed).width(260f).height(40f).right();
        }

        return card;
    }

    private ProgressBar createProgressBar(
        float target,
        boolean completed
    ) {
        com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle assetStyle =
            new com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable background =
            MenuVisualAssets.drawable("xp_bar");
        com.badlogic.gdx.scenes.scene2d.utils.Drawable fill =
            MenuVisualAssets.drawable(completed ? "xp_fill_green" : "xp_fill_teal");
        if (background != null && fill != null) {
            assetStyle.background = background;
            assetStyle.knobBefore = fill;
            assetStyle.knob = solidDrawable(
                new com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 0f)
            );
            return new ProgressBar(0f, target, 1f, false, assetStyle);
        }

        String style = completed ? "xp_green" : "xp_teal";

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

    private String rewardIconKey(String rewardType) {
        String reward = rewardType == null
            ? ""
            : rewardType.toUpperCase();

        if (reward.contains("GEM") || reward.contains("DIAMOND")) {
            return "gem_small";
        }
        if (reward.contains("FOOD")) {
            return "plantfood";
        }
        if (reward.contains("STAR") || reward.contains("XP")) {
            return "star";
        }
        return "coin_small";
    }

    private String rewardFrameKey(String rewardType) {
        String reward = rewardType == null ? "" : rewardType.toUpperCase();
        if (reward.contains("GEM") || reward.contains("DIAMOND")) {
            return "reward4";
        }
        if (reward.contains("FOOD") || reward.contains("STAR") || reward.contains("XP")) {
            return "reward3";
        }
        return "reward1";
    }

    private String questIconKey(String category) {
        String value = category == null ? "" : category.toUpperCase();
        if (value.contains("EPIC")) {
            return "epic_icon";
        }
        if (value.contains("DAILY")) {
            return "event_lawn";
        }
        if (value.contains("MINIGAME")) {
            return "event_foodfight";
        }
        if (value.contains("MAIN")) {
            return "event_beach";
        }
        return "quest";
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

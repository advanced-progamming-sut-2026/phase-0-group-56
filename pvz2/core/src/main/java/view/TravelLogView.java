package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.TravelLog;
import models.Quest;
import models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TravelLogView extends View {

    private String selectedCategory =
        "ALL";

    public TravelLogView() {
        menu = new TravelLog();
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
    protected void buildContent(
        Table table
    ) {

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

        Label heading =
            mediumTitle(
                "TRAVEL LOG"
            );

        heading.setAlignment(
            Align.center
        );

        table.add(heading)
            .padTop(4f)
            .padBottom(12f)
            .row();

        buildTabs(table);

        if (
            "MINIGAMES".equals(
                selectedCategory
            )
        ) {

            buildMinigameSection(
                table
            );

            return;
        }

        buildQuestList(
            table,
            user
        );
    }

    private void buildTabs(
        Table table
    ) {

        Table tabPanel =
            pvzInnerPanel();

        String[] categories = {
            "ALL",
            "MAIN",
            "DAILY",
            "EPIC",
            "MINIGAMES"
        };

        for (
            String category :
            categories
        ) {

            TextButton button;

            if (
                category.equals(
                    selectedCategory
                )
            ) {

                button =
                    purpleButton(
                        category,
                        () -> {
                        }
                    );

            } else {

                button =
                    brownButton(
                        category,
                        () -> {

                            selectedCategory =
                                category;

                            rebuild();
                        }
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

    private void buildQuestList(
        Table table,
        User user
    ) {

        List<Quest> quests =
            new ArrayList<>(
                user.getActiveQuests()
            );

        quests.sort(
            Comparator
                .comparingInt(
                    Quest::getPriority
                )
                .reversed()
                .thenComparing(
                    Quest::getQuestName
                )
        );

        boolean any =
            false;

        for (
            Quest quest :
            quests
        ) {

            if (
                !"ALL".equals(
                    selectedCategory
                )
                    &&
                    !selectedCategory
                        .equalsIgnoreCase(
                            quest.getCategory()
                        )
            ) {
                continue;
            }

            any = true;

            table.add(
                    buildQuestCard(
                        quest
                    )
                )
                .width(940f)
                .pad(7f)
                .row();
        }

        if (!any) {

            Table emptyPanel =
                pvzPanel();

            Label empty =
                mediumTitle(
                    "NO QUESTS IN THIS CATEGORY"
                );

            empty.setAlignment(
                Align.center
            );

            emptyPanel.add(empty)
                .width(600f)
                .pad(28f);

            table.add(emptyPanel)
                .width(700f)
                .padTop(30f);
        }
    }

    private Table buildQuestCard(
        Quest quest
    ) {

        Table card =
            pvzPanel();

        String state =
            quest.isDone()
                ? "DONE"
                : quest
                .getPriorityName();

        Label questName =
            mediumTitle(
                quest.getQuestName()
                    + "  ["
                    + state
                    + "]"
            );

        card.add(questName)
            .left()
            .growX()
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
            new ProgressBar(
                0f,
                target,
                1f,
                false,
                skin,
                quest.isDone()
                    ? "xp_green"
                    : "xp_teal"
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

        String progressText =
            (int) quest.getProgress()
                + " / "
                + (int) quest.getTarget();

        String reward =
            "REWARD: "
                + quest.getRewardAmount()
                + " "
                + quest.getRewardType();

        Table footer =
            new Table();

        Label progressLabel =
            secondaryLabel(
                "PROGRESS: "
                    + progressText
            );

        Label rewardLabel =
            secondaryLabel(
                reward
            );

        footer.add(progressLabel)
            .left();

        footer.add()
            .expandX();

        footer.add(rewardLabel)
            .right();

        card.add(footer)
            .width(820f);

        return card;
    }

    /*
     * ============================================================
     * MINIGAMES
     * ============================================================
     */

    private void buildMinigameSection(
        Table table
    ) {

        Table descriptionPanel =
            pvzInnerPanel();

        Label description =
            wrappedLabel(
                "Choose one of the mandatory minigames. "
                    + "The gameplay views can be connected here when their "
                    + "graphical implementations are finished.",
                720f
            );

        description.setAlignment(
            Align.center
        );

        descriptionPanel.add(description)
            .width(720f);

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

        title.setAlignment(
            Align.center
        );

        minigames.add(title)
            .colspan(3)
            .padBottom(20f)
            .row();

        minigames.add(
                greenButton(
                    "VASE BREAKER",
                    () ->
                        showMessage(
                            "Vase Breaker view exists, "
                                + "but its phase-2 graphical gameplay still needs to be wired here."
                        )
                )
            )
            .width(250f)
            .height(64f)
            .pad(10f);

        minigames.add(
                purpleButton(
                    "WALL-NUT BOWLING",
                    () ->
                        showMessage(
                            "Wall-nut Bowling view exists, "
                                + "but its phase-2 graphical gameplay still needs to be wired here."
                        )
                )
            )
            .width(250f)
            .height(64f)
            .pad(10f);

        minigames.add(
                brownButton(
                    "I, ZOMBIE",
                    () ->
                        showMessage(
                            "I, Zombie gameplay screen is not present in the current repository yet."
                        )
                )
            )
            .width(250f)
            .height(64f)
            .pad(10f);

        table.add(minigames)
            .width(860f);
    }

    private void rebuild() {

        content.clearChildren();

        buildContent(
            content
        );

        refreshResourceLabels();
    }
}

package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.TravelLog;
import models.Quest;
import models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TravelLogView extends View {
    private String selectedCategory = "ALL";

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
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();
        if (user == null) {
            table.add(new Label("Please log in.", skin));
            return;
        }

        Table tabs = new Table();
        String[] categories = {"ALL", "MAIN", "DAILY", "EPIC", "MINIGAMES"};
        for (String category : categories) {
            tabs.add(button(category, () -> {
                selectedCategory = category;
                content.clearChildren();
                buildContent(content);
            })).width(155f).height(42f).pad(3f);
        }
        table.add(tabs).padBottom(16f).row();

        if ("MINIGAMES".equals(selectedCategory)) {
            buildMinigameSection(table);
            return;
        }

        List<Quest> quests = new ArrayList<>(user.getActiveQuests());
        quests.sort(Comparator.comparingInt(Quest::getPriority).reversed()
            .thenComparing(Quest::getQuestName));

        boolean any = false;
        for (Quest quest : quests) {
            if (!"ALL".equals(selectedCategory)
                && !selectedCategory.equalsIgnoreCase(quest.getCategory())) {
                continue;
            }
            any = true;
            table.add(buildQuestCard(quest)).width(900f).pad(6f).row();
        }

        if (!any) {
            table.add(new Label("No quests in this category.", skin)).pad(20f);
        }
    }

    private Table buildQuestCard(Quest quest) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("image_ui_quests_panel_edge_to_edge_ten"));
        String state = quest.isDone() ? "DONE" : quest.getPriorityName();
        card.add(new Label(quest.getQuestName() + "  [" + state + "]", skin))
            .left().growX().pad(8f).row();
        card.add(wrappedLabel(quest.getDescription(), 820f)).width(820f).left().pad(8f).row();

        float target = Math.max(1f, quest.getTarget());
        ProgressBar progress = new ProgressBar(0f, target, 1f, false, skin);
        progress.setValue(Math.min(target, quest.getProgress()));
        card.add(progress).width(650f).height(22f).left().pad(8f).row();

        String progressText = (int) quest.getProgress() + " / " + (int) quest.getTarget();
        String reward = "Reward: " + quest.getRewardAmount() + " " + quest.getRewardType();
        card.add(new Label(progressText + "    |    " + reward, skin)).left().pad(8f);
        return card;
    }

    private void buildMinigameSection(Table table) {
        table.add(wrappedLabel(
                "The Travel Log is the entry point for the mandatory minigames. "
                    + "Buttons are kept separate from quest categories as required by phase 2.", 760f))
            .width(760f).padBottom(18f).row();

        table.add(button("Vase Breaker", () -> showMessage(
                "Vase Breaker view exists in view.gameview.VaseBreakerView, but its phase-2 gameplay GUI must be "
                    + "finished by the gameplay owner before wiring this button.")))
            .width(300f).height(52f).pad(7f).row();

        table.add(button("Wall-nut Bowling", () -> showMessage(
                "Wall-nut Bowling view exists in view.gameview.WallnutBowlingView, but its phase-2 "
                    + "gameplay GUI must be "
                    + "finished by the gameplay owner before wiring this button.")))
            .width(300f).height(52f).pad(7f).row();

        table.add(button("I, Zombie", () -> showMessage(
                "I, Zombie gameplay screen is not present in the current repository tree. Keep this entry visible so "
                    + "the gameplay implementation can be attached here.")))
            .width(300f).height(52f).pad(7f);
    }
}

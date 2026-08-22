package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import models.App;
import models.User;
import view.gameview.BeghouledView;
import view.gameview.IZombieView;

/** Menu and level selector for the implemented graphical minigames. */
public final class MiniGamesView extends View {
    @Override
    protected String getScreenTitle() {
        return "MINIGAMES";
    }

    @Override
    protected Screen getBackScreen() {
        return new PlayView();
    }

    @Override
    protected void buildContent(Table table) {
        table.top().pad(12f);
        Label hint = new Label(
            "Match plants in Beghouled, or command the zombie side in I, Zombie.",
            skin,
            "medium_outline"
        );
        hint.setAlignment(Align.center);
        table.add(hint).colspan(2).expandX().center().padBottom(18f).row();

        table.add(buildBeghouledCard()).width(520f).height(390f).pad(12f);
        table.add(buildIZombieCard()).width(520f).height(390f).pad(12f);
    }

    private Table buildBeghouledCard() {
        Table card = createCard(
            "BEGHOULED",
            "Swap adjacent plants only when the move forms 3 or more. "
                + "Matches earn sun for upgrades while endless zombies attack."
        );
        addLevelButtons(card, false);
        return card;
    }

    private Table buildIZombieCard() {
        Table card = createCard(
            "I, ZOMBIE",
            "Spend zombie sun beyond the red line, protect your five producers, "
                + "and eat all five brains."
        );
        addLevelButtons(card, true);
        return card;
    }

    private Table createCard(String titleText, String description) {
        Table card = new Table();
        card.top().pad(20f);
        Drawable background =
            getSkinDrawableSafe("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }

        Label title = new Label(titleText, skin, "big_outline");
        title.setAlignment(Align.center);
        Label body = new Label(description, skin);
        body.setWrap(true);
        body.setAlignment(Align.center);

        card.add(title).expandX().center().padBottom(12f).row();
        card.add(body).width(440f).height(105f).center().padBottom(20f).row();
        return card;
    }

    private void addLevelButtons(Table card, boolean iZombie) {
        User user = App.getCurrentuser();
        int unlocked = iZombie && user != null ? user.getIZombie() : 3;
        Table levels = new Table();

        for (int level = 1; level <= 3; level++) {
            int selectedLevel = level;
            boolean available = level <= unlocked;
            TextButton button = new TextButton(
                available ? "LEVEL " + level : "LOCKED " + level,
                skin,
                available ? "green" : "brown"
            );
            button.setDisabled(!available);
            if (available) {
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        App.setScreen(iZombie
                            ? new IZombieView(selectedLevel)
                            : new BeghouledView(selectedLevel));
                    }
                });
            }
            levels.add(button).size(130f, 62f).pad(5f);
        }
        card.add(levels).center();
    }
}

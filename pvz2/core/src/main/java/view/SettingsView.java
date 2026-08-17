package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.Settings;
import models.User;

public class SettingsView extends View {
    public SettingsView() {
        menu = new Settings();
    }

    @Override
    protected String getScreenTitle() {
        return "Settings";
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

        SelectBox<Integer> difficulty = new SelectBox<>(skin);
        difficulty.setItems(new Array<>(new Integer[]{1, 2, 3, 4, 5}));
        difficulty.setSelected(user.getDifficultyLevel());

        SelectBox<Integer> speed = new SelectBox<>(skin);
        speed.setItems(new Array<>(new Integer[]{1, 2, 3}));
        speed.setSelected(user.getGameSpeed());

        CheckBox showGrid = new CheckBox(" Show red board grid", skin);
        showGrid.setChecked(user.isShowGrid());

        CheckBox debug = new CheckBox(" Debug mode", skin);
        debug.setChecked(user.isDebugMode());

        Table form = new Table();
        form.add(new Label("Difficulty (1-5)", skin)).width(260f).left().pad(10f);
        form.add(difficulty).width(240f).pad(10f).row();
        form.add(new Label("Game speed (1-3)", skin)).width(260f).left().pad(10f);
        form.add(speed).width(240f).pad(10f).row();
        form.add(showGrid).colspan(2).left().pad(10f).row();
        form.add(debug).colspan(2).left().pad(10f).row();

        table.add(form).width(620f).padTop(25f).padBottom(22f).row();
        table.add(wrappedLabel(
                "Debug mode enables resource cheat buttons in every graphical menu. "
                    + "Game speed and grid visibility are saved per user for gameplay views to read.", 650f))
            .width(650f).padBottom(18f).row();

        table.add(button("Save settings", () -> {
            Settings controller = (Settings) menu;
            String first = controller.ChangeHardness(difficulty.getSelected());
            String second = controller.changeGameSpeed(speed.getSelected());
            String third = controller.setGridVisible(showGrid.isChecked());
            String fourth = controller.setDebugMode(debug.isChecked());
            showMessage(first + "\n" + second + "\n" + third + "\n" + fourth);
            refreshResourceLabels();
        })).width(260f).height(50f);
    }
}

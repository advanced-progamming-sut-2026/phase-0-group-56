package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
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
                "GAME SETTINGS"
            );

        heading.setAlignment(
            Align.center
        );

        table.add(heading)
            .padTop(10f)
            .padBottom(16f)
            .row();

        SelectBox<Integer> difficulty =
            new SelectBox<>(skin);

        difficulty.setItems(
            new Array<>(
                new Integer[]{
                    1,
                    2,
                    3,
                    4,
                    5
                }
            )
        );

        difficulty.setSelected(
            user.getDifficultyLevel()
        );

        SelectBox<Integer> speed =
            new SelectBox<>(skin);

        speed.setItems(
            new Array<>(
                new Integer[]{
                    1,
                    2,
                    3
                }
            )
        );

        speed.setSelected(
            user.getGameSpeed()
        );

        CheckBox showGrid =
            new CheckBox(
                " SHOW RED BOARD GRID",
                skin
            );

        showGrid.setChecked(
            user.isShowGrid()
        );

        CheckBox debug =
            new CheckBox(
                " DEBUG MODE",
                skin
            );

        debug.setChecked(
            user.isDebugMode()
        );

        Table panel =
            pvzPanel();

        addOption(
            panel,
            "DIFFICULTY",
            difficulty
        );

        addOption(
            panel,
            "GAME SPEED",
            speed
        );

        panel.add(showGrid)
            .colspan(2)
            .left()
            .pad(
                16f,
                20f,
                10f,
                20f
            )
            .row();

        panel.add(debug)
            .colspan(2)
            .left()
            .pad(
                10f,
                20f,
                16f,
                20f
            )
            .row();

        table.add(panel)
            .width(650f)
            .padBottom(16f)
            .row();

        Table helpPanel =
            pvzInnerPanel();

        Label description =
            wrappedLabel(
                "Difficulty changes game hardness. "
                    + "Game Speed controls gameplay speed from 1x to 3x. "
                    + "Grid displays the board helper. "
                    + "Debug Mode enables resource cheat buttons.",
                600f
            );

        description.setAlignment(
            Align.center
        );

        helpPanel.add(description)
            .width(600f);

        table.add(helpPanel)
            .width(660f)
            .padBottom(20f)
            .row();

        table.add(
                greenButton(
                    "SAVE SETTINGS",
                    () -> {

                        Settings controller =
                            (Settings) menu;

                        String first =
                            controller
                                .ChangeHardness(
                                    difficulty
                                        .getSelected()
                                );

                        String second =
                            controller
                                .changeGameSpeed(
                                    speed
                                        .getSelected()
                                );

                        String third =
                            controller
                                .setGridVisible(
                                    showGrid
                                        .isChecked()
                                );

                        String fourth =
                            controller
                                .setDebugMode(
                                    debug
                                        .isChecked()
                                );

                        showMessage(
                            first
                                + "\n"
                                + second
                                + "\n"
                                + third
                                + "\n"
                                + fourth
                        );

                        refreshResourceLabels();
                    }
                )
            )
            .width(300f)
            .height(58f);
    }

    private void addOption(
        Table table,
        String title,
        com.badlogic.gdx.scenes.scene2d.Actor actor
    ) {

        Label label =
            new Label(
                title,
                skin,
                "medium_outline"
            );

        table.add(label)
            .width(270f)
            .left()
            .pad(14f);

        table.add(actor)
            .width(230f)
            .height(44f)
            .pad(14f)
            .row();
    }
}

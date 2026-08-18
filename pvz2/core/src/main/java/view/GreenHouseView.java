package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.GreenHouseController;
import models.App;
import models.Pot;
import models.User;

public class GreenHouseView extends View {

    public GreenHouseView() {
        menu = new GreenHouseController();
    }

    @Override
    protected String getScreenTitle() {
        return "Greenhouse";
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

        Label title =
            mediumTitle(
                "ZEN GARDEN"
            );

        title.setAlignment(
            Align.center
        );

        table.add(title)
            .padTop(6f)
            .padBottom(12f)
            .row();

        Table hintPanel =
            pvzInnerPanel();

        Label hint =
            wrappedLabel(
                "Plant seedlings, wait for them to grow, "
                    + "accelerate growth with gems, "
                    + "and collect rewards when ready.",
                760f
            );

        hint.setAlignment(
            Align.center
        );

        hintPanel.add(hint)
            .width(760f);

        table.add(hintPanel)
            .width(820f)
            .padBottom(16f)
            .row();

        Table gardenPanel =
            pvzPanel();

        Table pots =
            new Table();

        for (int y = 1; y <= 4; y++) {

            for (int x = 1; x <= 5; x++) {

                final int px = x;
                final int py = y;

                Pot pot =
                    user
                        .getGreenHouse()
                        .getPotByPosition(
                            x,
                            y
                        );

                TextButton potButton;

                if (
                    pot != null
                        && !pot.isUnlocked()
                ) {

                    potButton =
                        brownButton(
                            potText(pot),
                            () ->
                                handlePot(
                                    px,
                                    py
                                )
                        );

                } else if (
                    pot != null
                        && pot.getSeedling()
                        != null
                        && pot
                        .getRemainingHours()
                        <= 0
                ) {

                    potButton =
                        purpleButton(
                            potText(pot),
                            () ->
                                handlePot(
                                    px,
                                    py
                                )
                        );

                } else {

                    potButton =
                        greenButton(
                            potText(pot),
                            () ->
                                handlePot(
                                    px,
                                    py
                                )
                        );
                }

                pots.add(potButton)
                    .width(175f)
                    .height(100f)
                    .pad(5f);
            }

            pots.row();
        }

        gardenPanel.add(pots);

        table.add(gardenPanel)
            .padBottom(16f)
            .row();

        table.add(
                purpleButton(
                    "OPEN SHOP",
                    () ->
                        App.setScreen(
                            new ShopView()
                        )
                )
            )
            .width(280f)
            .height(56f);
    }

    private String potText(
        Pot pot
    ) {

        if (pot == null) {

            return "INVALID POT";
        }

        if (!pot.isUnlocked()) {

            return "LOCKED\n"
                + "("
                + pot.getX()
                + ", "
                + pot.getY()
                + ")";
        }

        if (
            pot.getSeedling()
                == null
        ) {

            return "EMPTY\nPLANT HERE\n"
                + "("
                + pot.getX()
                + ", "
                + pot.getY()
                + ")";
        }

        if (
            pot.getRemainingHours()
                <= 0
        ) {

            return pot.getSeedling()
                + "\nREADY!\nCOLLECT";
        }

        return pot.getSeedling()
            + "\n"
            + pot.getRemainingHours()
            + "h REMAINING";
    }

    private void handlePot(
        int x,
        int y
    ) {

        User user =
            Data.getCurrentUser();

        if (user == null) {
            return;
        }

        Pot pot =
            user
                .getGreenHouse()
                .getPotByPosition(
                    x,
                    y
                );

        if (pot == null) {

            showMessage(
                "Error: invalid pot."
            );

            return;
        }

        if (!pot.isUnlocked()) {

            showMessage(
                "This pot is locked. "
                    + "Buy more pots from the shop."
            );

            return;
        }

        GreenHouseController controller =
            (GreenHouseController) menu;

        if (
            pot.getSeedling()
                == null
        ) {

            showConfirmation(
                "Plant Pot",
                "Plant a random greenhouse plant here?",
                () -> {

                    showMessage(
                        controller.plant(
                            x,
                            y
                        )
                    );

                    reloadGreenhouse();
                }
            );

            return;
        }

        if (
            pot.getRemainingHours()
                <= 0
        ) {

            showConfirmation(
                "Collect Reward",
                "Collect this fully grown plant?",
                () -> {

                    showMessage(
                        controller.collect(
                            x,
                            y,
                            false
                        )
                    );

                    reloadGreenhouse();
                }
            );

            return;
        }

        int cost =
            pot.getRemainingHours();

        showConfirmation(
            "Accelerate Growth",
            "Spend "
                + cost
                + " gem(s) to finish growth immediately?",
            () -> {

                showMessage(
                    controller.forceGrow(
                        x,
                        y,
                        cost
                    )
                );

                reloadGreenhouse();
            }
        );
    }

    private void reloadGreenhouse() {

        Data.saveUser();

        content.clearChildren();

        buildContent(content);

        refreshResourceLabels();
    }
}

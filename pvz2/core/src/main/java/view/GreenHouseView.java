package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

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
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();
        if (user == null) {
            table.add(new Label("Please log in.", skin));
            return;
        }

        table.add(wrappedLabel(
                "Each pot shows its lock state, plant and remaining growth time. "
                    + "Click a pot to plant, accelerate growth or collect its reward.", 850f))
            .width(850f).padBottom(12f).row();

        Table pots = new Table();
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                final int px = x;
                final int py = y;
                Pot pot = user.getGreenHouse().getPotByPosition(x, y);
                TextButton button = button(potText(pot), () -> handlePot(px, py));
                pots.add(button).width(190f).height(105f).pad(5f);
            }
            pots.row();
        }
        table.add(pots).padBottom(15f).row();

        table.add(button("Open Shop", () -> App.setScreen(new ShopView())))
            .width(260f).height(48f).padTop(8f);
    }

    private String potText(Pot pot) {
        if (pot == null) {
            return "Invalid pot";
        }
        if (!pot.isUnlocked()) {
            return "LOCKED\n(" + pot.getX() + ", " + pot.getY() + ")";
        }
        if (pot.getSeedling() == null) {
            return "EMPTY\nPlant here\n(" + pot.getX() + ", " + pot.getY() + ")";
        }
        if (pot.getRemainingHours() <= 0) {
            return pot.getSeedling() + "\nREADY\nClick to collect";
        }
        return pot.getSeedling() + "\n" + pot.getRemainingHours() + "h remaining\nClick for actions";
    }

    private void handlePot(int x, int y) {
        User user = Data.getCurrentUser();
        if (user == null) {
            return;
        }
        Pot pot = user.getGreenHouse().getPotByPosition(x, y);
        if (pot == null) {
            showMessage("Error: invalid pot.");
            return;
        }
        if (!pot.isUnlocked()) {
            showMessage("This pot is locked. Buy more pots from the shop.");
            return;
        }

        GreenHouseController controller = (GreenHouseController) menu;
        if (pot.getSeedling() == null) {
            showConfirmation("Plant pot", "Plant a random greenhouse plant here?", () -> {
                showMessage(controller.plant(x, y));
                reloadGreenhouse();
            });
            return;
        }
        if (pot.getRemainingHours() <= 0) {
            showConfirmation("Collect reward", "Collect this fully grown plant?", () -> {
                showMessage(controller.collect(x, y, false));
                reloadGreenhouse();
            });
            return;
        }

        int cost = pot.getRemainingHours();
        showConfirmation(
            "Accelerate growth",
            "Spend " + cost + " gem(s) to finish growth immediately?",
            () -> {
                showMessage(controller.forceGrow(x, y, cost));
                reloadGreenhouse();
            });
    }

    private void reloadGreenhouse() {
        Data.saveUser();
        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }
}

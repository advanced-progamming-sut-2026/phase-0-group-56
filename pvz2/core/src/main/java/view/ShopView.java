package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.Shop;
import models.User;
import models.factory.builder.PlantType;

import java.util.ArrayList;
import java.util.List;

public class ShopView extends View {
    public ShopView() {
        menu = new Shop();
    }

    @Override
    protected String getScreenTitle() {
        return "Shop";
    }

    @Override
    protected Screen getBackScreen() {
        return new GreenHouseView();
    }

    @Override
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();
        if (user == null) {
            table.add(new Label("Please log in.", skin));
            return;
        }

        Shop shop = (Shop) menu;
        String daily = shop.setDailyOffer();

        table.add(wrappedLabel("All purchases ask for confirmation before changing resources.", 760f))
            .width(760f).padBottom(12f).row();

        addItem(table, "Pot", "2000 Coins", "Unlock one greenhouse pot (maximum 20).",
            () -> confirmPurchase("Pot", "Buy one pot for 2000 coins?", () -> shop.purchase("pot", 1)));

        addItem(table, "Plant Food", "3 Gems", "Add one Plant Food (maximum stored: 3).",
            () -> confirmPurchase("Plant Food", "Buy one Plant Food for 3 gems?",
                () -> shop.purchase("plantfood", 1)));

        addItem(table, "Random Seed Packets", "1000 Coins", "Receive 5 random seed packets.",
            () -> confirmPurchase("Random Seed Packets", "Buy 5 random seed packets for 1000 coins?",
                shop::randomPurchase));

        addSpecificSeedItem(table, shop, user);

        addItem(table, "Currency Exchange", "5 Gems", "Convert 5 gems into 500 coins.",
            () -> confirmPurchase("Currency Exchange", "Exchange 5 gems for 500 coins?",
                () -> shop.purchase("exchange", 1)));

        addItem(table, "Daily Offer", "1600 Coins", daily,
            () -> confirmPurchase("Daily Offer", "Buy today's discounted seed packet offer?",
                () -> shop.purchase("daily", 1)));
    }

    private void addSpecificSeedItem(Table table, Shop shop, User user) {
        List<String> names = new ArrayList<>();
        for (PlantType type : user.getUnlockedPlants()) {
            names.add(type.name());
        }
        if (names.isEmpty()) {
            names.add("PEASHOOTER");
        }
        SelectBox<String> plant = new SelectBox<>(skin);
        plant.setItems(new Array<>(names.toArray(new String[0])));

        Table row = new Table();
        Table text = new Table();
        text.add(new Label("Specific Seed Packets", skin)).left().row();
        text.add(new Label("Price: 5 Gems", skin)).left().row();
        text.add(wrappedLabel("Choose an unlocked plant and receive 10 seed packets.", 430f))
            .width(430f).left();
        row.add(text).width(500f).left().pad(10f);
        row.add(plant).width(250f).pad(10f);
        row.add(button("Buy", () -> confirmPurchase(
                "Specific Seed Packets",
                "Buy 10 " + plant.getSelected() + " seed packets for 5 gems?",
                () -> shop.normalPurchase(plant.getSelected()))))
            .width(150f).height(44f).pad(10f);
        table.add(row).width(980f).pad(5f).row();
    }

    private interface PurchaseAction {
        String run();
    }

    private void addItem(Table table, String title, String price, String description, Runnable buyAction) {
        Table row = new Table();
        Table text = new Table();
        text.add(new Label(title, skin)).left().row();
        text.add(new Label("Price: " + price, skin)).left().row();
        text.add(wrappedLabel(description, 590f)).width(590f).left();
        row.add(text).width(700f).left().pad(10f);
        row.add(button("Buy", buyAction)).width(150f).height(44f).pad(10f);
        table.add(row).width(980f).pad(5f).row();
    }

    private void confirmPurchase(String title, String message, PurchaseAction purchase) {
        showConfirmation(title, message, () -> {
            String result = purchase.run();
            showMessage(result);
            refreshResourceLabels();
            if (!result.startsWith("Error:")) {
                Data.saveUser();
                content.clearChildren();
                buildContent(content);
                refreshResourceLabels();
            }
        });
    }
}

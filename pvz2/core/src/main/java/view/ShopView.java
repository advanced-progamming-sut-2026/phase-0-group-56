package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
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

        Shop shop =
            (Shop) menu;

        String daily =
            shop.setDailyOffer();

        Label heading =
            mediumTitle(
                "CRAZY DAVE'S SHOP"
            );

        heading.setAlignment(
            Align.center
        );

        table.add(heading)
            .padTop(4f)
            .padBottom(12f)
            .row();

        Table hintPanel =
            pvzInnerPanel();

        Label hint =
            wrappedLabel(
                "Every purchase asks for confirmation "
                    + "before coins or gems are spent.",
                720f
            );

        hint.setAlignment(
            Align.center
        );

        hintPanel.add(hint)
            .width(720f);

        table.add(hintPanel)
            .width(780f)
            .padBottom(14f)
            .row();

        addItem(
            table,
            "POT",
            "2000 COINS",
            "Unlock one greenhouse pot. Maximum: 20.",
            () ->
                confirmPurchase(
                    "Pot",
                    "Buy one pot for 2000 coins?",
                    () ->
                        shop.purchase(
                            "pot",
                            1
                        )
                )
        );

        addItem(
            table,
            "PLANT FOOD",
            "3 GEMS",
            "Add one Plant Food. Maximum stored: 3.",
            () ->
                confirmPurchase(
                    "Plant Food",
                    "Buy one Plant Food for 3 gems?",
                    () ->
                        shop.purchase(
                            "plantfood",
                            1
                        )
                )
        );

        addItem(
            table,
            "RANDOM SEED PACKETS",
            "1000 COINS",
            "Receive 5 random seed packets.",
            () ->
                confirmPurchase(
                    "Random Seed Packets",
                    "Buy 5 random seed packets for 1000 coins?",
                    shop::randomPurchase
                )
        );

        addSpecificSeedItem(
            table,
            shop,
            user
        );

        addItem(
            table,
            "CURRENCY EXCHANGE",
            "5 GEMS",
            "Convert 5 gems into 500 coins.",
            () ->
                confirmPurchase(
                    "Currency Exchange",
                    "Exchange 5 gems for 500 coins?",
                    () ->
                        shop.purchase(
                            "exchange",
                            1
                        )
                )
        );

        addItem(
            table,
            "DAILY OFFER",
            "1600 COINS",
            daily,
            () ->
                confirmPurchase(
                    "Daily Offer",
                    "Buy today's discounted seed packet offer?",
                    () ->
                        shop.purchase(
                            "daily",
                            1
                        )
                )
        );
    }

    private void addSpecificSeedItem(
        Table table,
        Shop shop,
        User user
    ) {

        List<String> names =
            new ArrayList<>();

        for (
            PlantType type :
            user.getUnlockedPlants()
        ) {

            names.add(
                type.name()
            );
        }

        if (names.isEmpty()) {
            names.add("PEASHOOTER");
        }

        SelectBox<String> plant =
            new SelectBox<>(skin);

        plant.setItems(
            new Array<>(
                names.toArray(
                    new String[0]
                )
            )
        );

        Table card =
            pvzPanel();

        Table text =
            new Table();

        Label title =
            mediumTitle(
                "SPECIFIC SEED PACKETS"
            );

        text.add(title)
            .left()
            .row();

        Label price =
            secondaryLabel(
                "PRICE: 5 GEMS"
            );

        text.add(price)
            .left()
            .padTop(4f)
            .row();

        text.add(
                wrappedLabel(
                    "Choose an unlocked plant "
                        + "and receive 10 seed packets.",
                    430f
                )
            )
            .width(430f)
            .left()
            .padTop(8f);

        card.add(text)
            .width(490f)
            .left()
            .pad(10f);

        card.add(plant)
            .width(240f)
            .height(44f)
            .pad(10f);

        card.add(
                greenButton(
                    "BUY",
                    () ->
                        confirmPurchase(
                            "Specific Seed Packets",
                            "Buy 10 "
                                + plant.getSelected()
                                + " seed packets for 5 gems?",
                            () ->
                                shop.normalPurchase(
                                    plant.getSelected()
                                )
                        )
                )
            )
            .width(150f)
            .height(50f)
            .pad(10f);

        table.add(card)
            .width(980f)
            .pad(6f)
            .row();
    }

    private interface PurchaseAction {
        String run();
    }

    private void addItem(
        Table table,
        String titleText,
        String priceText,
        String description,
        Runnable buyAction
    ) {

        Table card =
            pvzPanel();

        Table text =
            new Table();

        Label title =
            mediumTitle(
                titleText
            );

        text.add(title)
            .left()
            .row();

        Label price =
            secondaryLabel(
                "PRICE: "
                    + priceText
            );

        text.add(price)
            .left()
            .padTop(4f)
            .row();

        text.add(
                wrappedLabel(
                    description,
                    580f
                )
            )
            .width(580f)
            .left()
            .padTop(8f);

        card.add(text)
            .width(700f)
            .left()
            .pad(10f);

        card.add(
                greenButton(
                    "BUY",
                    buyAction
                )
            )
            .width(160f)
            .height(52f)
            .pad(10f);

        table.add(card)
            .width(980f)
            .pad(6f)
            .row();
    }

    private void confirmPurchase(
        String title,
        String message,
        PurchaseAction purchase
    ) {

        showConfirmation(
            title,
            message,
            () -> {

                String result =
                    purchase.run();

                showMessage(result);

                refreshResourceLabels();

                if (
                    !result.startsWith(
                        "Error:"
                    )
                ) {

                    Data.saveUser();

                    content
                        .clearChildren();

                    buildContent(
                        content
                    );

                    refreshResourceLabels();
                }
            }
        );
    }
}

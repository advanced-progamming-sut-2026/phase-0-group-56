package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

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

        table.add(
                menuSectionHeader(
                    "shop",
                    "CRAZY DAVE'S SHOP",
                    "Spend coins and gems on permanent and daily offers."
                )
            )
            .width(980f)
            .padTop(4f)
            .padBottom(12f)
            .row();

        Table hintPanel =
            pvzInnerPanel();

        Label hint =
            wrappedLabel(
                "Every purchase asks for confirmation "
                    + "before coins or gems are spent.",
                620f
            );

        hint.setAlignment(
            Align.center
        );

        Image dave = MenuVisualAssets.image("dave_waist");
        if (dave != null) {
            dave.setScaling(Scaling.fit);
            hintPanel.add(dave)
                .size(78f)
                .padRight(10f);
        }

        hintPanel.add(hint)
            .width(620f);

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
            "Receive 5 seed packets for one random unlocked plant.",
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

            if (
                type != null
                    && type
                    != PlantType.MARIGOLD
            ) {

                names.add(
                    type.name()
                );
            }
        }

        names.sort(
            String.CASE_INSENSITIVE_ORDER
        );

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

        Image currencyIcon = MenuVisualAssets.image("gem");
        if (currencyIcon != null) {
            currencyIcon.setScaling(Scaling.fit);
            card.add(currencyIcon)
                .size(54f)
                .center()
                .padRight(8f);
        }

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
                assetTextButton(
                    "gems_buy",
                    "gems_buy_down",
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
            .width(1080f)
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

        String productIconKey = productIconKey(titleText, priceText);
        Image currencyIcon = MenuVisualAssets.image(productIconKey);
        if (currencyIcon != null) {
            currencyIcon.setScaling(Scaling.fit);
            card.add(currencyIcon)
                .size(54f)
                .center()
                .padRight(8f);
        }

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
                assetTextButton(
                    purchaseButtonKey(titleText, priceText),
                    purchaseButtonDownKey(titleText, priceText),
                    "BUY",
                    buyAction
                )
            )
            .width(160f)
            .height(52f)
            .pad(10f);

        table.add(card)
            .width(1080f)
            .pad(6f)
            .row();
    }

    private String productIconKey(
        String titleText,
        String priceText
    ) {
        String title = titleText == null ? "" : titleText.toUpperCase();

        if (title.contains("POT")) {
            return "pot";
        }
        if (title.contains("PLANT FOOD")) {
            return "plantfood";
        }
        if (title.contains("DAILY")) {
            return "star";
        }
        if (title.contains("CURRENCY")) {
            return "gem";
        }

        return priceText != null && priceText.toUpperCase().contains("GEM")
            ? "gem"
            : "coin";
    }

    private String purchaseButtonKey(
        String titleText,
        String priceText
    ) {
        String title = titleText == null ? "" : titleText.toUpperCase();
        String price = priceText == null ? "" : priceText.toUpperCase();

        if (title.contains("CURRENCY")) {
            return "generic_currency";
        }
        if (price.contains("GEM")) {
            return "gems_buy";
        }
        return "coin_buy";
    }

    private String purchaseButtonDownKey(
        String titleText,
        String priceText
    ) {
        String normalKey = purchaseButtonKey(titleText, priceText);
        return switch (normalKey) {
            case "generic_currency" -> "generic_currency_down";
            case "gems_buy" -> "gems_buy_down";
            default -> "coin_buy_down";
        };
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

package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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

        Shop shop =
            (Shop) menu;

        String daily =
            shop.setDailyOffer();

        table.add(buildShopHeader())
            .width(1000f)
            .padTop(4f)
            .padBottom(12f)
            .row();

        Table hintPanel =
            pvzInnerPanel();

        Label hint = secondaryLabel(
            "Every purchase asks for confirmation before coins or gems are spent."
        );

        hint.setAlignment(
            Align.center
        );
        hint.setWrap(true);

        Image dave = MenuVisualAssets.image("dave_waist");
        if (dave != null) {
            dave.setScaling(Scaling.fit);
            hintPanel.add(dave)
                .size(96f)
                .padRight(10f);
        }

        hintPanel.add(hint)
            .width(680f);

        table.add(hintPanel)
            .width(900f)
            .padBottom(14f)
            .row();

        addItem(
            table,
            "POT",
            "2000 COINS",
            "Unlock one greenhouse pot. Maximum: 12 (the Zen Garden board).",
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

    /**
     * Store header built from the extracted PvZ2 world-map artwork.  The
     * cart is deliberately used instead of the generic shirt icon so the
     * screen immediately reads as the in-game Shop.
     */
    private Table buildShopHeader() {
        Table header = pvzPanel();
        header.pad(12f, 22f, 12f, 22f);

        Image cart = MenuVisualAssets.image("store_cart");
        if (cart != null) {
            cart.setScaling(Scaling.fit);
            cart.setTouchable(Touchable.disabled);
            header.add(cart)
                .size(112f)
                .padRight(18f)
                .center();
        }

        Table copy = new Table();
        Label title = titleLabel("CRAZY DAVE'S SHOP");
        title.setAlignment(Align.left);
        copy.add(title)
            .left()
            .growX()
            .row();

        Label subtitle = secondaryLabel(
            "Spend coins and gems on permanent upgrades, seed packets and daily offers."
        );
        subtitle.setWrap(true);
        subtitle.setAlignment(Align.left);
        copy.add(subtitle)
            .width(580f)
            .left()
            .padTop(5f);
        header.add(copy)
            .growX()
            .left();

        Image saleBanner = MenuVisualAssets.image("store_sale_banner");
        if (saleBanner != null) {
            saleBanner.setScaling(Scaling.fit);
            Stack saleBadge = new Stack();
            saleBadge.setTouchable(Touchable.disabled);
            saleBadge.add(saleBanner);

            Label saleLabel = mediumTitle("DAILY\nOFFERS");
            saleLabel.setAlignment(Align.center);
            saleLabel.setTouchable(Touchable.disabled);
            saleBadge.add(saleLabel);

            header.add(saleBadge)
                .size(170f, 62f)
                .padLeft(12f)
                .center();
        }

        return header;
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

        Table card = pvzInnerPanel();
        card.pad(13f, 16f, 13f, 16f);

        Image currencyIcon = MenuVisualAssets.image("gem");
        if (currencyIcon != null) {
            currencyIcon.setScaling(Scaling.fit);
            card.add(currencyIcon)
                .size(86f)
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

        Label seedDescription = secondaryLabel(
            "Choose an unlocked plant and receive 10 seed packets."
        );
        seedDescription.setWrap(true);
        text.add(seedDescription)
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
            .width(175f)
            .height(58f)
            .pad(10f);

        table.add(card)
            .width(1000f)
            .pad(5f)
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

        Table card = pvzInnerPanel();
        card.pad(13f, 16f, 13f, 16f);

        String productIconKey = productIconKey(titleText, priceText);
        Image currencyIcon = MenuVisualAssets.image(productIconKey);
        if (currencyIcon != null) {
            currencyIcon.setScaling(Scaling.fit);
            card.add(currencyIcon)
                .size(86f)
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

        Label descriptionLabel = secondaryLabel(description);
        descriptionLabel.setWrap(true);
        text.add(descriptionLabel)
            .width(610f)
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
            .width(175f)
            .height(58f)
            .pad(8f);

        table.add(card)
            .width(1000f)
            .pad(5f)
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

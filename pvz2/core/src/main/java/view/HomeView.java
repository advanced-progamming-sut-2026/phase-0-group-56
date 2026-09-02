package view;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controllers.datacontroller.Data;
import controllers.menus.Home;
import models.App;
import models.User;

public class HomeView extends View {

    public HomeView() {
        menu = new Home();
        App.setCurrentmenu(menu);
    }

    @Override
    protected String getScreenTitle() {
        return "Main Menu";
    }

    @Override
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();

        if (user == null) {
            buildNoUserScreen(table);
            return;
        }

        String nickname = user.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = user.getName();
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = "PLAYER";
        }

        Table hero = pvzPanel();
        hero.pad(18f, 24f, 16f, 24f);

        Image pot = MenuVisualAssets.image("pot");
        if (pot != null) {
            pot.setScaling(Scaling.fit);
            hero.add(pot).size(90f).padRight(16f).center();
        }

        Table heroCopy = new Table();
        Label welcome = mediumTitle("WELCOME, " + nickname.toUpperCase() + "!");
        welcome.setAlignment(Align.left);
        heroCopy.add(welcome).left().growX().row();

        Label subtitle = secondaryLabel(
            "Your garden is ready. Choose a destination and keep the lawn growing."
        );
        subtitle.setWrap(true);
        subtitle.setAlignment(Align.left);
        heroCopy.add(subtitle).width(620f).left().padTop(4f).row();

        Table progressLine = new Table();
        Image star = MenuVisualAssets.image("star");
        if (star != null) {
            star.setScaling(Scaling.fit);
            progressLine.add(star).size(25f).padRight(5f);
        }
        progressLine.add(secondaryLabel(
            "LEVELS " + user.getLevelsPassed()
                + "   •   " + user.getLastProgressText()
                + "   •   DIFFICULTY " + user.getDifficultyLevel()
        )).left();
        heroCopy.add(progressLine).left().padTop(8f);
        hero.add(heroCopy).growX().left();

        table.add(hero).width(900f).center().padTop(4f).padBottom(14f).row();

        Table stats = pvzInnerPanel();
        stats.pad(9f, 14f, 9f, 14f);
        stats.defaults().width(210f).height(58f).pad(4f);
        stats.add(statCard("coin_small", "COINS", String.valueOf(user.getCoins())));
        stats.add(statCard("gem_small", "GEMS", String.valueOf(user.getDiamonds())));
        stats.add(statCard("star", "LEVELS CLEARED", String.valueOf(user.getLevelsPassed())));
        stats.add(statCard("quest", "ACTIVE QUESTS", String.valueOf(user.getActiveQuests().size())));
        table.add(stats).width(920f).center().padBottom(14f).row();

        Table navigation = pvzPanel();
        navigation.pad(14f, 18f, 16f, 18f);
        Label navigationTitle = mediumTitle("GARDEN DESTINATIONS");
        navigationTitle.setAlignment(Align.center);
        navigation.add(navigationTitle).colspan(4).growX().padBottom(8f).row();

        navigation.defaults().width(210f).height(136f).pad(5f);
        navigation.add(homeTile("event_beach", "event_beach_down", "almanac",
            "ADVENTURE", "Continue your journey", () -> openMenu("Adventure menu")));
        navigation.add(homeTile("event_lawn", "event_lawn_down", "almanac",
            "COLLECTION", "Plants and unlocks", () -> openMenu("Collection menu")));
        navigation.add(homeTile("quest", "quest_down", "hud_quests",
            "TRAVEL LOG", "Quests and rewards", () -> openMenu("Travel Log menu")));
        navigation.add(homeTile("greenhouse", null, "hud_zg",
            "GREENHOUSE", "Grow your plants", () -> openMenu("Greenhouse menu")));
        navigation.row();
        navigation.add(homeTile("shop", "store_cart_down", "shop",
            "SHOP", "Daily offers", () -> openMenu("Shop menu")));
        boolean hasUnreadNews = !user.getUnreadNews().isEmpty();
        navigation.add(homeTile("news", null, "news",
            hasUnreadNews ? "NEWS  •  NEW" : "NEWS",
            hasUnreadNews ? "Unread updates" : "Latest updates",
            () -> openMenu("News menu"),
            hasUnreadNews));
        navigation.add(homeTile("star", null, "almanac",
            "LEADERBOARD", "Top players", () -> openMenu("Leaderboard menu")));
        navigation.add(homeTile("pot", null, "almanac",
            "PROFILE", "Your garden", () -> openMenu("Profile menu")));
        navigation.row();
        navigation.add(homeTile("almanac", null, "almanac",
            "NETWORK PLAY", "Two-player I, Zombie", () -> openMenu("Network menu")));
        navigation.add(homeTile("coin", null, "shop",
            "WALLET", "Currencies", () -> openMenu("Wallet menu")));
        navigation.add(homeTile("settings", null, "settings",
            "SETTINGS", "Game options", () -> openMenu("Settings menu")));
        navigation.add().width(210f).height(136f).pad(5f);

        table.add(navigation).width(940f).center().padBottom(14f).row();

        Table status = pvzInnerPanel();
        status.pad(9f, 18f, 9f, 18f);
        Label statusLabel = secondaryLabel(
            "ACCOUNT STATUS  •  "
                + (user.isDebugMode() ? "DEBUG MODE ON" : "STANDARD MODE")
                + "  •  " + user.getLastProgressText()
        );
        statusLabel.setAlignment(Align.center);
        status.add(statusLabel).width(820f).center();
        table.add(status).width(900f).center().padBottom(12f).row();

        Table accountActions = new Table();
        accountActions.add(assetTextButton("brown_button", "brown_button_down",
            "LOG OUT", this::confirmLogout)).width(225f).height(52f).padRight(10f);
        accountActions.add(assetTextButton("brown_button", "brown_button_down",
            "EXIT GAME", this::confirmExit)).width(225f).height(52f).padLeft(10f);
        table.add(accountActions).center().padBottom(8f);
    }

    private Table statCard(String iconKey, String caption, String value) {
        Table card = pvzInnerPanel();
        card.pad(6f, 10f, 6f, 10f);
        Image icon = MenuVisualAssets.image(iconKey);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            card.add(icon).size(28f).padRight(5f);
        }
        Table labels = new Table();
        labels.add(secondaryLabel(caption)).left().row();
        labels.add(mediumTitle(value)).left();
        card.add(labels).left();
        return card;
    }

    private Table homeTile(
        String iconKey,
        String pressedKey,
        String fallbackStyle,
        String title,
        String caption,
        Runnable action
    ) {
        return homeTile(
            iconKey,
            pressedKey,
            fallbackStyle,
            title,
            caption,
            action,
            false
        );
    }

    /**
     * Builds a home-menu destination card.  The optional notification state
     * is rendered as a badge over the artwork while keeping the whole card
     * as the hit target.
     */
    private Table homeTile(
        String iconKey,
        String pressedKey,
        String fallbackStyle,
        String title,
        String caption,
        Runnable action,
        boolean showNotification
    ) {
        Table tile = pvzInnerPanel();
        tile.pad(8f, 7f, 8f, 7f);

        ImageButton icon = MenuVisualAssets.imageButton(iconKey, pressedKey, null);
        if (icon == null) {
            try {
                icon = new ImageButton(skin, fallbackStyle);
            } catch (Exception ignored) {
                icon = new ImageButton(skin);
            }
        }
        // The card is the hit target, not only the artwork.  Disabling
        // touch on the child lets Scene2D bubble clicks from the icon and
        // the labels to the card listener below.
        icon.setTouchable(Touchable.disabled);

        Stack iconStack = new Stack();
        iconStack.setTouchable(Touchable.disabled);
        icon.setSize(68f, 68f);
        iconStack.add(icon);

        if (showNotification) {
            Image notification = MenuVisualAssets.image("notification");
            if (notification == null) {
                notification = MenuVisualAssets.image("red_dot");
            }
            if (notification != null) {
                notification.setScaling(Scaling.fit);
                notification.setTouchable(Touchable.disabled);
                notification.setSize(25f, 25f);
                iconStack.add(notification);
            }
        }

        tile.add(iconStack).size(68f).center().padBottom(3f).row();

        Label titleLabel = mediumTitle(title);
        titleLabel.setAlignment(Align.center);
        titleLabel.setTouchable(Touchable.disabled);
        tile.add(titleLabel).width(190f).center().row();

        Label captionLabel = secondaryLabel(caption);
        captionLabel.setAlignment(Align.center);
        captionLabel.setTouchable(Touchable.disabled);
        tile.add(captionLabel).width(190f).center();

        tile.setTouchable(Touchable.enabled);
        tile.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
        return tile;
    }

    private void openMenu(String menuName) {
        String result = menu.ChangeMenu(menuName);
        if (result != null && result.startsWith("Error")) {
            showMessage(result);
        }
    }

    private void confirmLogout() {
        showConfirmation(
            "Log Out",
            "Are you sure you want to log out of this account?",
            () -> ((Home) menu).LogOut()
        );
    }

    private void confirmExit() {
        showConfirmation(
            "Exit Game",
            "Your progress will be saved before closing the game. Continue?",
            () -> menu.exitMenu()
        );
    }

    private void buildNoUserScreen(Table table) {
        Table panel = pvzPanel();
        Image logo = MenuVisualAssets.image("logo");
        if (logo != null) {
            logo.setScaling(Scaling.fit);
            panel.add(logo).size(270f, 80f).center().padBottom(16f).row();
        }
        Label warning = mediumTitle("NO USER IS LOGGED IN");
        warning.setAlignment(Align.center);
        panel.add(warning).width(550f).center().padBottom(14f).row();

        Label description = wrappedLabel(
            "You need to log in before opening the main menu.",
            520f
        );
        description.setAlignment(Align.center);
        panel.add(description).width(520f).center().padBottom(20f).row();
        panel.add(assetTextButton("green_button", "green_button_down",
                "GO TO LOGIN", () -> App.setScreen(new LogInView())))
            .width(240f).height(56f).center();
        table.add(panel).width(680f).center().padTop(70f);
    }
}

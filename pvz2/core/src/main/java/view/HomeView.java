package view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

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
        Image pot = MenuVisualAssets.image("pot");
        if (pot != null) {
            pot.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            hero.add(pot)
                .size(84f)
                .padRight(14f)
                .center();
        }

        Table heroCopy = new Table();
        Label welcome = mediumTitle(
            "WELCOME, " + nickname.toUpperCase() + "!"
        );
        welcome.setAlignment(Align.left);
        heroCopy.add(welcome)
            .left()
            .growX()
            .row();
        Label subtitle = secondaryLabel(
            "Your garden is ready. Choose a destination and keep the lawn growing."
        );
        subtitle.setWrap(true);
        subtitle.setAlignment(Align.left);
        heroCopy.add(subtitle)
            .width(560f)
            .left();
        hero.add(heroCopy)
            .growX()
            .left();

        table.add(hero)
            .width(820f)
            .center()
            .padTop(4f)
            .padBottom(14f)
            .row();

        Table shortcutPanel = pvzInnerPanel();

        shortcutPanel.defaults()
            .width(132f)
            .height(112f)
            .pad(4f);

        shortcutPanel.add(
            menuShortcut(
                "almanac",
                "ADVENTURE",
                () -> openMenu("Adventure menu")
            )
        );

        shortcutPanel.add(
            menuShortcut(
                "almanac",
                "COLLECTION",
                () -> openMenu("Collection menu")
            )
        );

        shortcutPanel.add(
            menuShortcut(
                "hud_quests",
                "TRAVEL LOG",
                () -> openMenu("Travel Log menu")
            )
        );

        shortcutPanel.add(
            menuShortcut(
                "settings",
                "SETTINGS",
                () -> openMenu("Settings menu")
            )
        );

        shortcutPanel.add(
            menuShortcut(
                "hud_zg",
                "GREENHOUSE",
                () -> openMenu("Greenhouse menu")
            )
        );

        table.add(shortcutPanel)
            .width(760f)
            .center()
            .padBottom(12f)
            .row();

        Table mainPanel = pvzPanel();

        TextButton adventure =
            greenButton(
                "ADVENTURE",
                () -> openMenu("Adventure menu")
            );

        TextButton collection =
            greenButton(
                "COLLECTION",
                () -> openMenu("Collection menu")
            );

        int unreadNews = user.getUnreadNews().size();

        String newsText =
            unreadNews > 0
                ? "NEWS  (" + unreadNews + " NEW)"
                : "NEWS";

        TextButton news =
            greenButton(
                newsText,
                () -> openMenu("News menu")
            );

        TextButton profile =
            greenButton(
                "PROFILE",
                () -> openMenu("Profile menu")
            );

        TextButton settings =
            brownButton(
                "SETTINGS",
                () -> openMenu("Settings menu")
            );

        TextButton leaderboard =
            purpleButton(
                "LEADERBOARD",
                () -> openMenu("Leaderboard menu")
            );

        TextButton quests =
            purpleButton(
                "TRAVEL LOG / QUESTS",
                () -> openMenu("Travel Log menu")
            );

        TextButton greenhouse =
            greenButton(
                "GREENHOUSE",
                () -> openMenu("Greenhouse menu")
            );

        TextButton shop =
            greenButton(
                "SHOP",
                () -> openMenu("Shop menu")
            );

        TextButton wallet =
            purpleButton(
                "WALLET",
                () -> openMenu("Wallet menu")
            );

        mainPanel.defaults()
            .width(310f)
            .height(56f)
            .pad(7f, 12f, 7f, 12f);

        mainPanel.add(
                mediumTitle("GARDEN COMMAND CENTER")
            )
            .colspan(2)
            .center()
            .padBottom(7f)
            .row();

        mainPanel.add(adventure);
        mainPanel.add(collection);
        mainPanel.row();

        mainPanel.add(news);
        mainPanel.add(profile);
        mainPanel.row();

        mainPanel.add(settings);
        mainPanel.add(leaderboard);
        mainPanel.row();

        mainPanel.add(quests);
        mainPanel.add(greenhouse);
        mainPanel.row();

        mainPanel.add(shop);
        mainPanel.add(wallet);
        mainPanel.row();

        table.add(mainPanel)
            .width(760f)
            .center()
            .padBottom(12f)
            .row();

        Table infoPanel = pvzInnerPanel();

        Label progress =
            secondaryLabel(
                "CURRENT PROGRESS: "
                    + user.getLastProgressText()
                    + "  |  COMPLETED LEVELS: "
                    + user.getLevelsPassed()
                    + "  |  DIFFICULTY: "
                    + user.getDifficultyLevel()
            );

        progress.setAlignment(Align.center);

        infoPanel.add(progress)
            .width(700f)
            .center()
            .padBottom(5f)
            .row();

        Label debugStatus =
            secondaryLabel(
                "DEBUG MODE: "
                    + (user.isDebugMode() ? "ON" : "OFF")
                    + "  |  RESOURCE CHEATS: "
                    + (user.isDebugMode() ? "AVAILABLE" : "HIDDEN")
            );

        debugStatus.setAlignment(Align.center);

        infoPanel.add(debugStatus)
            .width(700f)
            .center();

        table.add(infoPanel)
            .width(760f)
            .center()
            .padBottom(12f)
            .row();

        Table accountActions = new Table();

        TextButton logout =
            brownButton(
                "LOG OUT",
                this::confirmLogout
            );

        TextButton exitGame =
            brownButton(
                "EXIT GAME",
                this::confirmExit
            );

        accountActions.add(logout)
            .width(230f)
            .height(52f)
            .padRight(12f);

        accountActions.add(exitGame)
            .width(230f)
            .height(52f)
            .padLeft(12f);

        table.add(accountActions)
            .center()
            .padBottom(8f);
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

        Label warning =
            mediumTitle(
                "NO USER IS LOGGED IN"
            );

        warning.setAlignment(Align.center);

        panel.add(warning)
            .width(500f)
            .center()
            .padBottom(20f)
            .row();

        Label description =
            wrappedLabel(
                "You need to log in before opening the main menu.",
                500f
            );

        description.setAlignment(Align.center);

        panel.add(description)
            .width(500f)
            .center()
            .padBottom(22f)
            .row();

        panel.add(
                greenButton(
                    "GO TO LOGIN",
                    () -> App.setScreen(new LogInView())
                )
            )
            .width(230f)
            .height(54f)
            .center();

        table.add(panel)
            .width(650f)
            .center()
            .padTop(80f);
    }
}

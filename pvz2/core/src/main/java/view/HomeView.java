package view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
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
    }

    @Override
    protected String getScreenTitle() {
        return "Main Menu";
    }

    @Override
    protected void buildContent(
        Table table
    ) {

        User user =
            Data.getCurrentUser();

        if (user == null) {

            buildNoUserScreen(
                table
            );

            return;
        }

        /*
         * Welcome text
         */
        Label welcome =
            mediumTitle(
                "WELCOME, "
                    + user.getNickname()
                    .toUpperCase()
                    + "!"
            );

        welcome.setAlignment(
            Align.center
        );

        table.add(welcome)
            .center()
            .padTop(10f)
            .padBottom(18f)
            .row();

        /*
         * Main PvZ-style panel
         */
        Table mainPanel =
            pvzPanel();

        /*
         * Buttons inside the main panel.
         */
        TextButton adventure =
            greenButton(
                "ADVENTURE",
                () ->
                    App.setScreen(
                        new PlayView()
                    )
            );

        TextButton collection =
            greenButton(
                "COLLECTION",
                () ->
                    App.setScreen(
                        new CollectionView()
                    )
            );

        int unread =
            user
                .getUnreadNews()
                .size();

        String newsText =
            unread > 0
                ? "NEWS  (" + unread + " NEW)"
                : "NEWS";

        TextButton news =
            greenButton(
                newsText,
                () ->
                    App.setScreen(
                        new NewsView()
                    )
            );

        TextButton profile =
            greenButton(
                "PROFILE",
                () ->
                    App.setScreen(
                        new ProfileView()
                    )
            );

        TextButton settings =
            brownButton(
                "SETTINGS",
                () ->
                    App.setScreen(
                        new SettingsView()
                    )
            );

        TextButton leaderboard =
            purpleButton(
                "LEADERBOARD",
                () ->
                    App.setScreen(
                        new LeaderBoardView()
                    )
            );

        TextButton quests =
            purpleButton(
                "TRAVEL LOG / QUESTS",
                () ->
                    App.setScreen(
                        new TravelLogView()
                    )
            );

        TextButton greenhouse =
            greenButton(
                "GREENHOUSE",
                () ->
                    App.setScreen(
                        new GreenHouseView()
                    )
            );

        /*
         * Consistent sizing for every button.
         */
        mainPanel.defaults()
            .width(310f)
            .height(62f)
            .pad(
                10f,
                14f,
                10f,
                14f
            );

        /*
         * Row 1
         */
        mainPanel.add(adventure);
        mainPanel.add(collection);
        mainPanel.row();

        /*
         * Row 2
         */
        mainPanel.add(news);
        mainPanel.add(profile);
        mainPanel.row();

        /*
         * Row 3
         */
        mainPanel.add(settings);
        mainPanel.add(leaderboard);
        mainPanel.row();

        /*
         * Row 4
         */
        mainPanel.add(quests);
        mainPanel.add(greenhouse);
        mainPanel.row();

        table.add(mainPanel)
            .width(760f)
            .center()
            .padBottom(18f)
            .row();

        /*
         * Small bottom information panel.
         */
        Table infoPanel =
            pvzInnerPanel();

        Label info =
            secondaryLabel(
                "Choose a menu to continue your adventure."
            );

        info.setAlignment(
            Align.center
        );

        infoPanel.add(info)
            .width(520f)
            .center();

        table.add(infoPanel)
            .width(600f)
            .center()
            .padBottom(18f)
            .row();

        /*
         * Logout button.
         */
        TextButton logout =
            brownButton(
                "LOG OUT",
                this::logout
            );

        table.add(logout)
            .width(240f)
            .height(54f)
            .center()
            .padBottom(10f);
    }

    private void logout() {

        showConfirmation(
            "Log Out",
            "Are you sure you want to log out?",
            () -> {

                ((Home) menu).LogOut();

                App.setScreen(
                    new SignUpView()
                );
            }
        );
    }

    private void buildNoUserScreen(
        Table table
    ) {

        Table panel =
            pvzPanel();

        Label warning =
            mediumTitle(
                "NO USER IS LOGGED IN"
            );

        warning.setAlignment(
            Align.center
        );

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

        description.setAlignment(
            Align.center
        );

        panel.add(description)
            .width(500f)
            .center()
            .padBottom(22f)
            .row();

        panel.add(
                greenButton(
                    "GO TO LOGIN",
                    () ->
                        App.setScreen(
                            new LogInView()
                        )
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

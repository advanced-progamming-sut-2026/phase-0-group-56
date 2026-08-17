package view;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

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
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();
        if (user == null) {
            table.add(new Label("No user is logged in.", skin)).row();
            table.add(button("Go to login", () -> App.setScreen(new LogInView()))).padTop(14f);
            return;
        }

        Label welcome = new Label("Welcome, " + user.getNickname() + "!", skin);
        table.add(welcome).padTop(18f).padBottom(22f).row();

        Table buttons = new Table();
        buttons.defaults().width(260f).height(60f).pad(9f);

        buttons.add(button("Adventure", () -> App.setScreen(new PlayView())));
        buttons.add(button("Collection", () -> App.setScreen(new CollectionView()))).row();

        int unread = user.getUnreadNews().size();
        String newsText = unread > 0 ? "News  (" + unread + " new)" : "News";
        buttons.add(button(newsText, () -> App.setScreen(new NewsView())));
        buttons.add(button("Profile", () -> App.setScreen(new ProfileView()))).row();

        buttons.add(button("Settings", () -> App.setScreen(new SettingsView())));
        buttons.add(button("Leaderboard", () -> App.setScreen(new LeaderBoardView()))).row();

        buttons.add(button("Travel Log / Quests", () -> App.setScreen(new TravelLogView())));
        buttons.add(button("Greenhouse", () -> App.setScreen(new GreenHouseView()))).row();

        table.add(buttons).row();
        table.add(button("Log out", () -> {
            ((Home) menu).LogOut();
            App.setScreen(new SignUpView());
        })).width(220f).height(46f).padTop(22f);
    }
}

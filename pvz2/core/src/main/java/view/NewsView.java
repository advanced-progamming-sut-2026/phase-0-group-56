package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.News;
import models.App;
import models.User;

import java.util.ArrayList;
import java.util.List;

public class NewsView extends View {
    public NewsView() {
        menu = new News();
    }

    @Override
    protected String getScreenTitle() {
        return "News";
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

        int unreadCount = user.getUnreadNews().size();
        table.add(new Label("Unread: " + unreadCount, skin)).padBottom(12f).row();

        List<String> all = new ArrayList<>();
        for (String message : user.getUnreadNews()) {
            all.add("[NEW] " + message);
        }
        for (String message : user.getReadNews()) {
            all.add(message);
        }

        if (all.isEmpty()) {
            table.add(new Label("No news yet.", skin)).pad(20f).row();
        } else {
            for (String message : all) {
                Table card = new Table();
                card.add(wrappedLabel(message, 760f)).width(760f).left().pad(12f);
                table.add(card).width(820f).pad(5f).row();
            }
        }

        if (unreadCount > 0) {
            table.add(button("Mark unread as read", () -> {
                String result = ((News) menu).ShowNews();
                showMessage(result);
                App.setScreen(new NewsView());
            })).width(250f).height(45f).padTop(16f);
        }
    }
}

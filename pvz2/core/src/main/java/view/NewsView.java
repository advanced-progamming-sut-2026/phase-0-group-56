package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

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
            table.add(
                mediumTitle("PLEASE LOG IN")
            );
            return;
        }

        int unreadCount =
            user.getUnreadNews().size();

        Label status =
            mediumTitle(
                unreadCount > 0
                    ? unreadCount + " NEW MESSAGE(S)"
                    : "ALL CAUGHT UP"
            );

        status.setAlignment(Align.center);

        table.add(status)
            .padTop(8f)
            .padBottom(16f)
            .row();

        List<String> all =
            new ArrayList<>();

        for (String message :
            user.getUnreadNews()) {

            all.add("[NEW] " + message);
        }

        for (String message :
            user.getReadNews()) {

            all.add(message);
        }

        if (all.isEmpty()) {

            Table emptyPanel =
                pvzPanel();

            Label empty =
                mediumTitle("NO NEWS YET");

            empty.setAlignment(
                Align.center
            );

            emptyPanel.add(empty)
                .width(500f)
                .center()
                .pad(25f);

            table.add(emptyPanel)
                .width(600f)
                .padTop(30f)
                .row();

            return;
        }

        for (String message : all) {

            Table card =
                pvzInnerPanel();

            Label body =
                wrappedLabel(
                    message,
                    700f
                );

            body.setAlignment(
                Align.left
            );

            card.add(body)
                .width(700f)
                .left();

            table.add(card)
                .width(780f)
                .pad(6f)
                .row();
        }

        if (unreadCount > 0) {

            table.add(
                    greenButton(
                        "MARK ALL AS READ",
                        () -> {

                            String result =
                                ((News) menu)
                                    .ShowNews();

                            showMessage(result);

                            App.setScreen(
                                new NewsView()
                            );
                        }
                    )
                )
                .width(280f)
                .height(56f)
                .padTop(18f);
        }
    }
}

package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.Profile;
import models.User;

public class ProfileView extends View {

    public ProfileView() {
        menu = new Profile();
    }

    @Override
    protected String getScreenTitle() {
        return "Profile";
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

        Label player =
            mediumTitle(
                user.getNickname()
                    .toUpperCase()
            );

        player.setAlignment(
            Align.center
        );

        table.add(player)
            .padTop(5f)
            .padBottom(14f)
            .row();

        Table info =
            pvzPanel();

        addInfo(
            info,
            "USERNAME",
            user.getName()
        );

        addInfo(
            info,
            "NICKNAME",
            user.getNickname()
        );

        addInfo(
            info,
            "EMAIL",
            user.getEmail()
        );

        addInfo(
            info,
            "GAMES PLAYED",
            String.valueOf(
                user.getGamesPlayed()
            )
        );

        addInfo(
            info,
            "COINS",
            String.valueOf(
                user.getCoins()
            )
        );

        addInfo(
            info,
            "GEMS",
            String.valueOf(
                user.getDiamonds()
            )
        );

        addInfo(
            info,
            "LEVELS COMPLETED",
            String.valueOf(
                user.getLevelsPassed()
            )
        );

        addInfo(
            info,
            "HIGHEST MEOWPOINT",
            String.valueOf(
                user.getHighestScore()
            )
        );

        addInfo(
            info,
            "CURRENT PROGRESS",
            user.getLastProgressText()
        );

        table.add(info)
            .width(690f)
            .padBottom(18f)
            .row();

        Table actions =
            new Table();

        actions.defaults()
            .width(270f)
            .height(54f)
            .pad(8f);

        actions.add(
            greenButton(
                "CHANGE USERNAME",
                () ->
                    showSingleEdit(
                        "CHANGE USERNAME",
                        "New username",
                        value ->
                            ((Profile) menu)
                                .changeUserName(
                                    value
                                )
                    )
            )
        );

        actions.add(
                greenButton(
                    "CHANGE NICKNAME",
                    () ->
                        showSingleEdit(
                            "CHANGE NICKNAME",
                            "New nickname",
                            value ->
                                ((Profile) menu)
                                    .changeNickName(
                                        value
                                    )
                        )
                )
            )
            .row();

        actions.add(
            greenButton(
                "CHANGE EMAIL",
                () ->
                    showSingleEdit(
                        "CHANGE EMAIL",
                        "New email",
                        value ->
                            ((Profile) menu)
                                .changeEmail(
                                    value
                                )
                    )
            )
        );

        actions.add(
                purpleButton(
                    "CHANGE PASSWORD",
                    this::showPasswordEditor
                )
            )
            .row();

        table.add(actions);
    }

    private void addInfo(
        Table table,
        String key,
        String value
    ) {

        Label left =
            new Label(
                key,
                skin,
                "medium_outline"
            );

        Label right =
            new Label(
                value == null
                    ? ""
                    : value,
                skin
            );

        table.add(left)
            .width(260f)
            .left()
            .pad(7f);

        table.add(right)
            .width(340f)
            .left()
            .pad(7f)
            .row();
    }

    private interface EditAction {
        String apply(String value);
    }

    private void showSingleEdit(
        String title,
        String placeholder,
        EditAction action
    ) {

        content.clearChildren();

        TextField input =
            field(placeholder);

        Table panel =
            pvzPanel();

        Label heading =
            mediumTitle(title);

        heading.setAlignment(
            Align.center
        );

        panel.add(heading)
            .padBottom(20f)
            .row();

        panel.add(input)
            .width(430f)
            .height(46f)
            .padBottom(22f)
            .row();

        content.add(panel)
            .width(580f)
            .padTop(70f)
            .padBottom(18f)
            .row();

        Table controls =
            new Table();

        controls.add(
                brownButton(
                    "CANCEL",
                    this::reloadProfile
                )
            )
            .width(190f)
            .height(52f)
            .padRight(12f);

        controls.add(
                greenButton(
                    "SAVE",
                    () -> {

                        String result =
                            action.apply(
                                input
                                    .getText()
                                    .trim()
                            );

                        showMessage(result);

                        if (
                            !result.startsWith(
                                "Error:"
                            )
                        ) {

                            refreshResourceLabels();
                        }
                    }
                )
            )
            .width(190f)
            .height(52f);

        content.add(controls);
    }

    private void showPasswordEditor() {

        content.clearChildren();

        TextField oldPassword =
            passwordField(
                "Old password"
            );

        TextField newPassword =
            passwordField(
                "New password"
            );

        TextField confirmPassword =
            passwordField(
                "Confirm new password"
            );

        Table panel =
            pvzPanel();

        Label heading =
            mediumTitle(
                "CHANGE PASSWORD"
            );

        heading.setAlignment(
            Align.center
        );

        panel.add(heading)
            .colspan(2)
            .padBottom(18f)
            .row();

        panel.add(
                new Label(
                    "OLD PASSWORD",
                    skin,
                    "medium_outline"
                )
            )
            .width(200f)
            .left()
            .pad(8f);

        panel.add(oldPassword)
            .width(360f)
            .height(44f)
            .pad(8f)
            .row();

        panel.add(
                new Label(
                    "NEW PASSWORD",
                    skin,
                    "medium_outline"
                )
            )
            .width(200f)
            .left()
            .pad(8f);

        panel.add(newPassword)
            .width(360f)
            .height(44f)
            .pad(8f)
            .row();

        panel.add(
                new Label(
                    "CONFIRM PASSWORD",
                    skin,
                    "medium_outline"
                )
            )
            .width(200f)
            .left()
            .pad(8f);

        panel.add(confirmPassword)
            .width(360f)
            .height(44f)
            .pad(8f)
            .row();

        content.add(panel)
            .width(680f)
            .padTop(60f)
            .padBottom(18f)
            .row();

        Table controls =
            new Table();

        controls.add(
                brownButton(
                    "CANCEL",
                    this::reloadProfile
                )
            )
            .width(190f)
            .height(52f)
            .padRight(12f);

        controls.add(
                greenButton(
                    "SAVE",
                    () -> {

                        String result =
                            ((Profile) menu)
                                .changePassword(
                                    oldPassword
                                        .getText(),
                                    newPassword
                                        .getText(),
                                    confirmPassword
                                        .getText()
                                );

                        showMessage(result);
                    }
                )
            )
            .width(190f)
            .height(52f);

        content.add(controls);
    }

    private void reloadProfile() {

        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }
}

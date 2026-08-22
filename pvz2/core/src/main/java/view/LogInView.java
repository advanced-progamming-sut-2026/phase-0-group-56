package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;

import controllers.menus.LogIn;
import models.App;

public class LogInView extends View {

    public LogInView() {
        menu = new LogIn();
    }

    @Override
    protected String getScreenTitle() {
        return "Log In";
    }

    @Override
    protected Screen getBackScreen() {
        return new SignUpView();
    }

    @Override
    protected void buildContent(Table table) {

        LogIn controller =
            (LogIn) menu;

        table.add(
                menuSectionHeader(
                    "hud_quests",
                    "WELCOME BACK",
                    "Log in to continue your garden adventure."
                )
            )
            .width(760f)
            .padTop(12f)
            .padBottom(18f)
            .row();

        Table panel =
            pvzPanel();

        TextField username =
            field("Username");

        TextField password =
            passwordField("Password");

        CheckBox stayLoggedIn =
            new CheckBox(
                " STAY LOGGED IN",
                skin
            );

        Table form =
            new Table();

        form.add(
                new Label(
                    "USERNAME",
                    skin,
                    "medium_outline"
                )
            )
            .width(190f)
            .left()
            .pad(10f);

        form.add(username)
            .width(400f)
            .height(44f)
            .pad(10f)
            .row();

        form.add(
                new Label(
                    "PASSWORD",
                    skin,
                    "medium_outline"
                )
            )
            .width(190f)
            .left()
            .pad(10f);

        form.add(password)
            .width(400f)
            .height(44f)
            .pad(10f)
            .row();

        form.add()
            .width(190f);

        form.add(stayLoggedIn)
            .left()
            .pad(10f)
            .row();

        panel.add(form)
            .width(650f)
            .row();

        table.add(panel)
            .width(760f)
            .padBottom(20f)
            .row();

        table.add(
                greenButton(
                    "LOG IN",
                    () -> {

                        String result =
                            controller.login(
                                username
                                    .getText()
                                    .trim(),
                                password.getText(),
                                stayLoggedIn
                                    .isChecked()
                            );

                        if (
                            result.startsWith(
                                "Error:"
                            )
                        ) {

                            showMessage(result);
                            return;
                        }

                        App.setScreen(
                            new HomeView()
                        );
                    }
                )
            )
            .width(270f)
            .height(58f)
            .padBottom(10f)
            .row();

        table.add(
                purpleButton(
                    "FORGOT PASSWORD",
                    this::showForgotPasswordPanel
                )
            )
            .width(270f)
            .height(52f)
            .padBottom(10f)
            .row();

        table.add(
                brownButton(
                    "CREATE ACCOUNT",
                    () ->
                        App.setScreen(
                            new SignUpView()
                        )
                )
            )
            .width(270f)
            .height(52f);
    }

    private void showForgotPasswordPanel() {

        content.clearChildren();

        LogIn controller =
            (LogIn) menu;

        Label title =
            mediumTitle(
                "RECOVER ACCOUNT"
            );

        title.setAlignment(
            Align.center
        );

        TextField username =
            field("Username");

        TextField email =
            field("Email");

        Table panel =
            pvzPanel();

        panel.add(title)
            .colspan(2)
            .padBottom(18f)
            .row();

        panel.add(
                new Label(
                    "USERNAME",
                    skin,
                    "medium_outline"
                )
            )
            .width(180f)
            .left()
            .pad(8f);

        panel.add(username)
            .width(380f)
            .height(44f)
            .pad(8f)
            .row();

        panel.add(
                new Label(
                    "EMAIL",
                    skin,
                    "medium_outline"
                )
            )
            .width(180f)
            .left()
            .pad(8f);

        panel.add(email)
            .width(380f)
            .height(44f)
            .pad(8f)
            .row();

        content.add(panel)
            .width(700f)
            .padTop(60f)
            .padBottom(18f)
            .row();

        Table buttons =
            new Table();

        buttons.add(
                brownButton(
                    "CANCEL",
                    () -> {
                        content.clearChildren();
                        buildContent(content);
                    }
                )
            )
            .width(200f)
            .height(52f)
            .padRight(10f);

        buttons.add(
                greenButton(
                    "CONTINUE",
                    () -> {

                        String question =
                            controller
                                .getSecurityQuestion(
                                    username
                                        .getText()
                                        .trim(),
                                    email
                                        .getText()
                                        .trim()
                                );

                        if (
                            question.startsWith(
                                "Error:"
                            )
                        ) {

                            showMessage(
                                question
                            );

                            return;
                        }

                        showResetPasswordPanel(
                            username
                                .getText()
                                .trim(),
                            email
                                .getText()
                                .trim(),
                            question
                        );
                    }
                )
            )
            .width(200f)
            .height(52f);

        content.add(buttons);
    }

    private void showResetPasswordPanel(
        String username,
        String email,
        String question
    ) {

        content.clearChildren();

        LogIn controller =
            (LogIn) menu;

        TextField answer =
            field(
                "Security answer"
            );

        TextField newPassword =
            passwordField(
                "New password"
            );

        TextField confirm =
            passwordField(
                "Confirm new password"
            );

        Table panel =
            pvzPanel();

        Label title =
            mediumTitle(
                "RESET PASSWORD"
            );

        title.setAlignment(
            Align.center
        );

        panel.add(title)
            .colspan(2)
            .padBottom(16f)
            .row();

        Label questionLabel =
            wrappedLabel(
                question,
                520f
            );

        questionLabel.setAlignment(
            Align.center
        );

        panel.add(questionLabel)
            .colspan(2)
            .width(520f)
            .padBottom(18f)
            .row();

        addRecoveryRow(
            panel,
            "ANSWER",
            answer
        );

        addRecoveryRow(
            panel,
            "NEW PASSWORD",
            newPassword
        );

        addRecoveryRow(
            panel,
            "CONFIRM",
            confirm
        );

        content.add(panel)
            .width(720f)
            .padTop(35f)
            .padBottom(18f)
            .row();

        Table buttons =
            new Table();

        buttons.add(
                brownButton(
                    "CANCEL",
                    () -> {
                        content.clearChildren();
                        buildContent(content);
                    }
                )
            )
            .width(200f)
            .height(52f)
            .padRight(10f);

        buttons.add(
                greenButton(
                    "RESET",
                    () -> {

                        String result =
                            controller
                                .resetPassword(
                                    username,
                                    email,
                                    answer
                                        .getText()
                                        .trim(),
                                    newPassword
                                        .getText(),
                                    confirm
                                        .getText()
                                );

                        showMessage(result);

                        if (
                            !result.startsWith(
                                "Error:"
                            )
                        ) {

                            content.clearChildren();
                            buildContent(content);
                        }
                    }
                )
            )
            .width(200f)
            .height(52f);

        content.add(buttons);
    }

    private void addRecoveryRow(
        Table table,
        String label,
        TextField field
    ) {

        table.add(
                new Label(
                    label,
                    skin,
                    "medium_outline"
                )
            )
            .width(190f)
            .left()
            .pad(8f);

        table.add(field)
            .width(370f)
            .height(44f)
            .pad(8f)
            .row();
    }
}

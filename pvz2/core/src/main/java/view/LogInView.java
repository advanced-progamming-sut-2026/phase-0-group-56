package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

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
        LogIn controller = (LogIn) menu;
        TextField username = field("Username");
        TextField password = passwordField("Password");
        CheckBox stayLoggedIn = new CheckBox(" Stay logged in", skin);

        Table form = new Table();
        form.add(new Label("Username", skin)).width(170f).left().pad(8f);
        form.add(username).width(430f).height(44f).pad(8f).row();
        form.add(new Label("Password", skin)).width(170f).left().pad(8f);
        form.add(password).width(430f).height(44f).pad(8f).row();
        form.add().width(170f);
        form.add(stayLoggedIn).left().pad(8f).row();

        table.add(form).width(720f).padTop(30f).padBottom(20f).row();

        table.add(button("Log in", () -> {
            String result = controller.login(
                username.getText().trim(),
                password.getText(),
                stayLoggedIn.isChecked());
            if (result.startsWith("Error:")) {
                showMessage(result);
            }
        })).width(240f).height(48f).padBottom(10f).row();

        table.add(button("Forgot password", this::showForgotPasswordDialog))
            .width(240f).height(44f).padBottom(10f).row();
        table.add(button("Create account", () -> App.setScreen(new SignUpView())))
            .width(240f).height(44f);
    }

    private void showForgotPasswordDialog() {
        LogIn controller = (LogIn) menu;
        TextField username = field("Username");
        TextField email = field("Email");

        Dialog first = new Dialog("Recover account", skin) {
            @Override
            protected void result(Object object) {
                if (!Boolean.TRUE.equals(object)) {
                    return;
                }
                String question = controller.getSecurityQuestion(
                    username.getText().trim(), email.getText().trim());
                if (question.startsWith("Error:")) {
                    showMessage(question);
                    return;
                }
                showResetDialog(username.getText().trim(), email.getText().trim(), question);
            }
        };
        first.getContentTable().add(new Label("Username", skin)).pad(8f);
        first.getContentTable().add(username).width(340f).pad(8f).row();
        first.getContentTable().add(new Label("Email", skin)).pad(8f);
        first.getContentTable().add(email).width(340f).pad(8f).row();
        first.button("Cancel", false);
        first.button("Continue", true);
        first.show(stage);
    }

    private void showResetDialog(String username, String email, String question) {
        LogIn controller = (LogIn) menu;
        TextField answer = field("Security answer");
        TextField newPassword = passwordField("New password");
        TextField confirm = passwordField("Confirm new password");

        Dialog dialog = new Dialog("Reset password", skin) {
            @Override
            protected void result(Object object) {
                if (!Boolean.TRUE.equals(object)) {
                    return;
                }
                String result = controller.resetPassword(
                    username,
                    email,
                    answer.getText().trim(),
                    newPassword.getText(),
                    confirm.getText());
                showMessage(result);
            }
        };
        Table contentTable = dialog.getContentTable();
        contentTable.add(wrappedLabel(question, 460f)).width(460f).colspan(2).pad(10f).row();
        contentTable.add(new Label("Answer", skin)).pad(8f);
        contentTable.add(answer).width(340f).pad(8f).row();
        contentTable.add(new Label("New password", skin)).pad(8f);
        contentTable.add(newPassword).width(340f).pad(8f).row();
        contentTable.add(new Label("Confirm", skin)).pad(8f);
        contentTable.add(confirm).width(340f).pad(8f).row();
        dialog.button("Cancel", false);
        dialog.button("Reset", true);
        dialog.show(stage);
    }
}

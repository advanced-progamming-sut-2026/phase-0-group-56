package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

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
    protected void buildContent(Table table) {
        User user = Data.getCurrentUser();
        if (user == null) {
            table.add(new Label("Please log in.", skin));
            return;
        }

        Table info = new Table();
        addInfo(info, "Username", user.getName());
        addInfo(info, "Nickname", user.getNickname());
        addInfo(info, "Email", user.getEmail());
        addInfo(info, "Games played", String.valueOf(user.getGamesPlayed()));
        addInfo(info, "Coins", String.valueOf(user.getCoins()));
        addInfo(info, "Gems", String.valueOf(user.getDiamonds()));
        addInfo(info, "Levels completed", String.valueOf(user.getLevelsPassed()));
        addInfo(info, "Highest MeowPoint", String.valueOf(user.getHighestScore()));
        addInfo(info, "Current progress", user.getLastProgressText());

        table.add(info).width(620f).padTop(16f).padBottom(20f).row();

        Table actions = new Table();
        actions.defaults().width(245f).height(48f).pad(7f);
        actions.add(button("Change username", () -> showSingleEdit(
            "Change username", "New username", value -> ((Profile) menu).changeUserName(value))));
        actions.add(button("Change nickname", () -> showSingleEdit(
            "Change nickname", "New nickname", value -> ((Profile) menu).changeNickName(value)))).row();
        actions.add(button("Change email", () -> showSingleEdit(
            "Change email", "New email", value -> ((Profile) menu).changeEmail(value))));
        actions.add(button("Change password", this::showPasswordDialog)).row();
        table.add(actions);
    }

    private void addInfo(Table table, String key, String value) {
        table.add(new Label(key + ":", skin)).width(210f).left().pad(6f);
        table.add(new Label(value == null ? "" : value, skin)).width(350f).left().pad(6f).row();
    }

    private interface EditAction {
        String apply(String value);
    }

    private void showSingleEdit(String title, String placeholder, EditAction action) {
        TextField input = field(placeholder);
        Dialog dialog = new Dialog(title, skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    String result = action.apply(input.getText().trim());
                    showMessage(result);
                    if (!result.startsWith("Error:")) {
                        content.clearChildren();
                        buildContent(content);
                        refreshResourceLabels();
                    }
                }
            }
        };
        dialog.getContentTable().add(input).width(420f).pad(18f);
        dialog.button("Cancel", false);
        dialog.button("Save", true);
        dialog.show(stage);
    }

    private void showPasswordDialog() {
        TextField oldPassword = passwordField("Old password");
        TextField newPassword = passwordField("New password");

        Dialog dialog = new Dialog("Change password", skin) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    String result = ((Profile) menu).changePassword(
                        oldPassword.getText(), newPassword.getText());
                    showMessage(result);
                }
            }
        };
        Table contentTable = dialog.getContentTable();
        contentTable.add(new Label("Old password", skin)).pad(8f);
        contentTable.add(oldPassword).width(340f).pad(8f).row();
        contentTable.add(new Label("New password", skin)).pad(8f);
        contentTable.add(newPassword).width(340f).pad(8f).row();
        dialog.button("Cancel", false);
        dialog.button("Save", true);
        dialog.show(stage);
    }
}

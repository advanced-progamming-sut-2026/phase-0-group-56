package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;

import controllers.menus.SignUp;
import models.App;

public class SignUpView extends View {
    public SignUpView() {
        menu = new SignUp();
    }

    @Override
    protected String getScreenTitle() {
        return "Create Account";
    }

    @Override
    protected void buildContent(Table table) {
        SignUp controller = (SignUp) menu;

        TextField username = field("Username");
        TextField password = passwordField("Password");
        TextField confirm = passwordField("Confirm password");
        TextField nickname = field("Nickname");
        TextField email = field("Email");
        SelectBox<String> gender = new SelectBox<>(skin);
        gender.setItems(new Array<>(new String[]{"Male", "Female"}));

        SelectBox<String> question = new SelectBox<>(skin);
        question.setItems(new Array<>(SignUp.SECURITY_QUESTIONS));
        TextField answer = field("Security answer");
        TextField answerConfirm = field("Confirm security answer");

        Table form = new Table();
        addRow(form, "Username", username);
        addRow(form, "Password", password);
        addRow(form, "Confirm password", confirm);
        addRow(form, "Nickname", nickname);
        addRow(form, "Email", email);
        addRow(form, "Gender", gender);
        addRow(form, "Security question", question);
        addRow(form, "Answer", answer);
        addRow(form, "Confirm answer", answerConfirm);

        table.add(wrappedLabel(
                "Create an account. All validation errors are shown before any data is saved.", 620f))
            .width(620f).padBottom(12f).row();
        table.add(form).width(760f).padBottom(16f).row();

        table.add(button("Create account", () -> {
            String result = controller.register(
                username.getText().trim(),
                password.getText(),
                confirm.getText(),
                nickname.getText().trim(),
                email.getText().trim(),
                gender.getSelected());

            if (result.startsWith("Error:")) {
                showMessage(result);
                return;
            }

            int questionNumber = question.getSelectedIndex() + 1;
            result = controller.pickQuestion(
                questionNumber,
                answer.getText().trim(),
                answerConfirm.getText().trim());
            showMessage(result);
            if (!result.startsWith("Error:")) {
                App.setScreen(new LogInView());
            }
        })).width(260f).height(48f).padBottom(10f).row();

        table.add(button("I already have an account", () -> App.setScreen(new LogInView())))
            .width(300f).height(44f);
    }

    private void addRow(Table table, String label, com.badlogic.gdx.scenes.scene2d.Actor actor) {
        table.add(new Label(label, skin)).width(180f).left().pad(6f);
        table.add(actor).width(470f).height(42f).pad(6f).row();
    }

    @Override
    protected Screen getBackScreen() {
        return null;
    }
}

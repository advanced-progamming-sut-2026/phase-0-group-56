package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
    protected Screen getBackScreen() {
        return null;
    }

    @Override
    protected void buildContent(Table table) {

        SignUp controller = (SignUp) menu;

        table.add(
                menuSectionHeader(
                    "almanac",
                    "JOIN THE ADVENTURE",
                    "Create your account and save your garden progress."
                )
            )
            .width(840f)
            .padTop(4f)
            .padBottom(14f)
            .row();

        Table panel = pvzPanel();

        TextField username = field("Username");
        TextField password = passwordField("Password");
        TextField confirm = passwordField("Confirm password");
        TextField nickname = field("Nickname");
        TextField email = field("Email");

        SelectBox<String> gender = new SelectBox<>(skin);
        gender.setItems(
            new Array<>(
                new String[]{
                    "Male",
                    "Female"
                }
            )
        );

        SelectBox<String> question = new SelectBox<>(skin);
        question.setItems(
            new Array<>(
                SignUp.SECURITY_QUESTIONS
            )
        );

        TextField answer = field("Security answer");
        TextField answerConfirm = field("Confirm security answer");

        Table form = new Table();

        addRow(form, "USERNAME", username);
        addRow(form, "PASSWORD", password);
        addRow(form, "CONFIRM", confirm);
        addRow(form, "NICKNAME", nickname);
        addRow(form, "EMAIL", email);
        addRow(form, "GENDER", gender);
        addRow(form, "SECURITY QUESTION", question);
        addRow(form, "ANSWER", answer);
        addRow(form, "CONFIRM ANSWER", answerConfirm);

        panel.add(form)
            .width(760f)
            .row();

        table.add(panel)
            .width(840f)
            .padBottom(16f)
            .row();

        table.add(
                greenButton(
                    "CREATE ACCOUNT",
                    () -> {

                        String result =
                            controller.register(
                                username.getText().trim(),
                                password.getText(),
                                confirm.getText(),
                                nickname.getText().trim(),
                                email.getText().trim(),
                                gender.getSelected()
                            );

                        if (result.startsWith("Error:")) {
                            showMessage(result);
                            return;
                        }

                        int questionNumber =
                            question.getSelectedIndex() + 1;

                        result =
                            controller.pickQuestion(
                                questionNumber,
                                answer.getText().trim(),
                                answerConfirm.getText().trim()
                            );

                        if (result.startsWith("Error:")) {
                            showMessage(result);
                            return;
                        }

                        App.setScreen(
                            new LogInView()
                        );
                    }
                )
            )
            .width(300f)
            .height(58f)
            .padBottom(10f)
            .row();

        table.add(
                brownButton(
                    "I ALREADY HAVE AN ACCOUNT",
                    () ->
                        App.setScreen(
                            new LogInView()
                        )
                )
            )
            .width(350f)
            .height(52f);
    }

    private void addRow(
        Table table,
        String labelText,
        Actor actor
    ) {

        Label label =
            new Label(
                labelText,
                skin,
                "medium_outline"
            );

        table.add(label)
            .width(235f)
            .left()
            .pad(6f);

        table.add(actor)
            .width(460f)
            .height(42f)
            .pad(6f)
            .row();
    }
}

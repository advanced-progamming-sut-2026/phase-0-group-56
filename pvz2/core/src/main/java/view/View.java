package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import controllers.datacontroller.Data;
import controllers.menus.Menu;
import models.App;
import models.User;
import pvz.skin.PvzSkin;


public class View implements Screen {
    protected static final float VIRTUAL_WIDTH = 1280f;
    protected static final float VIRTUAL_HEIGHT = 720f;

    protected Menu menu;
    public static java.util.Scanner scanner = App.getInput();
    protected String input;
    protected Stage stage;
    protected Skin skin;
    protected Table root;
    protected Table content;

    private Label coinLabel;
    private Label diamondLabel;

    public void input() {
        if (scanner != null && scanner.hasNextLine()) {
            input = scanner.nextLine().trim();
        }
    }

    protected String getScreenTitle() {
        return getClass().getSimpleName().replace("View", "");
    }

    protected void buildContent(Table table) {
        // Subclasses override.
    }

    protected Screen getBackScreen() {
        return null;
    }

    @Override
    public void show() {
        skin = PvzSkin.get();
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        root.pad(18f);
        stage.addActor(root);

        buildHeader();
        content = new Table();
        content.top().pad(12f);

        ScrollPane scrollPane = new ScrollPane(content, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        root.add(scrollPane).grow().padTop(10f);

        buildContent(content);
        refreshResourceLabels();
    }

    private void buildHeader() {
        Table header = new Table();

        Screen backScreen = getBackScreen();
        if (backScreen != null) {
            TextButton back = new TextButton("< Back", skin);
            back.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    App.setScreen(getBackScreen());
                }
            });
            header.add(back).width(120f).height(42f).left();
        } else {
            header.add().width(120f);
        }

        Label title = new Label(getScreenTitle(), skin, "big");
        title.setAlignment(Align.center);
        header.add(title).expandX().center();

        Table resources = buildResourceBar();
        header.add(resources).right();

        root.add(header).growX().height(58f).row();
    }

    private Table buildResourceBar() {
        Table bar = new Table();
        User user = Data.getCurrentUser();
        if (user == null) {
            return bar;
        }

        coinLabel = new Label("Coins: " + user.getCoins(), skin);
        diamondLabel = new Label("Gems: " + user.getDiamonds(), skin);
        bar.add(coinLabel).padRight(16f);
        bar.add(diamondLabel).padRight(8f);

        if (user.isDebugMode()) {
            TextButton addCoins = new TextButton("+Coin", skin);
            TextButton addGems = new TextButton("+Gem", skin);
            addCoins.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    User current = Data.getCurrentUser();
                    if (current != null) {
                        current.addCoins(1000);
                        Data.saveUser();
                        refreshResourceLabels();
                    }
                }
            });
            addGems.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    User current = Data.getCurrentUser();
                    if (current != null) {
                        current.addDiamonds(10);
                        Data.saveUser();
                        refreshResourceLabels();
                    }
                }
            });
            bar.add(addCoins).width(78f).height(36f).padLeft(4f);
            bar.add(addGems).width(78f).height(36f).padLeft(4f);
        }
        return bar;
    }

    protected void refreshResourceLabels() {
        User user = Data.getCurrentUser();
        if (user == null) {
            return;
        }
        if (coinLabel != null) {
            coinLabel.setText("Coins: " + user.getCoins());
        }
        if (diamondLabel != null) {
            diamondLabel.setText("Gems: " + user.getDiamonds());
        }
    }

    protected TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    protected TextField field(String placeholder) {
        TextField field = new TextField("", skin);
        field.setMessageText(placeholder);
        return field;
    }

    protected TextField passwordField(String placeholder) {
        TextField field = field(placeholder);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    protected Label wrappedLabel(String text, float width) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        label.setAlignment(Align.left);
        label.setWidth(width);
        return label;
    }

    protected void showMessage(String message) {
        final Table overlay = new Table();
        overlay.setFillParent(true);

        final Table box = new Table(skin);
        box.pad(30f);

        Label titleLabel = new Label("Message", skin, "big");
        Label messageLabel = wrappedLabel(message == null ? "" : message, 520f);

        TextButton okButton = new TextButton("OK", skin);

        box.add(titleLabel).padBottom(20f).row();
        box.add(messageLabel).width(520f).padBottom(25f).row();
        box.add(okButton).width(160f).height(48f);

        overlay.add(box).width(620f);

        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });

        stage.addActor(overlay);
    }

    protected void showConfirmation(String title, String message, Runnable onConfirm) {
        final Table overlay = new Table();
        overlay.setFillParent(true);

        final Table box = new Table(skin);
        box.pad(30f);

        Label titleLabel = new Label(title == null ? "Confirm" : title, skin, "big");
        Label messageLabel = wrappedLabel(message == null ? "" : message, 500f);

        TextButton cancelButton = new TextButton("Cancel", skin);
        TextButton confirmButton = new TextButton("Confirm", skin);

        box.add(titleLabel).colspan(2).padBottom(20f).row();
        box.add(messageLabel).colspan(2).width(500f).padBottom(25f).row();

        box.add(cancelButton)
            .width(160f)
            .height(48f)
            .padRight(10f);

        box.add(confirmButton)
            .width(160f)
            .height(48f);

        overlay.add(box).width(620f);

        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });

        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();

                if (onConfirm != null) {
                    onConfirm.run();
                }
            }
        });

        stage.addActor(overlay);
    }

    protected void reload(Screen screen) {
        App.setScreen(screen);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (stage != null) {
            stage.act(Math.min(delta, 1f / 30f));
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {
        Data.saveUser();
    }

    @Override
    public void resume() {
        // No-op.
    }

    @Override
    public void hide() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
    protected boolean handleGlobalCommands(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }

        java.util.regex.Matcher enterMatcher =
            java.util.regex.Pattern
                .compile("(?i)^menu\\s+enter\\s+(?<menuName>.+)$")
                .matcher(command);

        if (command.matches("(?i)^menu\\s+show\\s+current$")) {
            if (menu != null) {
                System.out.println(menu.ShowCurrentMenu());
            }
            return true;
        }

        if (command.matches("(?i)^menu\\s+exit$")) {
            if (menu != null) {
                System.out.println(menu.exitMenu());
            }
            return true;
        }

        if (enterMatcher.matches()) {
            if (menu == null) {
                return true;
            }

            String targetMenu = enterMatcher.group("menuName").trim();

            targetMenu =
                targetMenu.substring(0, 1).toUpperCase()
                    + targetMenu.substring(1).toLowerCase();

            if (!targetMenu.endsWith(" menu")) {
                targetMenu += " menu";
            }

            System.out.println(menu.ChangeMenu(targetMenu));
            return true;
        }

        return false;
    }
}

package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
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

    /*
     * Legacy terminal views still depend on these two fields.
     * Do not remove them until all old terminal views are deleted.
     */
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
        return getClass()
            .getSimpleName()
            .replace("View", "");
    }

    /*
     * Each child View builds its own content here.
     */
    protected void buildContent(Table table) {
    }

    /*
     * Child screens may override this to enable the Back button.
     */
    protected Screen getBackScreen() {
        return null;
    }

    @Override
    public void show() {
        skin = PvzSkin.get();

        stage = new Stage(
            new FitViewport(
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT
            )
        );

        Gdx.input.setInputProcessor(stage);

        root = new Table();
        root.setFillParent(true);
        root.top();
        root.pad(
            12f,
            18f,
            14f,
            18f
        );

        Drawable screenBackground =
            getSkinDrawableSafe(
                "image_ui_quests_panel_edge_to_edge_ten"
            );

        if (screenBackground != null) {
            root.setBackground(screenBackground);
        }

        stage.addActor(root);

        buildHeader();

        content = new Table();
        content.top();
        content.pad(18f);

        ScrollPane scrollPane =
            new ScrollPane(content, skin);

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(
            true,
            false
        );

        root.add(scrollPane)
            .grow()
            .padTop(12f);

        buildContent(content);

        refreshResourceLabels();
    }

    /*
     * ============================================================
     * HEADER
     * ============================================================
     */

    private void buildHeader() {

        Table header = new Table();

        header.pad(7f, 10f, 7f, 10f);

        Drawable headerBackground =
            getSkinDrawableSafe(
                "image_ui_mainmenu_mm_settings_tab_10"
            );

        if (headerBackground != null) {
            header.setBackground(headerBackground);
        }

        Screen backScreen = getBackScreen();

        if (backScreen != null) {

            TextButton back =
                new TextButton(
                    "BACK",
                    skin,
                    "brown"
                );

            back.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        InputEvent event,
                        float x,
                        float y
                    ) {
                        App.setScreen(
                            getBackScreen()
                        );
                    }
                }
            );

            header.add(back)
                .width(135f)
                .height(52f)
                .left();

        } else {

            header.add()
                .width(135f);
        }

        Label title =
            new Label(
                getScreenTitle()
                    .toUpperCase(),
                skin,
                "big_outline"
            );

        title.setAlignment(
            Align.center
        );

        header.add(title)
            .expandX()
            .center();

        Table resourceBar =
            buildResourceBar();

        header.add(resourceBar)
            .right();

        root.add(header)
            .growX()
            .minHeight(74f)
            .row();
    }

    /*
     * ============================================================
     * RESOURCE BAR
     * ============================================================
     */

    private Table buildResourceBar() {

        Table bar = new Table();

        bar.pad(5f, 8f, 5f, 8f);

        Drawable resourceBackground =
            getSkinDrawableSafe(
                "image_ui_dialog_asset_inner_bkgd_10"
            );

        if (resourceBackground != null) {
            bar.setBackground(resourceBackground);
        }

        User user = Data.getCurrentUser();

        if (user == null) {
            return bar;
        }

        coinLabel =
            new Label(
                "COINS: " + user.getCoins(),
                skin,
                "medium_outline"
            );

        diamondLabel =
            new Label(
                "GEMS: " + user.getDiamonds(),
                skin,
                "medium_outline"
            );

        bar.add(coinLabel)
            .padRight(12f);

        bar.add(diamondLabel);

        /*
         * Debug resource buttons.
         * They only appear when debug mode is enabled.
         */
        if (user.isDebugMode()) {

            TextButton addCoins =
                new TextButton(
                    "+ COINS",
                    skin,
                    "green_small"
                );

            TextButton addGems =
                new TextButton(
                    "+ GEMS",
                    skin,
                    "green_small"
                );

            addCoins.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        InputEvent event,
                        float x,
                        float y
                    ) {

                        User current =
                            Data.getCurrentUser();

                        if (current == null) {
                            return;
                        }

                        current.addCoins(1000);

                        Data.saveUser();

                        refreshResourceLabels();
                    }
                }
            );

            addGems.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        InputEvent event,
                        float x,
                        float y
                    ) {

                        User current =
                            Data.getCurrentUser();

                        if (current == null) {
                            return;
                        }

                        current.addDiamonds(10);

                        Data.saveUser();

                        refreshResourceLabels();
                    }
                }
            );

            bar.row();

            bar.add(addCoins)
                .width(105f)
                .height(38f)
                .padTop(4f);

            bar.add(addGems)
                .width(105f)
                .height(38f)
                .padTop(4f);
        }

        return bar;
    }

    protected void refreshResourceLabels() {

        User user =
            Data.getCurrentUser();

        if (user == null) {
            return;
        }

        if (coinLabel != null) {

            coinLabel.setText(
                "COINS: "
                    + user.getCoins()
            );
        }

        if (diamondLabel != null) {

            diamondLabel.setText(
                "GEMS: "
                    + user.getDiamonds()
            );
        }
    }

    /*
     * ============================================================
     * PVZ UI HELPERS
     * ============================================================
     */

    protected TextButton button(
        String text,
        Runnable action
    ) {

        return greenButton(
            text,
            action
        );
    }

    protected TextButton greenButton(
        String text,
        Runnable action
    ) {

        return styledButton(
            text,
            "green",
            action
        );
    }

    protected TextButton greenSmallButton(
        String text,
        Runnable action
    ) {

        return styledButton(
            text,
            "green_small",
            action
        );
    }

    protected TextButton brownButton(
        String text,
        Runnable action
    ) {

        return styledButton(
            text,
            "brown",
            action
        );
    }

    protected TextButton purpleButton(
        String text,
        Runnable action
    ) {

        return styledButton(
            text,
            "purple",
            action
        );
    }

    /**
     * Creates a compact visual shortcut using one of the official
     * pvz-skin ImageButton styles. The action is deliberately attached
     * only to the icon so the surrounding menu layout remains unchanged.
     */
    protected Table menuShortcut(
        String iconStyle,
        String caption,
        Runnable action
    ) {
        Table shortcut = pvzInnerPanel();

        ImageButton icon;

        try {
            icon = new ImageButton(skin, iconStyle);
        } catch (Exception exception) {
            icon = new ImageButton(skin);
        }

        icon.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    if (action != null) {
                        action.run();
                    }
                }
            }
        );

        shortcut.add(icon)
            .size(62f)
            .center()
            .padBottom(4f)
            .row();

        Label label = secondaryLabel(caption);
        label.setAlignment(Align.center);

        shortcut.add(label)
            .width(116f)
            .center();

        return shortcut;
    }

    /**
     * Builds a non-interactive section banner for menu screens. It uses only
     * official skin controls and keeps the screen-specific content untouched.
     */
    protected Table menuSectionHeader(
        String iconStyle,
        String titleText,
        String subtitleText
    ) {
        Table banner = pvzInnerPanel();

        ImageButton icon;

        try {
            icon = new ImageButton(skin, iconStyle);
        } catch (Exception exception) {
            icon = new ImageButton(skin);
        }

        icon.setTouchable(Touchable.disabled);

        Table text = new Table();

        Label title = mediumTitle(titleText);
        title.setAlignment(Align.left);

        Label subtitle = secondaryLabel(subtitleText);
        subtitle.setWrap(true);
        subtitle.setAlignment(Align.left);

        text.add(title)
            .left()
            .growX()
            .row();

        text.add(subtitle)
            .left()
            .growX();

        banner.add(icon)
            .size(62f)
            .padRight(12f);

        banner.add(text)
            .growX()
            .left();

        return banner;
    }

    private TextButton styledButton(
        String text,
        String style,
        Runnable action
    ) {

        TextButton button;

        try {

            button =
                new TextButton(
                    text,
                    skin,
                    style
                );

        } catch (Exception exception) {

            /*
             * Safe fallback if a skin version
             * does not contain a requested style.
             */
            button =
                new TextButton(
                    text,
                    skin
                );
        }

        button.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {

                    if (action != null) {
                        action.run();
                    }
                }
            }
        );

        return button;
    }

    protected Label titleLabel(
        String text
    ) {

        try {

            return new Label(
                text,
                skin,
                "big_outline"
            );

        } catch (Exception exception) {

            return new Label(
                text,
                skin
            );
        }
    }

    protected Label mediumTitle(
        String text
    ) {

        try {

            return new Label(
                text,
                skin,
                "medium_outline"
            );

        } catch (Exception exception) {

            return new Label(
                text,
                skin
            );
        }
    }

    protected Label secondaryLabel(
        String text
    ) {

        try {

            return new Label(
                text,
                skin,
                "secondary"
            );

        } catch (Exception exception) {

            return new Label(
                text,
                skin
            );
        }
    }

    /*
     * Main reusable PvZ panel.
     */
    protected Table pvzPanel() {

        Table panel = new Table();

        panel.pad(
            24f,
            30f,
            28f,
            30f
        );

        Drawable background =
            getSkinDrawableSafe(
                "image_ui_quests_panel_edge_to_edge_ten"
            );

        if (background != null) {
            panel.setBackground(background);
        }

        return panel;
    }

    /*
     * Smaller panel suitable for settings,
     * profile information, shop cards, etc.
     */
    protected Table pvzInnerPanel() {

        Table panel = new Table();

        panel.pad(18f);

        Drawable background =
            getSkinDrawableSafe(
                "image_ui_dialog_asset_inner_bkgd_10"
            );

        if (background != null) {
            panel.setBackground(background);
        }

        return panel;
    }

    protected Drawable getSkinDrawableSafe(
        String name
    ) {

        if (skin == null || name == null) {
            return null;
        }

        try {
            return skin.getDrawable(name);
        } catch (Exception exception) {
            return null;
        }
    }

    /*
     * ============================================================
     * INPUT HELPERS
     * ============================================================
     */

    protected TextField field(
        String placeholder
    ) {

        TextField field =
            new TextField(
                "",
                skin
            );

        field.setMessageText(
            placeholder
        );

        return field;
    }

    protected TextField passwordField(
        String placeholder
    ) {

        TextField field =
            field(placeholder);

        field.setPasswordMode(true);
        field.setPasswordCharacter('*');

        return field;
    }

    protected Label wrappedLabel(
        String text,
        float width
    ) {

        Label label =
            new Label(
                text == null ? "" : text,
                skin
            );

        label.setWrap(true);
        label.setAlignment(Align.left);
        label.setWidth(width);

        return label;
    }

    /*
     * ============================================================
     * MESSAGE OVERLAY
     * ============================================================
     *
     * We intentionally do NOT use Scene2D Dialog here.
     *
     * pvz-skin currently does not provide the default WindowStyle
     * that LibGDX Dialog expects, so using Dialog causes:
     *
     * No Window$WindowStyle registered with name: default
     */

    protected void showMessage(
        String message
    ) {

        final Table overlay =
            new Table();

        overlay.setFillParent(true);
        overlay.setTouchable(
            Touchable.enabled
        );

        final Table box =
            pvzPanel();

        Label title =
            mediumTitle("MESSAGE");

        title.setAlignment(
            Align.center
        );

        Label body =
            wrappedLabel(
                message == null
                    ? ""
                    : message,
                500f
            );

        body.setAlignment(
            Align.center
        );

        TextButton ok =
            greenButton(
                "OK",
                overlay::remove
            );

        box.add(title)
            .center()
            .padBottom(18f)
            .row();

        box.add(body)
            .width(500f)
            .center()
            .padBottom(22f)
            .row();

        box.add(ok)
            .width(180f)
            .height(52f)
            .center();

        overlay.add(box)
            .width(600f);

        stage.addActor(overlay);
    }

    /*
     * ============================================================
     * CONFIRMATION OVERLAY
     * ============================================================
     */

    protected void showConfirmation(
        String title,
        String message,
        Runnable onConfirm
    ) {

        final Table overlay =
            new Table();

        overlay.setFillParent(true);
        overlay.setTouchable(
            Touchable.enabled
        );

        final Table box =
            pvzPanel();

        Label titleLabel =
            mediumTitle(
                title == null
                    ? "CONFIRM"
                    : title.toUpperCase()
            );

        titleLabel.setAlignment(
            Align.center
        );

        Label messageLabel =
            wrappedLabel(
                message == null
                    ? ""
                    : message,
                500f
            );

        messageLabel.setAlignment(
            Align.center
        );

        TextButton cancel =
            brownButton(
                "CANCEL",
                overlay::remove
            );

        TextButton confirm =
            greenButton(
                "CONFIRM",
                () -> {

                    overlay.remove();

                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                }
            );

        box.add(titleLabel)
            .colspan(2)
            .center()
            .padBottom(18f)
            .row();

        box.add(messageLabel)
            .colspan(2)
            .width(500f)
            .center()
            .padBottom(24f)
            .row();

        box.add(cancel)
            .width(180f)
            .height(52f)
            .padRight(12f);

        box.add(confirm)
            .width(180f)
            .height(52f);

        overlay.add(box)
            .width(620f);

        stage.addActor(overlay);
    }

    /*
     * ============================================================
     * SCREEN RELOAD
     * ============================================================
     */

    protected void reload(
        Screen screen
    ) {

        App.setScreen(screen);
    }

    /*
     * ============================================================
     * RENDER
     * ============================================================
     */

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(
            0.025f,
            0.09f,
            0.055f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        if (stage != null) {

            stage.act(
                Math.min(
                    delta,
                    1f / 30f
                )
            );

            stage.draw();
        }
    }

    @Override
    public void resize(
        int width,
        int height
    ) {

        if (stage != null) {

            stage.getViewport()
                .update(
                    width,
                    height,
                    true
                );
        }
    }

    @Override
    public void pause() {
        Data.saveUser();
    }

    @Override
    public void resume() {
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

    /*
     * ============================================================
     * LEGACY TERMINAL COMMAND SUPPORT
     * ============================================================
     *
     * NetworkView, WalletView and old minigame views
     * still depend on this method.
     */

    protected boolean handleGlobalCommands(
        String command
    ) {

        if (
            command == null
                || command.isBlank()
        ) {

            return false;
        }

        java.util.regex.Matcher enterMatcher =
            java.util.regex.Pattern
                .compile(
                    "(?i)^menu\\s+enter\\s+(?<menuName>.+)$"
                )
                .matcher(command);

        if (
            command.matches(
                "(?i)^menu\\s+show\\s+current$"
            )
        ) {

            if (menu != null) {

                System.out.println(
                    menu.ShowCurrentMenu()
                );
            }

            return true;
        }

        if (
            command.matches(
                "(?i)^menu\\s+exit$"
            )
        ) {

            if (menu != null) {

                System.out.println(
                    menu.exitMenu()
                );
            }

            return true;
        }

        if (enterMatcher.matches()) {

            if (menu == null) {
                return true;
            }

            String targetMenu =
                enterMatcher
                    .group("menuName")
                    .trim();

            targetMenu =
                targetMenu
                    .substring(0, 1)
                    .toUpperCase()
                    +
                    targetMenu
                        .substring(1)
                        .toLowerCase();

            if (
                !targetMenu.endsWith(
                    " menu"
                )
            ) {

                targetMenu += " menu";
            }

            System.out.println(
                menu.ChangeMenu(
                    targetMenu
                )
            );

            return true;
        }

        return false;
    }
}

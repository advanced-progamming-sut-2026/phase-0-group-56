package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
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
    private Stack screenLayers;

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

        screenLayers = new Stack();
        screenLayers.setFillParent(true);

        Image background = MenuVisualAssets.image("background");
        if (background != null) {
            background.setScaling(Scaling.fill);
            background.setTouchable(Touchable.disabled);
            screenLayers.add(background);
        } else {
            Image fallbackBackground = new Image(solidDrawable(
                new Color(0.025f, 0.09f, 0.055f, 1f)
            ));
            fallbackBackground.setTouchable(Touchable.disabled);
            screenLayers.add(fallbackBackground);
        }

        // A restrained veil keeps the official space background atmospheric
        // while preserving contrast for the supplied PvZ controls and fonts.
        Image veil = new Image(solidDrawable(
            new Color(0.01f, 0.025f, 0.035f, 0.30f)
        ));
        veil.setTouchable(Touchable.disabled);
        screenLayers.add(veil);

        root = new Table();
        root.setFillParent(true);
        root.top();
        root.pad(
            8f,
            18f,
            10f,
            18f
        );

        screenLayers.add(root);
        stage.addActor(screenLayers);

        buildHeader();

        content = new Table();
        content.top();
        content.pad(12f);

        ScrollPane scrollPane =
            new ScrollPane(content, skin);

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(
            true,
            false
        );

        Table scrollFrame = new Table();
        // Keep the official main-menu artwork visible between cards. The
        // former opaque skin panel hid almost the entire background.
        scrollFrame.setBackground(solidDrawable(
            new Color(0.04f, 0.10f, 0.08f, 0.20f)
        ));

        scrollFrame.add(scrollPane)
            .grow()
            .pad(5f);

        root.add(scrollFrame)
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

        header.pad(4f, 10f, 4f, 10f);

        Drawable headerBackground =
            getSkinDrawableSafe(
                "image_ui_mainmenu_mm_settings_tab_10"
            );

        if (headerBackground != null) {
            header.setBackground(headerBackground);
        }

        Screen backScreen = getBackScreen();
        Table navigationSlot = new Table();

        if (backScreen != null) {

            // Keep Back as a real TextButton.  The extracted art is used by
            // the other menu controls, but a native Button gives the header
            // a direct hit target and avoids Stack/ImageButton event routing.
            TextButton back = brownButton(
                "BACK",
                () -> App.setScreen(getBackScreen())
            );

            navigationSlot.add(back)
                .width(122f)
                .height(50f)
                .left();
        }

        header.add(navigationSlot)
            .width(220f)
            .left();

        Table identity = new Table();
        Image logo = MenuVisualAssets.image("logo");
        if (logo != null) {
            logo.setScaling(Scaling.fit);
            identity.add(logo)
                .width(210f)
                .height(50f)
                .center()
                .row();
        }

        Label title = mediumTitle(getScreenTitle().toUpperCase());
        title.setAlignment(Align.center);
        identity.add(title)
            .center()
            .padTop(logo == null ? 8f : -2f);

        header.add(identity)
            .expandX()
            .center()
            .padLeft(8f)
            .padRight(8f);

        Table resourceBar =
            buildResourceBar();

        header.add(resourceBar)
            .width(220f)
            .right();

        root.add(header)
            .growX()
            .minHeight(80f)
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

        User user = Data.getCurrentUser();

        if (user == null) {
            return bar;
        }

        Drawable resourceBackground =
            getSkinDrawableSafe(
                "image_ui_dialog_asset_inner_bkgd_10"
            );

        if (resourceBackground != null) {
            bar.setBackground(resourceBackground);
        }

        coinLabel = resourceAmount(user.getCoins());
        diamondLabel = resourceAmount(user.getDiamonds());

        bar.add(resourceChip("coin_small", "COINS", coinLabel))
            .padRight(8f);

        bar.add(resourceChip("gem_small", "GEMS", diamondLabel));

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
                .padTop(5f)
                .padRight(4f);

            bar.add(addGems)
                .width(105f)
                .height(38f)
                .padTop(5f);
        }

        return bar;
    }

    private Label resourceAmount(int amount) {
        Label label = mediumTitle(String.valueOf(amount));
        label.setAlignment(Align.center);
        return label;
    }

    private Table resourceChip(
        String iconKey,
        String caption,
        Label amount
    ) {
        Table chip = new Table();
        Drawable chipBackground = MenuVisualAssets.drawable("counter_bg");
        if (chipBackground != null) {
            chip.setBackground(chipBackground);
        }

        Image icon = MenuVisualAssets.image(iconKey);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            chip.add(icon)
                .size(26f)
                .padRight(3f);
        }

        Table values = new Table();
        Label captionLabel = secondaryLabel(caption);
        captionLabel.setAlignment(Align.center);
        values.add(captionLabel).center().row();
        values.add(amount).center();

        chip.add(values).minWidth(58f).center();
        return chip;
    }

    protected void refreshResourceLabels() {

        User user =
            Data.getCurrentUser();

        if (user == null) {
            return;
        }

        if (coinLabel != null) {

            coinLabel.setText(
                String.valueOf(user.getCoins())
            );
        }

        if (diamondLabel != null) {

            diamondLabel.setText(
                String.valueOf(user.getDiamonds())
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
     * Creates a compact visual shortcut using extracted menu artwork when it
     * exists, then falls back to the bundled skin icon. The action is attached
     * only to the icon so the surrounding menu layout remains unchanged.
     */
    protected Table menuShortcut(
        String iconStyle,
        String caption,
        Runnable action
    ) {
        Table shortcut = pvzInnerPanel();
        shortcut.pad(12f, 10f, 12f, 10f);

        ImageButton icon = createMenuIcon(iconStyle);

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
            .size(66f)
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
        Drawable bannerBackground =
            MenuVisualAssets.drawable("settings_tab");
        if (bannerBackground == null) {
            bannerBackground =
                getSkinDrawableSafe("image_ui_mainmenu_mm_settings_tab_10");
        }
        if (bannerBackground != null) {
            banner.setBackground(bannerBackground);
        }
        banner.pad(10f, 16f, 10f, 16f);

        ImageButton icon = createMenuIcon(iconStyle);

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

    /**
     * Resolves the semantic skin style used by existing screens to the
     * matching extracted artwork. Keeping the mapping here means screens do
     * not know whether a visual comes from the skin module or assets/ui/menu.
     */
    private ImageButton createMenuIcon(String iconStyle) {
        String assetKey = menuAssetKey(iconStyle);
        if (assetKey != null) {
            ImageButton assetButton = MenuVisualAssets.imageButton(
                assetKey,
                pressedAssetKey(assetKey),
                null
            );
            if (assetButton != null) {
                return assetButton;
            }
        }

        try {
            return new ImageButton(skin, iconStyle);
        } catch (Exception exception) {
            return new ImageButton(skin);
        }
    }

    private String menuAssetKey(String iconStyle) {
        if (iconStyle == null) {
            return null;
        }

        return switch (iconStyle) {
            case "hud_quests", "quest" -> "quest";
            case "hud_zg", "greenhouse" -> "greenhouse";
            case "hud_minigames", "minigames" -> "minigames";
            case "hud_task_list", "tasks" -> "tasks";
            case "settings" -> "settings";
            case "shop" -> "shop";
            case "news" -> "news";
            default -> null;
        };
    }

    private String pressedAssetKey(String assetKey) {
        return switch (assetKey) {
            case "quest" -> "quest_down";
            case "event_shop" -> "event_shop_down";
            case "plant_boost" -> "plant_boost_down";
            case "premium" -> "premium_down";
            case "tickets" -> "tickets_down";
            case "coin_buy" -> "coin_buy_down";
            case "gems_buy" -> "gems_buy_down";
            case "generic_currency" -> "generic_currency_down";
            default -> null;
        };
    }

    /**
     * Builds a text-labelled button on top of one of the extracted PvZ
     * controls. Returning a Stack lets callers keep their existing Table cell
     * sizing while the artwork fills the complete hit target.
     */
    protected Stack assetTextButton(
        String normalKey,
        String pressedKey,
        String text,
        Runnable action
    ) {
        ImageButton icon = MenuVisualAssets.imageButton(
            normalKey,
            pressedKey,
            null
        );

        Stack button = new Stack();
        if (icon == null) {
            button.add(greenButton(text, action));
            return button;
        }

        // The artwork is visual-only; the transparent TextButton added below
        // is the reliable interactive layer.
        icon.setTouchable(Touchable.disabled);
        button.add(icon);

        Label caption = mediumTitle(text);
        caption.setAlignment(Align.center);
        caption.setTouchable(Touchable.disabled);
        button.add(caption);

        // Use a real TextButton as an invisible full-size hit target.  A
        // Stack/ImageButton can render the artwork correctly, but its event
        // bubbling is not reliable for purchase controls inside a Table.
        // The transparent button keeps the artwork while guaranteeing that
        // BUY, tabs and CLAIM invoke their action.
        TextButton hitTarget = greenButton("", action);
        hitTarget.setColor(1f, 1f, 1f, 0f);
        button.add(hitTarget);
        return button;
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

        final TextButton visualButton = button;
        button.addListener(
            new ClickListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int buttonCode
                ) {
                    visualButton.setTransform(true);
                    visualButton.setOrigin(Align.center);
                    visualButton.clearActions();
                    visualButton.addAction(Actions.scaleTo(0.97f, 0.97f, 0.06f));
                    return super.touchDown(event, x, y, pointer, buttonCode);
                }

                @Override
                public void touchUp(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int buttonCode
                ) {
                    visualButton.clearActions();
                    visualButton.addAction(Actions.scaleTo(1f, 1f, 0.08f));
                    super.touchUp(event, x, y, pointer, buttonCode);
                }

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
            getSkinDrawableSafe("image_ui_dialog_asset_dialogborder_10");

        if (background == null) {
            background = getSkinDrawableSafe(
                "image_ui_quests_panel_edge_to_edge_ten"
            );
        }

        if (background != null) {
            panel.setBackground(background);
        } else {
            panel.setBackground(solidDrawable(
                new Color(0.06f, 0.15f, 0.12f, 0.94f)
            ));
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
        } else {
            panel.setBackground(solidDrawable(
                new Color(0.10f, 0.22f, 0.16f, 0.95f)
            ));
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

    protected Drawable solidDrawable(Color color) {
        try {
            return skin.newDrawable("white-pixel", color);
        } catch (Exception exception) {
            return getSkinDrawableSafe("white-pixel");
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
        overlay.center();
        overlay.setTouchable(
            Touchable.enabled
        );
        overlay.setBackground(solidDrawable(
            new Color(0.005f, 0.015f, 0.02f, 0.68f)
        ));

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
        overlay.center();
        overlay.setTouchable(
            Touchable.enabled
        );
        overlay.setBackground(solidDrawable(
            new Color(0.005f, 0.015f, 0.02f, 0.68f)
        ));

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

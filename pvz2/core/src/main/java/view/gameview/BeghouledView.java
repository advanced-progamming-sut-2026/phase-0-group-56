package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.menus.gamecontroller.BeghouledController;
import models.App;
import models.factory.builder.PlantType;
import models.games.minigames.Beghouled;
import view.MiniGamesView;

import java.util.Locale;

/** Fully graphical Beghouled screen. */
public final class BeghouledView extends MinigameBoardView {
    private static final String LINK_TILE_PAM =
        "768/INITIAL/BACKGROUNDS/LINKTILE_01/LINKTILE_01.PAM";

    private final int levelNumber;
    private final BeghouledController controller;

    private MinigamePamVisual selectionVisual;
    private Label scoreLabel;
    private Label sunLabel;
    private Label comboLabel;
    private Label statusLabel;
    private Table upgradeTable;
    private Table gameOverPanel;
    private String upgradeSignature = "";
    private float animationTime;

    public BeghouledView(int levelNumber) {
        this.levelNumber = Math.max(1, Math.min(3, levelNumber));
        controller = new BeghouledController(this.levelNumber);
    }

    @Override
    public void show() {
        initialiseScreen();
        if (pvzAssetsRoot != null && textureBank != null) {
            selectionVisual = new MinigamePamVisual(
                pvzAssetsRoot,
                textureBank,
                LINK_TILE_PAM
            );
        }
        buildUi();
        installInput(createInput());
    }

    @Override
    public void render(float delta) {
        float safeDelta = safeDelta(delta);
        animationTime += safeDelta;
        updateSharedAssets();

        String log = controller.playGame(safeDelta);
        if (!log.isBlank()) {
            statusLabel.setText(log);
        }
        refreshHud();

        ScreenUtils.clear(0.03f, 0.08f, 0.04f, 1f);
        renderMap();
        renderSelectionFallback();
        renderWorld(safeDelta);

        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    private void renderWorld(float delta) {
        Beghouled game = controller.getGame();
        beginWorldBatch();
        try {
            renderSelectedTilePam();
            renderPlants(game.getPlantsInField());
            renderProjectiles(game.getBullets(), delta);
            renderZombies(game.getZombies(), delta);
        } finally {
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void renderSelectedTilePam() {
        if (!controller.hasSelection() || selectionVisual == null || !selectionVisual.isReady()) {
            return;
        }
        int column = controller.getSelectedColumn();
        int row = controller.getSelectedRow();
        selectionVisual.draw(
            batch,
            animationTime,
            cellCenterX(column),
            cellCenterY(row),
            lawnBounds.width / 9f,
            lawnBounds.height / 5f
        );
    }

    private void renderSelectionFallback() {
        if (!controller.hasSelection()
            || (selectionVisual != null && selectionVisual.isReady())) {
            return;
        }
        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float x = lawnBounds.x + controller.getSelectedColumn() * cellWidth;
        float y = lawnBounds.y + controller.getSelectedRow() * cellHeight;

        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.84f, 0.12f, 0.35f);
        shapes.rect(x, y, cellWidth, cellHeight);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void buildUi() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);

        TextButton back = button("BACK", "brown", this::goBack);
        Label title = new Label("BEGHOULED  •  LEVEL " + levelNumber,
            skin, "big_outline");
        title.setAlignment(Align.center);
        scoreLabel = new Label("", skin, "medium_outline");
        sunLabel = new Label("", skin, "medium_outline");
        comboLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label("Select two adjacent plants to make a match.", skin);
        statusLabel.setAlignment(Align.center);

        hud.add(back).size(105f, 50f);
        hud.add(scoreLabel).width(150f).padLeft(8f);
        hud.add(title).expandX().center();
        hud.add(sunLabel).width(145f).right();
        hud.add(comboLabel).width(130f).right().row();

        upgradeTable = new Table();
        hud.add(upgradeTable).colspan(5).center().padTop(5f).row();
        hud.add(statusLabel).colspan(5).expandX().fillX().padTop(3f);
        stage.addActor(hud);

        buildGameOverPanel();
        refreshHud();
    }

    private void buildGameOverPanel() {
        gameOverPanel = new Table();
        gameOverPanel.setFillParent(true);
        gameOverPanel.center();
        gameOverPanel.setVisible(false);

        Table card = new Table();
        card.pad(24f);
        Drawable background =
            getSkinDrawableSafe("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }
        card.add(new Label("GAME OVER", skin, "big_outline")).colspan(2).row();
        card.add(button("RETRY", "green", () -> App.setScreen(new BeghouledView(levelNumber))))
            .size(160f, 58f).pad(8f);
        card.add(button("MINIGAMES", "brown", this::goBack)).size(180f, 58f).pad(8f);
        gameOverPanel.add(card);
        stage.addActor(gameOverPanel);
    }

    private void refreshHud() {
        Beghouled game = controller.getGame();
        scoreLabel.setText("SCORE: " + game.getScore());
        sunLabel.setText("SUN: " + game.getSunCount());
        comboLabel.setText("COMBO: x" + Math.max(1, game.getLastCombo()));
        gameOverPanel.setVisible(game.isLost());
        refreshUpgradeButtons();
    }

    private void refreshUpgradeButtons() {
        StringBuilder signature = new StringBuilder();
        for (Beghouled.Upgrade upgrade : controller.getGame().getAvailableUpgrades()) {
            signature.append(upgrade.from()).append(':').append(upgrade.cost()).append('|');
        }
        if (signature.toString().equals(upgradeSignature)) {
            return;
        }
        upgradeSignature = signature.toString();
        upgradeTable.clearChildren();
        for (Beghouled.Upgrade upgrade : controller.getGame().getAvailableUpgrades()) {
            String text = pretty(upgrade.from()) + " → " + pretty(upgrade.to())
                + "  " + upgrade.cost();
            TextButton button = button(text, "brown", () -> performUpgrade(upgrade.from()));
            upgradeTable.add(button).height(46f).padRight(5f);
        }
    }

    private void performUpgrade(PlantType type) {
        statusLabel.setText(controller.upgrade(type));
        upgradeSignature = "";
        refreshHud();
    }

    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    goBack();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || !updatePointer(screenX, screenY)) {
                    return false;
                }
                BeghouledController.SelectionResult result = controller.selectTile(
                    pointerColumn(),
                    pointerRow()
                );
                statusLabel.setText(result.message());
                upgradeSignature = "";
                return true;
            }
        };
    }

    private TextButton button(String text, String style, Runnable action) {
        TextButton button = new TextButton(text, skin, style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    private void goBack() {
        App.setScreen(new MiniGamesView());
    }

    private static String pretty(PlantType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

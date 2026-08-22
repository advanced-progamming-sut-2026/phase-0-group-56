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
import controllers.menus.gamecontroller.IZombieController;
import models.App;
import models.entity.Zombie;
import models.games.minigames.IZombie;
import models.gamepanes.Tile;
import view.MiniGamesView;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Fully graphical reverse-role I, Zombie screen. */
public final class IZombieView extends MinigameBoardView {
    private static final String BRAIN_PAM =
        "768/FULL/EFFECTS/BRAIN_EFFECT/BRAIN_EFFECT.PAM";

    private final int levelNumber;
    private final IZombieController controller;
    private final Map<String, TextButton> zombieButtons = new LinkedHashMap<>();

    private MinigamePamVisual brainVisual;
    private String selectedZombie;
    private Label sunLabel;
    private Label brainLabel;
    private Label statusLabel;
    private Label resultLabel;
    private Table resultPanel;
    private float animationTime;

    public IZombieView(int levelNumber) {
        this.levelNumber = Math.max(1, Math.min(3, levelNumber));
        controller = new IZombieController(this.levelNumber);
    }

    @Override
    public void show() {
        initialiseScreen();
        if (pvzAssetsRoot != null && textureBank != null) {
            brainVisual = new MinigamePamVisual(pvzAssetsRoot, textureBank, BRAIN_PAM);
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

        ScreenUtils.clear(0.08f, 0.035f, 0.05f, 1f);
        renderMap();
        renderBoardGuides();
        renderWorld(safeDelta);

        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    private void renderWorld(float delta) {
        IZombie game = controller.getGame();
        beginWorldBatch();
        try {
            renderBrains(game);
            renderPlants(game.getPlantsInField());
            renderProjectiles(game.getBullets(), delta);
            renderZombies(game.getZombies(), delta);
        } finally {
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void renderBrains(IZombie game) {
        if (brainVisual == null || !brainVisual.isReady()) {
            return;
        }
        float width = lawnBounds.width / 9f * 0.86f;
        float height = lawnBounds.height / 5f * 0.72f;
        for (int row = 0; row < IZombie.ROW_COUNT; row++) {
            if (!game.isBrainEaten(row)) {
                brainVisual.draw(
                    batch,
                    animationTime,
                    cellCenterX(0),
                    cellCenterY(row),
                    width,
                    height
                );
            }
        }
    }

    private void renderBoardGuides() {
        IZombie game = controller.getGame();
        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float lineX = lawnBounds.x + IZombie.FIRST_ZOMBIE_COLUMN * cellWidth;

        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.92f, 0.08f, 0.05f, 0.92f);
        shapes.rect(lineX - 3f, lawnBounds.y, 6f, lawnBounds.height);
        renderProducerHalos(game, cellWidth, cellHeight);
        renderBrainFallbacks(game, cellWidth, cellHeight);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderProducerHalos(IZombie game, float cellWidth, float cellHeight) {
        shapes.setColor(1f, 0.82f, 0.08f, 0.28f);
        for (Zombie producer : game.getSunProducers()) {
            float x = lawnBounds.x + producer.getX() / (9f * Tile.getWidth()) * lawnBounds.width;
            float y = cellCenterY(producer.getLine());
            shapes.circle(x, y, Math.min(cellWidth, cellHeight) * 0.45f, 24);
        }
    }

    private void renderBrainFallbacks(IZombie game, float cellWidth, float cellHeight) {
        if (brainVisual != null && brainVisual.isReady()) {
            return;
        }
        shapes.setColor(0.96f, 0.42f, 0.62f, 0.88f);
        for (int row = 0; row < IZombie.ROW_COUNT; row++) {
            if (!game.isBrainEaten(row)) {
                shapes.ellipse(
                    cellCenterX(0) - cellWidth * 0.32f,
                    cellCenterY(row) - cellHeight * 0.22f,
                    cellWidth * 0.64f,
                    cellHeight * 0.44f
                );
            }
        }
    }

    private void buildUi() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);

        TextButton back = button("BACK", "brown", this::goBack);
        Label title = new Label("I, ZOMBIE  •  LEVEL " + levelNumber, skin, "big_outline");
        title.setAlignment(Align.center);
        sunLabel = new Label("", skin, "medium_outline");
        brainLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label("Choose a zombie, then place it right of the red line.", skin);
        statusLabel.setAlignment(Align.center);

        hud.add(back).size(105f, 50f);
        hud.add(sunLabel).width(145f).padLeft(8f);
        hud.add(title).expandX().center();
        hud.add(brainLabel).width(180f).right().row();

        Table cardRow = buildZombieCards();
        hud.add(cardRow).colspan(4).center().padTop(5f).row();
        hud.add(statusLabel).colspan(4).expandX().fillX().padTop(3f);
        stage.addActor(hud);
        buildResultPanel();
    }

    private Table buildZombieCards() {
        Table row = new Table();
        for (IZombie.ZombieCard card : controller.getGame().getZombieCards()) {
            String text = pretty(card.type()) + "\n" + card.cost() + " SUN";
            TextButton button = button(text, "brown", () -> selectZombie(card.type()));
            button.getLabel().setWrap(true);
            zombieButtons.put(card.type(), button);
            row.add(button).size(150f, 62f).padRight(5f);
        }
        return row;
    }

    private void buildResultPanel() {
        resultPanel = new Table();
        resultPanel.setFillParent(true);
        resultPanel.center();
        resultPanel.setVisible(false);

        Table card = new Table();
        card.pad(24f);
        Drawable background =
            getSkinDrawableSafe("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }
        resultLabel = new Label("", skin, "big_outline");
        card.add(resultLabel).colspan(2).row();
        card.add(button("RETRY", "green", () -> App.setScreen(new IZombieView(levelNumber))))
            .size(160f, 58f).pad(8f);
        card.add(button("MINIGAMES", "brown", this::goBack)).size(180f, 58f).pad(8f);
        resultPanel.add(card);
        stage.addActor(resultPanel);
    }

    private void refreshHud() {
        IZombie game = controller.getGame();
        sunLabel.setText("SUN: " + game.getSunCount());
        brainLabel.setText("BRAINS: " + game.getBrainsEatenCount() + "/5");
        for (Map.Entry<String, TextButton> entry : zombieButtons.entrySet()) {
            IZombie.ZombieCard card = findCard(entry.getKey());
            boolean affordable = card != null && game.getSunCount() >= card.cost();
            entry.getValue().setDisabled(!affordable || game.getState() != models.games.BaseGame.GameState.PLAYING);
            entry.getValue().setColor(
                entry.getKey().equals(selectedZombie)
                    ? new Color(0.72f, 1f, 0.72f, 1f)
                    : Color.WHITE
            );
        }

        boolean ended = game.isWonGame() || game.isLostGame();
        resultPanel.setVisible(ended);
        if (ended) {
            resultLabel.setText(game.isWonGame() ? "ZOMBIES WIN!" : "PLANTS WIN");
        }
    }

    private IZombie.ZombieCard findCard(String type) {
        for (IZombie.ZombieCard card : controller.getGame().getZombieCards()) {
            if (card.type().equals(type)) {
                return card;
            }
        }
        return null;
    }

    private void selectZombie(String type) {
        selectedZombie = type.equals(selectedZombie) ? null : type;
        statusLabel.setText(selectedZombie == null
            ? "Zombie selection cancelled."
            : pretty(selectedZombie) + " selected.");
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
                if (selectedZombie == null) {
                    statusLabel.setText("Choose a zombie card first.");
                    return true;
                }
                statusLabel.setText(controller.placeZombie(
                    selectedZombie,
                    pointerColumn(),
                    pointerRow()
                ));
                refreshHud();
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

    private static String pretty(String value) {
        if (value == null || value.isBlank()) {
            return "Zombie";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}

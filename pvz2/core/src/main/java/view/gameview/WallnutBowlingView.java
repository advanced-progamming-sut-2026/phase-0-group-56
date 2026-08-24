package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.menus.gamecontroller.WallnutController;
import models.App;
import models.Constants;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.games.minigames.BowlingNut;
import models.games.minigames.BigNut;
import models.games.minigames.WallnutBowling;
import view.MiniGamesView;

import java.util.Locale;

/** Graphical Wall-Nut Bowling screen. */
public final class WallnutBowlingView extends MinigameBoardView {
    private static final String WALLNUT_PAM =
        "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    private static final String EXPLODE_NUT_PAM =
        "768/INITIAL/PLANT/EXPLODEONUT/EXPLODEONUT.PAM";

    private final WallnutController controller;
    private MinigamePamVisual wallnutVisual;
    private MinigamePamVisual explodeNutVisual;
    private Table conveyorRow;
    private String beltSignature = "";
    private Label statusLabel;
    private Label progressLabel;
    private Table resultPanel;
    private Label resultLabel;
    private String selectedNut;
    private float animationTime;

    public WallnutBowlingView() {
        this(currentProgress());
    }

    public WallnutBowlingView(int level) {
        controller = new WallnutController(level);
    }

    @Override
    public void show() {
        initialiseScreen();
        if (pvzAssetsRoot != null && textureBank != null) {
            wallnutVisual = new MinigamePamVisual(pvzAssetsRoot, textureBank, WALLNUT_PAM);
            explodeNutVisual = new MinigamePamVisual(
                pvzAssetsRoot,
                textureBank,
                EXPLODE_NUT_PAM
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

        ScreenUtils.clear(0.04f, 0.08f, 0.06f, 1f);
        renderMap();
        renderBoundaryLine();
        renderNutFallbacks();
        renderWorld(safeDelta);

        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    private void renderWorld(float delta) {
        WallnutBowling game = controller.getGame();
        beginWorldBatch();
        try {
            renderNuts(game);
            renderZombies(game.getZombies(), delta);
        } finally {
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void renderNuts(WallnutBowling game) {
        for (BowlingNut nut : game.getNuts()) {
            MinigamePamVisual visual = nut.isExplosive() ? explodeNutVisual : wallnutVisual;
            if (visual == null || !visual.isReady()) {
                continue;
            }
            visual.draw(
                batch,
                animationTime,
                nutCenterX(nut),
                nutCenterY(nut),
                lawnBounds.width / 9f * (nut instanceof BigNut ? 1.08f : 0.74f),
                lawnBounds.height / 5f * (nut instanceof BigNut ? 1.08f : 0.74f)
            );
        }
    }

    private void renderNutFallbacks() {
        if (shapes == null) {
            return;
        }
        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (BowlingNut nut : controller.getGame().getNuts()) {
            MinigamePamVisual visual = nut.isExplosive() ? explodeNutVisual : wallnutVisual;
            if (visual != null && visual.isReady()) {
                continue;
            }
            shapes.setColor(nut instanceof BigNut
                ? new Color(0.86f, 0.66f, 0.24f, 0.95f)
                : nut.isExplosive()
                ? new Color(0.95f, 0.22f, 0.10f, 0.95f)
                : new Color(0.70f, 0.48f, 0.20f, 0.95f));
            shapes.circle(
                nutCenterX(nut),
                nutCenterY(nut),
                Math.min(lawnBounds.width / 9f, lawnBounds.height / 5f)
                    * (nut instanceof BigNut ? 0.38f : 0.28f),
                24
            );
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderBoundaryLine() {
        if (shapes == null || lawnBounds.width <= 0f || lawnBounds.height <= 0f) {
            return;
        }
        worldViewport.apply();
        worldCamera.update();
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.95f, 0.18f, 0.12f, 0.95f));
        float x = lawnBounds.x + Constants.WALLNUT_LIMIT_LINE * lawnBounds.width / 9f;
        shapes.rect(x - 2f, lawnBounds.y, 4f, lawnBounds.height);
        shapes.end();
    }

    private float nutCenterX(BowlingNut nut) {
        return lawnBounds.x
            + (nut.getX() + nut.getWidth() * 0.5f)
            / (9f * Tile.getWidth()) * lawnBounds.width;
    }

    private float nutCenterY(BowlingNut nut) {
        return lawnBounds.y
            + (nut.getY() + nut.getHeight() * 0.5f)
            / (5f * Tile.getHeight()) * lawnBounds.height;
    }

    private void buildUi() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);

        TextButton back = button("BACK", "brown", this::goBack);
        Label title = new Label(
            "WALL-NUT BOWLING  •  LEVEL " + controller.getProgress(),
            skin,
            "big_outline"
        );
        title.setAlignment(Align.center);
        progressLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label("Choose a nut and place it before the red line.", skin);
        statusLabel.setAlignment(Align.center);

        hud.add(back).size(105f, 50f);
        hud.add(title).expandX().center();
        hud.add(progressLabel).width(210f).right().row();

        Table controls = new Table();
        conveyorRow = new Table();
        controls.add(conveyorRow).center();
        hud.add(controls).colspan(3).center().padTop(4f).row();
        hud.add(statusLabel).colspan(3).expandX().fillX().padTop(4f);
        stage.addActor(hud);
        buildResultPanel();
        refreshHud();
    }

    private void buildResultPanel() {
        resultPanel = new Table();
        resultPanel.setFillParent(true);
        resultPanel.center();
        resultPanel.setVisible(false);

        Table card = new Table();
        card.pad(24f);
        Drawable background = getSkinDrawableSafe("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }
        resultLabel = new Label("", skin, "big_outline");
        card.add(resultLabel).colspan(2).row();
        card.add(button("RETRY", "green", () ->
                App.setScreen(new WallnutBowlingView(controller.getProgress()))))
            .size(160f, 58f).pad(8f);
        card.add(button("MINIGAMES", "brown", this::goBack))
            .size(180f, 58f).pad(8f);
        resultPanel.add(card);
        stage.addActor(resultPanel);
    }

    private void refreshHud() {
        WallnutBowling game = controller.getGame();
        progressLabel.setText("NUTS: " + game.getNuts().size()
            + "   ZOMBIES: " + game.getZombies().size());
        boolean playing = game.getState() == models.games.BaseGame.GameState.PLAYING;
        refreshConveyor(game, playing);

        boolean ended = game.getState() == models.games.BaseGame.GameState.END;
        resultPanel.setVisible(ended);
        if (ended) {
            boolean won = game.check_endGame().message() != null
                && "Won".equals(game.check_endGame().message());
            resultLabel.setText(won ? "WALL-NUTS WIN!" : "ZOMBIES WIN");
        }
    }

    private void refreshConveyor(WallnutBowling game, boolean playing) {
        StringBuilder signature = new StringBuilder();
        for (String type : game.getBelt()) {
            signature.append(type).append('|');
        }
        signature.append("SELECTED=").append(selectedNut);
        String nextSignature = signature.toString();
        if (conveyorRow == null) {
            return;
        }
        if (nextSignature.equals(beltSignature)) {
            for (Actor actor : conveyorRow.getChildren()) {
                if (actor instanceof TextButton button) {
                    button.setDisabled(!playing);
                }
            }
            return;
        }
        beltSignature = nextSignature;
        conveyorRow.clearChildren();
        for (String type : game.getBelt()) {
            TextButton card = button(type.toUpperCase(Locale.ROOT),
                "brown", () -> selectNut(type));
            card.getLabel().setWrap(true);
            card.getLabel().setAlignment(Align.center);
            card.setDisabled(!playing);
            card.setColor(type.equals(selectedNut)
                ? new Color(0.72f, 1f, 0.72f, 1f) : Color.WHITE);
            conveyorRow.add(card).size(166f, 58f).padRight(6f);
        }
    }

    private void selectNut(String type) {
        selectedNut = type.equals(selectedNut) ? null : type;
        statusLabel.setText(selectedNut == null
            ? "Nut selection cancelled."
            : pretty(selectedNut) + " selected. Click a tile before the red line.");
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
                if (button != Input.Buttons.LEFT || selectedNut == null
                    || !updatePointer(screenX, screenY)) {
                    return false;
                }
                boolean placed = controller.plant(selectedNut, pointerColumn(), pointerRow());
                statusLabel.setText(placed
                    ? pretty(selectedNut) + " launched."
                    : "Select a nut on the conveyor and place it before the red line.");
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
        return value == null ? "Nut" : value.toLowerCase(Locale.ROOT);
    }

    private static int currentProgress() {
        models.User user = App.getCurrentuser();
        return user == null ? 1 : user.getWallNutBowling();
    }
}

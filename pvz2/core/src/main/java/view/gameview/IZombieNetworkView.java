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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.menus.gamecontroller.IZombieNetworkController;
import models.App;
import models.entity.Plant;
import models.entity.Projectile;
import models.entity.ProjectileType;
import models.entity.Zombie;
import models.factory.PlantFactory;
import models.factory.ZombieFactory;
import models.games.minigames.IZombie;
import models.factory.builder.PlantType;
import models.gamepanes.Tile;
import network.IZombieNetworkMatch;
import network.IZombieNetworkState;
import network.NetworkClient;
import network.NetworkService;
import view.MiniGamesView;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/** Graphical client for the authoritative network I, Zombie match. */
public final class IZombieNetworkView extends MinigameBoardView {
    private final IZombieNetworkController controller;
    private String selectedType;
    private Label statusLabel;
    private Label resourceLabel;
    private Label timerLabel;
    private Label scoreLabel;
    private Label reactionLabel;
    private Table resultPanel;
    private Label resultText;
    private float reactionTime;

    public IZombieNetworkView(String matchId, String role, String opponent) {
        this(matchId, parseRole(role), opponent);
    }

    public IZombieNetworkView(
        String matchId, IZombieNetworkState.Role role, String opponent
    ) {
        NetworkClient client = NetworkService.getClient();
        if (client == null) {
            throw new IllegalStateException("Network client is not connected.");
        }
        controller = new IZombieNetworkController(client, matchId, role);
    }

    private static IZombieNetworkState.Role parseRole(String value) {
        try {
            return IZombieNetworkState.Role.valueOf(value == null ? "" : value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return IZombieNetworkState.Role.ZOMBIES;
        }
    }

    @Override
    public void show() {
        initialiseScreen();
        buildUi();
        installInput(createInput());
        statusLabel.setText(controller.join());
    }

    @Override
    public void render(float delta) {
        float safeDelta = safeDelta(delta);
        updateSharedAssets();
        reactionTime = Math.max(0f, reactionTime - safeDelta);
        if (reactionLabel != null && reactionTime > 0f) {
            reactionLabel.setScale(1f + 0.12f * (float) Math.sin(reactionTime * 12f));
        } else if (reactionLabel != null) {
            reactionLabel.setScale(1f);
        }
        processReactions();
        refreshHud();

        ScreenUtils.clear(0.04f, 0.08f, 0.05f, 1f);
        renderMap();
        renderBoardGuides();
        renderState();
        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    private void renderState() {
        IZombieNetworkState state = controller.getState();
        if (state == null) {
            return;
        }
        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < IZombie.ROW_COUNT; row++) {
            if (!state.eatenBrains()[row]) {
                shapes.setColor(0.95f, 0.30f, 0.55f, 0.85f);
                shapes.circle(cellCenterX(0), cellCenterY(row), 24f, 20);
            }
        }
        for (IZombieNetworkState.Unit plant : state.plants()) {
            shapes.setColor(0.25f, 0.85f, 0.35f, 0.95f);
            shapes.circle(cellCenterX(Math.round(plant.x() - 0.5f)), cellCenterY(plant.row()), 24f, 20);
            drawHealth(plant.x(), plant.row(), plant.hp() / plant.maxHp(), Color.GREEN);
        }
        for (IZombieNetworkState.Unit zombie : state.zombies()) {
            shapes.setColor(0.50f, 0.25f, 0.18f, 0.98f);
            float x = lawnBounds.x + zombie.x() / IZombieNetworkMatch.COLUMN_COUNT * lawnBounds.width;
            shapes.rect(x - 18f, cellCenterY(zombie.row()) - 34f, 36f, 58f);
            drawHealth(zombie.x(), zombie.row(), zombie.hp() / zombie.maxHp(), Color.RED);
        }
        shapes.setColor(0.95f, 0.95f, 0.35f, 1f);
        for (IZombieNetworkState.Projectile projectile : state.projectiles()) {
            float x = lawnBounds.x + projectile.x() / IZombieNetworkMatch.COLUMN_COUNT * lawnBounds.width;
            shapes.circle(x, cellCenterY(projectile.row()), 8f, 12);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        renderPamEntities(state);
    }

    private void renderPamEntities(IZombieNetworkState state) {
        if (plantRenderer == null && zombieRenderer == null && projectileRenderer == null) {
            return;
        }
        List<Plant> plants = createPlantModels(state);
        List<Zombie> zombies = createZombieModels(state);
        List<Projectile> projectiles = createProjectileModels(state);
        beginWorldBatch();
        try {
            for (Plant plant : plants) {
                if (plantRenderer != null) {
                    plantRenderer.render(batch, plant, cellCenterX(plant.getTileIndex()),
                        cellCenterY(plant.getLine()), lawnBounds.width / 9f, lawnBounds.height / 5f);
                }
            }
            if (zombieRenderer != null) {
                zombieRenderer.render(batch, zombies, lawnBounds, 1f / 60f);
            }
            if (projectileRenderer != null) {
                projectileRenderer.render(batch, projectiles, lawnBounds, 1f / 60f);
            }
        } finally {
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private List<Plant> createPlantModels(IZombieNetworkState state) {
        List<Plant> result = new ArrayList<>();
        PlantFactory factory = new PlantFactory();
        for (IZombieNetworkState.Unit unit : state.plants()) {
            try {
                Plant plant = factory.createPlant(PlantType.valueOf(
                    unit.type().toUpperCase(Locale.ROOT)));
                plant.setTileIndex(Math.max(0, Math.min(8, Math.round(unit.x() - 0.5f))));
                plant.setLine(unit.row());
                plant.setHP(unit.hp());
                result.add(plant);
            } catch (RuntimeException ignored) {
                // The shape fallback remains visible when an optional card asset is absent.
            }
        }
        return result;
    }

    private List<Zombie> createZombieModels(IZombieNetworkState state) {
        List<Zombie> result = new ArrayList<>();
        for (IZombieNetworkState.Unit unit : state.zombies()) {
            try {
                Zombie zombie = ZombieFactory.createZombie(unit.type());
                zombie.setLine(unit.row());
                zombie.setTileIndex(Math.max(0, Math.min(8, (int) unit.x())));
                zombie.setPosition(unit.x() * Tile.getWidth() - zombie.getWidth() * 0.5f,
                    unit.row() * Tile.getHeight());
                zombie.setHp(Math.round(unit.hp()));
                zombie.setAlive(true);
                result.add(zombie);
            } catch (RuntimeException ignored) {
                // Keep rendering the remaining synchronized entities.
            }
        }
        return result;
    }

    private List<Projectile> createProjectileModels(IZombieNetworkState state) {
        List<Projectile> result = new ArrayList<>();
        for (IZombieNetworkState.Projectile unit : state.projectiles()) {
            Projectile projectile = new Projectile();
            projectile.setType(ProjectileType.PEA);
            projectile.setLine(unit.row());
            projectile.setPosition(unit.x() * Tile.getWidth(), unit.row() * Tile.getHeight() + 30f);
            projectile.setDamage(unit.damage());
            projectile.setActive(true);
            result.add(projectile);
        }
        return result;
    }

    private void drawHealth(float x, int row, float ratio, Color color) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        float worldX = lawnBounds.x + x / IZombieNetworkMatch.COLUMN_COUNT * lawnBounds.width;
        shapes.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapes.rect(worldX - 22f, cellCenterY(row) + 35f, 44f, 5f);
        shapes.setColor(color);
        shapes.rect(worldX - 22f, cellCenterY(row) + 35f, 44f * clamped, 5f);
    }

    private void renderBoardGuides() {
        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float lineX = lawnBounds.x + IZombieNetworkMatch.FIRST_ZOMBIE_COLUMN * lawnBounds.width / 9f;
        shapes.setColor(0.95f, 0.08f, 0.06f, 0.95f);
        shapes.rect(lineX - 3f, lawnBounds.y, 6f, lawnBounds.height);
        shapes.end();
    }

    private void buildUi() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);
        TextButton back = button("LEAVE", "brown", this::leave);
        Label title = new Label("NETWORK I, ZOMBIE", skin, "big_outline");
        title.setAlignment(Align.center);
        resourceLabel = new Label("", skin, "medium_outline");
        timerLabel = new Label("", skin, "medium_outline");
        scoreLabel = new Label("", skin, "medium_outline");
        reactionLabel = new Label("", skin, "medium_outline");
        hud.add(back).size(110f, 50f);
        hud.add(resourceLabel).width(175f).padLeft(8f);
        hud.add(title).expandX().center();
        hud.add(timerLabel).width(170f).right();
        hud.row();
        hud.add(scoreLabel).colspan(4).center().padTop(4f).row();
        hud.add(buildCards()).colspan(4).center().padTop(5f).row();
        hud.add(buildReactionButtons()).colspan(4).center().padTop(4f).row();
        statusLabel = new Label("Waiting for the opponent...", skin);
        statusLabel.setAlignment(Align.center);
        hud.add(statusLabel).colspan(4).expandX().fillX().padTop(4f).row();
        hud.add(reactionLabel).colspan(4).center().padTop(4f);
        stage.addActor(hud);
        buildResultPanel();
    }

    private Table buildCards() {
        Table row = new Table();
        if (controller.getRole() == IZombieNetworkState.Role.PLANTS) {
            for (IZombieNetworkMatch.Card card : IZombieNetworkMatch.plantCards()) {
                row.add(button(card.type().replace('_', ' ') + "\n" + card.cost() + " SUN",
                    "brown", () -> selectedType = toggle(card.type()))).size(160f, 60f).pad(3f);
            }
        } else {
            for (IZombieNetworkMatch.Card card : IZombieNetworkMatch.zombieCards()) {
                row.add(button(card.type() + "\n" + card.cost() + " SUN",
                    "brown", () -> selectedType = toggle(card.type()))).size(160f, 60f).pad(3f);
            }
        }
        return row;
    }

    private Table buildReactionButtons() {
        Table row = new Table();
        addReaction(row, "NICE MOVE", "TEXT", "NICE_MOVE");
        addReaction(row, "HURRY UP", "TEXT", "HURRY_UP");
        addReaction(row, "GOOD GAME", "TEXT", "GOOD_GAME");
        addReaction(row, "😀", "EMOJI", "😀");
        addReaction(row, "😎", "EMOJI", "😎");
        addReaction(row, "🌻", "EMOJI", "🌻");
        addReaction(row, "DANCE", "STICKER", "ZOMBIE_DANCE");
        addReaction(row, "BRAIN", "STICKER", "BRAIN_POP");
        addReaction(row, "SUN", "STICKER", "SUN_DANCE");
        return row;
    }

    private void addReaction(Table row, String text, String category, String value) {
        row.add(button(text, "brown", () -> statusLabel.setText(
            controller.sendReaction(category, value)))).size(100f, 35f).pad(2f);
    }

    private void buildResultPanel() {
        resultPanel = new Table();
        resultPanel.setFillParent(true);
        resultPanel.center();
        resultPanel.setVisible(false);
        Table card = new Table();
        card.pad(24f);
        resultText = new Label("", skin, "big_outline");
        card.add(resultText).colspan(2).row();
        card.add(button("SUBMIT SCORE", "green", () -> statusLabel.setText(controller.submitScore())))
            .size(180f, 55f).pad(6f);
        card.add(button("MINIGAMES", "brown", this::leave)).size(180f, 55f).pad(6f);
        resultPanel.add(card);
        stage.addActor(resultPanel);
    }

    private void refreshHud() {
        IZombieNetworkState state = controller.getState();
        if (state == null) {
            return;
        }
        int resource = controller.getRole() == IZombieNetworkState.Role.PLANTS
            ? state.plantSun() : state.zombieSun();
        String resourceName = controller.getRole() == IZombieNetworkState.Role.PLANTS
            ? "PLANT SUN: " : "ZOMBIE SUN: ";
        resourceLabel.setText(resourceName + resource);
        timerLabel.setText("TIME: " + Math.max(0, state.remainingMillis() / 1000));
        scoreLabel.setText("BRAINS: " + state.brainsEaten() + "/5   •   SCORE: "
            + (controller.getRole() == IZombieNetworkState.Role.PLANTS ? state.plantScore() : state.zombieScore()));
        boolean ended = state.phase() == IZombieNetworkState.Phase.PLANTS_WON
            || state.phase() == IZombieNetworkState.Phase.ZOMBIES_WON
            || state.phase() == IZombieNetworkState.Phase.ABORTED;
        resultPanel.setVisible(ended);
        if (ended) {
            if (state.phase() != IZombieNetworkState.Phase.ABORTED
                && !controller.isScoreSubmitted()) {
                statusLabel.setText(controller.submitScore());
            }
            resultText.setText(state.phase() == IZombieNetworkState.Phase.PLANTS_WON
                ? "PLANTS WIN!" : state.phase() == IZombieNetworkState.Phase.ZOMBIES_WON
                ? "ZOMBIES WIN!" : "MATCH ABORTED");
        }
    }

    private void processReactions() {
        IZombieNetworkController.Reaction reaction;
        while ((reaction = controller.pollReaction()) != null) {
            reactionLabel.setText(reaction.sender() + ": " + reaction.value());
            reactionTime = 3f;
        }
        if (reactionTime <= 0f) {
            reactionLabel.setText("");
        }
    }

    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    leave();
                    return true;
                }
                if (controller.getRole() == IZombieNetworkState.Role.ZOMBIES
                    && keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_3) {
                    int index = keycode - Input.Keys.NUM_1;
                    selectedType = IZombieNetworkMatch.zombieCards().get(index).type();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || !updatePointer(screenX, screenY)
                    || selectedType == null) {
                    return false;
                }
                String result;
                if (controller.getRole() == IZombieNetworkState.Role.PLANTS) {
                    result = controller.placePlant(selectedType, pointerColumn(), pointerRow());
                } else {
                    result = controller.placeZombie(selectedType, pointerColumn(), pointerRow());
                }
                statusLabel.setText(result);
                return true;
            }
        };
    }

    private String toggle(String type) {
        return type.equals(selectedType) ? null : type;
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

    private void leave() {
        controller.leave();
        controller.close();
        App.setScreen(new MiniGamesView());
    }

    @Override
    public void dispose() {
        controller.close();
        super.dispose();
    }
}

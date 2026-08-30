package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.menus.gamecontroller.CouchIZombieController;
import models.App;
import models.entity.Plant;
import models.entity.Projectile;
import models.entity.ProjectileType;
import models.entity.Zombie;
import models.factory.PlantFactory;
import models.factory.ZombieFactory;
import models.factory.builder.PlantType;
import models.gamepanes.Tile;
import network.IZombieNetworkMatch;
import network.IZombieNetworkState;
import view.MiniGamesView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Couch Play: plants use the mouse, zombies use the keyboard. */
public final class IZombieCouchView extends MinigameBoardView {
    private final CouchIZombieController controller = new CouchIZombieController();
    private String selectedPlant;
    private String selectedZombie;
    private int zombieColumn = IZombieNetworkMatch.FIRST_ZOMBIE_COLUMN;
    private int zombieRow;
    private Label statusLabel;
    private Label hudLabel;
    private Label resultLabel;

    @Override
    public void show() {
        initialiseScreen();
        buildUi();
        installInput(createInput());
    }

    @Override
    public void render(float delta) {
        float safeDelta = safeDelta(delta);
        controller.playGame(safeDelta);
        refreshHud();
        ScreenUtils.clear(0.04f, 0.08f, 0.05f, 1f);
        renderMap();
        renderWorld();
        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    private void renderWorld() {
        IZombieNetworkState state = controller.getGame().snapshot();
        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < 5; row++) {
            if (!state.eatenBrains()[row]) {
                shapes.setColor(0.95f, 0.30f, 0.55f, 0.85f);
                shapes.circle(cellCenterX(0), cellCenterY(row), 24f, 20);
            }
        }
        for (IZombieNetworkState.Unit plant : state.plants()) {
            shapes.setColor(0.25f, 0.85f, 0.35f, 0.95f);
            shapes.circle(cellCenterX(Math.round(plant.x() - 0.5f)), cellCenterY(plant.row()), 24f, 20);
        }
        for (IZombieNetworkState.Unit zombie : state.zombies()) {
            shapes.setColor(0.50f, 0.25f, 0.18f, 0.98f);
            float x = lawnBounds.x + zombie.x() / 9f * lawnBounds.width;
            shapes.rect(x - 18f, cellCenterY(zombie.row()) - 34f, 36f, 58f);
        }
        shapes.setColor(0.95f, 0.95f, 0.35f, 1f);
        for (IZombieNetworkState.Projectile projectile : state.projectiles()) {
            float x = lawnBounds.x + projectile.x() / 9f * lawnBounds.width;
            shapes.circle(x, cellCenterY(projectile.row()), 8f, 12);
        }
        shapes.setColor(0.95f, 0.08f, 0.06f, 0.95f);
        float lineX = lawnBounds.x + 5f * lawnBounds.width / 9f;
        shapes.rect(lineX - 3f, lawnBounds.y, 6f, lawnBounds.height);
        if (selectedZombie != null) {
            shapes.setColor(1f, 1f, 0.3f, 0.9f);
            float x = cellCenterX(zombieColumn);
            float y = cellCenterY(zombieRow);
            shapes.rect(x - 35f, y - 45f, 70f, 90f);
        }
        shapes.end();
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
                // Shape fallback remains visible for a missing optional asset.
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
                // Continue rendering entities whose assets are available.
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

    private void buildUi() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);
        hud.add(button("LEAVE", "brown", this::goBack)).size(110f, 50f);
        Label title = new Label("I, ZOMBIE  •  COUCH PLAY", skin, "big_outline");
        title.setAlignment(Align.center);
        hud.add(title).expandX().center();
        hudLabel = new Label("", skin, "medium_outline");
        hud.add(hudLabel).width(340f).right().row();
        Table plants = new Table();
        for (IZombieNetworkMatch.Card card : IZombieNetworkMatch.plantCards()) {
            plants.add(button("PLANT: " + card.type().replace('_', ' '), "green",
                () -> selectedPlant = card.type())).size(180f, 48f).pad(3f);
        }
        hud.add(plants).colspan(3).center().padTop(7f).row();
        statusLabel = new Label("Mouse: choose a plant and click the left side.  Keyboard: 1-3, arrows, SPACE.", skin);
        statusLabel.setAlignment(Align.center);
        hud.add(statusLabel).colspan(3).expandX().fillX().padTop(4f);
        stage.addActor(hud);
        resultLabel = new Label("", skin, "big_outline");
        resultLabel.setVisible(false);
        resultLabel.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        resultLabel.setPosition(0f, 0f);
        resultLabel.setAlignment(Align.center);
        stage.addActor(resultLabel);
    }

    private void refreshHud() {
        IZombieNetworkState state = controller.getGame().snapshot();
        hudLabel.setText("PLANT SUN: " + state.plantSun() + "  ZOMBIE SUN: " + state.zombieSun()
            + "  BRAINS: " + state.brainsEaten() + "/5");
        if (state.phase() != IZombieNetworkState.Phase.PLAYING) {
            resultLabel.setVisible(true);
            resultLabel.setText(state.phase() == IZombieNetworkState.Phase.PLANTS_WON
                ? "PLANTS WIN!" : "ZOMBIES WIN!");
        }
    }

    private InputAdapter createInput() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    goBack();
                    return true;
                }
                if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_3) {
                    selectedZombie = IZombieNetworkMatch.zombieCards()
                        .get(keycode - Input.Keys.NUM_1).type();
                    selectedPlant = null;
                    return true;
                }
                if (keycode == Input.Keys.LEFT) zombieColumn = Math.max(5, zombieColumn - 1);
                if (keycode == Input.Keys.RIGHT) zombieColumn = Math.min(8, zombieColumn + 1);
                if (keycode == Input.Keys.UP) zombieRow = Math.min(4, zombieRow + 1);
                if (keycode == Input.Keys.DOWN) zombieRow = Math.max(0, zombieRow - 1);
                if (keycode == Input.Keys.SPACE && selectedZombie != null) {
                    statusLabel.setText(controller.placeZombie(selectedZombie, zombieColumn, zombieRow));
                    return true;
                }
                return keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT
                    || keycode == Input.Keys.UP || keycode == Input.Keys.DOWN;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT || selectedPlant == null
                    || !updatePointer(screenX, screenY)) {
                    return false;
                }
                statusLabel.setText(controller.placePlant(selectedPlant, pointerColumn(), pointerRow()));
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
}

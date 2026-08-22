package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import models.entity.Plant;
import models.entity.Projectile;
import models.entity.Zombie;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import view.View;

/** Shared rendering shell for lawn-based graphical minigames. */
abstract class MinigameBoardView extends View {
    protected static final float MAX_DELTA = 1f / 15f;

    protected final OrthographicCamera worldCamera = new OrthographicCamera();
    protected final OrthographicCamera uiCamera = new OrthographicCamera();
    protected final Rectangle lawnBounds = new Rectangle();
    protected final Vector2 pointerWorld = new Vector2();

    protected FitViewport worldViewport;
    protected FitViewport uiViewport;
    protected SpriteBatch batch;
    protected ShapeRenderer shapes;
    protected FileHandle pvzAssetsRoot;
    protected TextureBank textureBank;
    protected PlantRenderer plantRenderer;
    protected ZombieRenderer zombieRenderer;
    protected ProjectileRenderer projectileRenderer;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private InputMultiplexer inputMultiplexer;
    private boolean disposed;

    protected final void initialiseScreen() {
        disposed = false;
        skin = PvzSkin.get();
        worldViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, worldCamera);
        uiViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(uiViewport);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        loadMap();
        configureLawn();
        initialiseAssets();
    }

    protected final void installInput(InputAdapter worldInput) {
        inputMultiplexer = new InputMultiplexer(stage, worldInput);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    protected final float safeDelta(float delta) {
        return Math.min(Math.max(0f, delta), MAX_DELTA);
    }

    protected final void updateSharedAssets() {
        if (textureBank != null) {
            textureBank.update();
        }
        if (plantRenderer != null) {
            plantRenderer.update();
        }
    }

    protected final void renderMap() {
        if (mapRenderer == null) {
            return;
        }
        worldViewport.apply();
        worldCamera.update();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
    }

    protected final void beginWorldBatch() {
        worldViewport.apply();
        worldCamera.update();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
    }

    protected final void renderPlants(Iterable<Plant> plants) {
        if (plantRenderer == null || plants == null) {
            return;
        }
        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        for (Plant plant : plants) {
            if (plant == null || plant.getHp() <= 0f) {
                continue;
            }
            plantRenderer.render(
                batch,
                plant,
                cellCenterX(plant.getTileIndex()),
                cellCenterY(plant.getLine()),
                cellWidth,
                cellHeight
            );
        }
    }

    protected final void renderZombies(Iterable<Zombie> zombies, float delta) {
        if (zombieRenderer != null) {
            zombieRenderer.render(batch, zombies, lawnBounds, delta);
        }
    }

    protected final void renderProjectiles(java.util.List<Projectile> projectiles, float delta) {
        if (projectileRenderer != null) {
            projectileRenderer.render(batch, projectiles, lawnBounds, delta);
        }
    }

    protected final float cellCenterX(int column) {
        return lawnBounds.x + (column + 0.5f) * lawnBounds.width / 9f;
    }

    protected final float cellCenterY(int row) {
        return lawnBounds.y + (row + 0.5f) * lawnBounds.height / 5f;
    }

    protected final boolean updatePointer(int screenX, int screenY) {
        pointerWorld.set(screenX, screenY);
        worldViewport.unproject(pointerWorld);
        return lawnBounds.contains(pointerWorld);
    }

    protected final int pointerColumn() {
        return Math.max(0, Math.min(8, (int) (
            (pointerWorld.x - lawnBounds.x) / (lawnBounds.width / 9f)
        )));
    }

    protected final int pointerRow() {
        return Math.max(0, Math.min(4, (int) (
            (pointerWorld.y - lawnBounds.y) / (lawnBounds.height / 5f)
        )));
    }

    private void loadMap() {
        try {
            tiledMap = new TmxMapLoader().load("ancientegypt.tmx");
            mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);
        } catch (RuntimeException exception) {
            tiledMap = null;
            mapRenderer = null;
            Gdx.app.error(getClass().getSimpleName(), "Could not load ancientegypt.tmx", exception);
        }
    }

    private void configureLawn() {
        if (tiledMap != null) {
            try {
                lawnBounds.set(PitchBoundsReader.read(tiledMap));
            } catch (RuntimeException exception) {
                setFallbackLawn();
            }
        } else {
            setFallbackLawn();
        }

        float cameraX = lawnBounds.x + lawnBounds.width / 2f;
        float cameraY = lawnBounds.y + lawnBounds.height / 2f;
        if (tiledMap != null) {
            MapProperties properties = tiledMap.getProperties();
            cameraX = numberProperty(properties, "battleCameraX", cameraX);
            cameraY = numberProperty(properties, "battleCameraY", cameraY);
        }
        worldCamera.position.set(cameraX, cameraY, 0f);
        worldCamera.update();
    }

    private void setFallbackLawn() {
        lawnBounds.set(250f, 105f, 9f * 82f, 5f * 97f);
    }

    private static float numberProperty(MapProperties properties, String key, float fallback) {
        Object value = properties == null ? null : properties.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return value == null ? fallback : Float.parseFloat(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void initialiseAssets() {
        pvzAssetsRoot = PvzAssetLocator.find();
        if (pvzAssetsRoot == null) {
            Gdx.app.error(getClass().getSimpleName(), "Extracted PvZ assets were not found.");
            return;
        }
        try {
            textureBank = new TextureBank("768", pvzAssetsRoot);
            plantRenderer = new PlantRenderer(pvzAssetsRoot);
            zombieRenderer = new ZombieRenderer(pvzAssetsRoot, textureBank);
            projectileRenderer = new ProjectileRenderer(pvzAssetsRoot, textureBank);
        } catch (RuntimeException exception) {
            Gdx.app.error(getClass().getSimpleName(), "Could not initialise PvZ renderers.", exception);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (worldViewport != null) {
            worldViewport.update(width, height, false);
        }
        if (uiViewport != null) {
            uiViewport.update(width, height, true);
        }
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == inputMultiplexer) {
            Gdx.input.setInputProcessor(null);
        }
        if (!disposed) {
            Gdx.app.postRunnable(this::dispose);
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (tiledMap != null) {
            tiledMap.dispose();
        }
        if (plantRenderer != null) {
            plantRenderer.dispose();
        }
        if (zombieRenderer != null) {
            zombieRenderer.dispose();
        }
        if (projectileRenderer != null) {
            projectileRenderer.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        if (textureBank != null) {
            textureBank.dispose();
        }
    }
}

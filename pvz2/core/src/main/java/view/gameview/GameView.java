package view.gameview;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import models.entity.Sun;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controllers.datacontroller.SeedPackage;
import controllers.menus.gamecontroller.GameController;
import models.App;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.BaseGame;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import view.PlayView;
import view.View;
import view.components.PlantTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main graphical screen for a level.
 *
 * <p>Architecture contract used by this project:</p>
 * <ul>
 *     <li>PlayMenu passes chapter + level to this View.</li>
 *     <li>GameView creates one GameController.</li>
 *     <li>GameController creates/owns the BaseGame instance.</li>
 *     <li>GameView reads game state only through controller.getGame().</li>
 *     <li>Every gameplay mutation goes through GameController.</li>
 * </ul>
 */
public final class GameView extends View {

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;
    private static final float MAX_DELTA = 1f / 15f;
    private static final float CAMERA_SLIDE_DURATION = 0.75f;
    private static final int REQUIRED_SELECTION_COUNT = 5;
    private static final String PVZ_ASSET_RESOLUTION = "768";

    private final Chapters chapter;
    private final Level level;
    private final GameController controller;

    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final Vector2 pointerWorld = new Vector2();
    private final Rectangle lawnBounds = new Rectangle();

    private FitViewport worldViewport;
    private FitViewport uiViewport;
    private Stage stage;
    private InputMultiplexer inputMultiplexer;
    private InputAdapter worldInput;

    private Skin skin;

    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float mapPixelWidth = VIRTUAL_WIDTH;
    private float mapPixelHeight = VIRTUAL_HEIGHT;

    private float startCameraX;
    private float startCameraY;
    private float battleCameraX;
    private float battleCameraY;

    private boolean cameraSliding;
    private float cameraSlideElapsed;
    private float cameraSlideFromX;
    private float cameraSlideFromY;

    private Table preparationRoot;
    private Table selectedSlotsTable;
    private Label selectionCountLabel;
    private Label preparationStatusLabel;
    private TextButton letsRockButton;
    private PlantTable plantTable;

    private ToolsStack toolsStack;

    private FileHandle pvzAssetsRoot;
    private TextureBank textureBank;
    private SpriteBatch entityBatch;
    private ProjectileRenderer projectileRenderer;
    private WorldEntityRenderer worldEntities;

    private boolean appPausedGame;
    private boolean disposed;

    public GameView(Chapters chapter, Level level) {
        if (chapter == null) {
            throw new IllegalArgumentException("chapter cannot be null");
        }
        if (level == null) {
            throw new IllegalArgumentException("level cannot be null");
        }

        this.chapter = chapter;
        this.level = level;
        this.controller = new GameController(chapter, level);

        // View's existing CLI/menu contract.
        this.menu = controller;
    }

    @Override
    public void show() {
        disposed = false;

        skin = PvzSkin.get();

        worldViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, worldCamera);
        uiViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(uiViewport);

        loadTiledMap();
        configureMapGeometry();
        initialisePvzAssets();
        entityBatch = new SpriteBatch();
        if (pvzAssetsRoot != null && textureBank != null) {
            projectileRenderer = new ProjectileRenderer(pvzAssetsRoot, textureBank);
        }
        initialiseWorldEntities();

        toolsStack = new ToolsStack(controller);
        toolsStack.setVisible(false);
        stage.addActor(toolsStack);

        if (controller.getGame().getState() == BaseGame.GameState.STARTING) {
            buildPreparationUI();
            setWorldCamera(startCameraX, startCameraY);
        } else {
            // Special modes such as ConveyorBelt may already start in PLAYING.
            setWorldCamera(battleCameraX, battleCameraY);
            toolsStack.setVisible(true);

            if (worldEntities != null) {
                worldEntities.preloadPlants(controller.getSelectedPlants());
            }
        }

        worldInput = createWorldInput();
        inputMultiplexer = new InputMultiplexer(stage, worldInput);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        if (disposed || stage == null) {
            return;
        }

        float safeDelta = Math.min(Math.max(delta, 0f), MAX_DELTA);

        if (textureBank != null) {
            textureBank.update();
        }

        BaseGame.GameState state = controller.getGame().getState();

        if (cameraSliding) {
            updateCameraSlide(safeDelta);
        } else if (state == BaseGame.GameState.PLAYING) {
            float scaledDelta = safeDelta * (toolsStack == null ? 1f : toolsStack.getTimeScale());
            String log = controller.playGame(scaledDelta);

            if (toolsStack != null) {
                toolsStack.refresh();
                if (log != null && !log.isBlank()) {
                    toolsStack.setStatus(lastMeaningfulLine(log));
                }
            }

            // GameController can change screens after win/loss.
            if (App.getScreen() != this) {
                return;
            }
        } else if (toolsStack != null && state == BaseGame.GameState.PAUSE) {
            toolsStack.refresh();
        }

        ScreenUtils.clear(0.04f, 0.08f, 0.06f, 1f);

        renderMap();

        if (state == BaseGame.GameState.PLAYING || state == BaseGame.GameState.PAUSE) {
            renderGameEntities(safeDelta);
        }

        uiViewport.apply();
        stage.act(safeDelta);
        stage.draw();
    }

    /**
     * Renders world entities using the same viewport/camera as the TMX map.
     * Plant/Sun details live outside GameView.
     */
    private void renderGameEntities(float delta) {
        worldViewport.apply();
        worldCamera.update();

        // Plants, suns and the rest of the normal world entities.
        if (worldEntities != null) {
            worldEntities.render(delta);
        }

        // Projectiles use their own batch because WorldEntityRenderer
        // manages its own rendering pipeline.
        if (projectileRenderer == null || entityBatch == null) {
            return;
        }

        entityBatch.setProjectionMatrix(worldCamera.combined);

        float animationDelta =
            controller.getGame().getState() == BaseGame.GameState.PLAYING
                ? delta * (toolsStack == null ? 1f : toolsStack.getTimeScale())
                : 0f;

        entityBatch.begin();
        try {
            projectileRenderer.render(
                entityBatch,
                controller.getGame().getBullets(),
                lawnBounds,
                animationDelta
            );
        } finally {
            entityBatch.end();
        }
    }


    private void renderMap() {
        if (mapRenderer == null) {
            return;
        }

        worldViewport.apply();
        worldCamera.update();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
    }

    private void buildPreparationUI() {
        preparationRoot = new Table();
        preparationRoot.setFillParent(true);
        preparationRoot.left().top();
        preparationRoot.pad(18f);

        Table panel = new Table();
        panel.top().left();
        panel.pad(14f);

        Drawable background = safeDrawable("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            panel.setBackground(background);
        }

        Label title = new Label("CHOOSE YOUR PLANTS", skin, "big_outline");
        title.setAlignment(Align.center);

        selectionCountLabel = new Label("", skin, "medium_outline");
        preparationStatusLabel = new Label("", skin);
        preparationStatusLabel.setAlignment(Align.center);

        selectedSlotsTable = new Table();

        plantTable = new PlantTable(
            controller.getSelectablePlants(),
            textureBank,
            skin,
            4,
            new PlantTable.Adapter() {
                @Override
                public boolean isSelected(PlantType type) {
                    return controller.isPlantSelected(type);
                }

                @Override
                public boolean isEnabled(PlantType type) {
                    return controller.isPlantSelected(type) || controller.canSelectAnotherPlant();
                }

                @Override
                public String detail(PlantType type) {
                    if (controller.isPlantSelected(type)) {
                        return "SELECTED";
                    }

                    SeedPackage preview = controller.getPlantPreview(type);
                    return preview == null ? "" : "SUN " + (int) preview.getCost();
                }

                @Override
                public void clicked(PlantType type) {
                    String result = controller.isPlantSelected(type)
                        ? controller.removePlant(type)
                        : controller.addPlant(type);

                    setPreparationStatus(result);
                    refreshPreparationUI();
                }
            }
        );

        ScrollPane scrollPane = new ScrollPane(plantTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        letsRockButton = new TextButton("LET'S ROCK!", skin, "green");
        letsRockButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!controller.canStartGame()) {
                    setPreparationStatus(
                        "Select exactly " + REQUIRED_SELECTION_COUNT + " plants first."
                    );
                    refreshPreparationUI();
                    return;
                }

                String result = controller.startGame();
                setPreparationStatus(result);

                if (controller.getGame().getState() == BaseGame.GameState.PLAYING) {
                    if (worldEntities != null) {
                        worldEntities.preloadPlants(controller.getSelectedPlants());
                    }

                    beginBattleCameraSlide();
                }
            }
        });

        TextButton backButton = new TextButton("BACK", skin, "brown");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new PlayView());
            }
        });

        panel.add(title).colspan(2).expandX().center().padBottom(8f).row();
        panel.add(selectionCountLabel).left();
        panel.add(backButton).right().width(110f).height(48f).row();
        panel.add(selectedSlotsTable).colspan(2).left().padTop(8f).padBottom(10f).row();
        panel.add(scrollPane).colspan(2).width(565f).height(430f).left().row();
        panel.add(preparationStatusLabel).colspan(2).expandX().fillX().padTop(8f).row();
        panel.add(letsRockButton).colspan(2).width(230f).height(64f).center().padTop(8f);

        // Only the left part is occupied by UI. The right side remains the visible
        // zombie staging side of the Tiled map, exactly as requested.
        preparationRoot.add(panel).width(610f).top().left();
        preparationRoot.add().expand();

        stage.addActor(preparationRoot);
        refreshPreparationUI();
    }

    private void refreshPreparationUI() {
        if (selectedSlotsTable == null) {
            return;
        }

        List<PlantType> selected = controller.getSelectedPlants();

        selectedSlotsTable.clearChildren();
        for (int i = 0; i < REQUIRED_SELECTION_COUNT; i++) {
            if (i < selected.size()) {
                PlantType type = selected.get(i);
                TextButton slot = new TextButton(shortPlantName(type), skin, "green_small");
                slot.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        setPreparationStatus(controller.removePlant(type));
                        refreshPreparationUI();
                    }
                });
                selectedSlotsTable.add(slot).size(102f, 58f).padRight(5f);
            } else {
                TextButton empty = new TextButton("EMPTY", skin, "brown");
                empty.setDisabled(true);
                selectedSlotsTable.add(empty).size(102f, 58f).padRight(5f);
            }
        }

        selectionCountLabel.setText(
            "SELECTED: " + selected.size() + "/" + REQUIRED_SELECTION_COUNT
        );

        letsRockButton.setDisabled(!controller.canStartGame());
        plantTable.refresh();
    }

    private void beginBattleCameraSlide() {
        if (preparationRoot != null) {
            preparationRoot.setTouchable(Touchable.disabled);
            preparationRoot.setVisible(false);
        }

        toolsStack.setVisible(false);

        cameraSliding = true;
        cameraSlideElapsed = 0f;
        cameraSlideFromX = worldCamera.position.x;
        cameraSlideFromY = worldCamera.position.y;
    }

    private void updateCameraSlide(float delta) {
        cameraSlideElapsed += delta;
        float alpha = Math.min(1f, cameraSlideElapsed / CAMERA_SLIDE_DURATION);
        float smooth = Interpolation.smooth.apply(alpha);

        float x = MathUtils.lerp(cameraSlideFromX, battleCameraX, smooth);
        float y = MathUtils.lerp(cameraSlideFromY, battleCameraY, smooth);
        setWorldCamera(x, y);

        if (alpha >= 1f) {
            cameraSliding = false;
            toolsStack.setVisible(true);
            toolsStack.refresh();
        }
    }

    private InputAdapter createWorldInput() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    BaseGame.GameState state = controller.getGame().getState();
                    if (state == BaseGame.GameState.PLAYING || state == BaseGame.GameState.PAUSE) {
                        controller.togglePause();
                        toolsStack.refresh();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT
                    || cameraSliding
                    || controller.getGame().getState() != BaseGame.GameState.PLAYING) {
                    return false;
                }

                pointerWorld.set(screenX, screenY);
                worldViewport.unproject(pointerWorld);

                if (toolsStack.getInteractionMode() == ToolsStack.InteractionMode.NORMAL
                    && worldEntities != null) {
                    Sun clickedSun = worldEntities.hitTestSun(
                        pointerWorld.x,
                        pointerWorld.y
                    );

                    if (clickedSun != null) {
                        String result = controller.collectSun(clickedSun);
                        if (result != null && !result.isBlank()) {
                            toolsStack.setStatus(result);
                        }
                        toolsStack.refresh();
                        return true;
                    }
                }


                if (!lawnBounds.contains(pointerWorld)) {
                    return false;
                }

                int column = (int) ((pointerWorld.x - lawnBounds.x) / (lawnBounds.width / 9f));
                int rowFromBottom = (int) ((pointerWorld.y - lawnBounds.y) / (lawnBounds.height / 5f));

                column = MathUtils.clamp(column, 0, 8);
                rowFromBottom = MathUtils.clamp(rowFromBottom, 0, 4);

                // The model stores row 0 as the first line. For an orthogonal Tiled map
                // drawn bottom-up, this is the direct row index. If your TMX visually puts
                // row 0 at the top, flip this one line to: int row = 4 - rowFromBottom;
                int row = rowFromBottom;

                handleWorldClick(column, row);
                return true;
            }
        };
    }

    private void handleWorldClick(int column, int row) {
        ToolsStack.InteractionMode mode = toolsStack.getInteractionMode();

        switch (mode) {
            case PLANT -> {
                PlantType type = toolsStack.getSelectedPlant();
                if (type == null) {
                    toolsStack.finishWorldAction();
                    return;
                }

                int before = controller.getGame().getPlantsInField().size();
                boolean couldPlant = controller.plant(type, column, row);
                String result = couldPlant ? "Planted successfully." : "Plant failed.";
                int after = controller.getGame().getPlantsInField().size();

                toolsStack.setStatus(result);
                if (after > before) {
                    toolsStack.finishWorldAction();
                }
            }

            case SHOVEL -> {
                toolsStack.setStatus(controller.pluck(column, row));
                toolsStack.finishWorldAction();
            }

            case PLANT_FOOD -> {
                int before = controller.getGame().getPlantFoodsCount();
                String result = controller.boost(column, row);
                int after = controller.getGame().getPlantFoodsCount();

                toolsStack.setStatus(result);
                if (after < before) {
                    toolsStack.finishWorldAction();
                }
            }

            case NORMAL -> {
                // Sun collection is handled by world-coordinate hit testing in touchDown().
            }
        }

        toolsStack.refresh();
    }

    private void loadTiledMap() {
        String mapPath = resolveMapPath();
        if (mapPath == null) {
            Gdx.app.error(
                "GameView",
                "No TMX map found for " + chapter + " level " + level.getId()
                    + ". Put the map under pvz2/assets/maps/."
            );
            return;
        }

        try {
            tiledMap = new TmxMapLoader().load(mapPath);
            mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);
        } catch (RuntimeException e) {
            tiledMap = null;
            mapRenderer = null;
            Gdx.app.error("GameView", "Failed to load TMX map: " + mapPath, e);
        }
    }

    private String resolveMapPath() {
        return chapter.name().toLowerCase() + ".tmx";
    }

    private void configureMapGeometry() {
        if (tiledMap != null) {
            MapProperties properties = tiledMap.getProperties();

            float mapWidthTiles = numberProperty(properties, "width", 20f);
            float mapHeightTiles = numberProperty(properties, "height", 12f);
            float tileWidth = numberProperty(properties, "tilewidth", 64f);
            float tileHeight = numberProperty(properties, "tileheight", 64f);

            mapPixelWidth = Math.max(VIRTUAL_WIDTH, mapWidthTiles * tileWidth);
            mapPixelHeight = Math.max(VIRTUAL_HEIGHT, mapHeightTiles * tileHeight);

            // Exact playable 9x5 rectangle from TMX:
            // object layer "map", rectangle object "pitch".
            lawnBounds.set(PitchBoundsReader.read(tiledMap));

            battleCameraX = numberProperty(
                properties,
                "battleCameraX",
                lawnBounds.x + lawnBounds.width / 2f
            );
            battleCameraY = numberProperty(
                properties,
                "battleCameraY",
                lawnBounds.y + lawnBounds.height / 2f
            );

            startCameraX = numberProperty(
                properties,
                "startCameraX",
                mapPixelWidth - VIRTUAL_WIDTH / 2f
            );
            startCameraY = numberProperty(properties, "startCameraY", mapPixelHeight / 2f);
        } else {
            // Keep the screen usable if the TMX itself could not be loaded,
            // but do not invent gameplay geometry.
            lawnBounds.set(0f, 0f, 0f, 0f);
            battleCameraX = VIRTUAL_WIDTH / 2f;
            battleCameraY = VIRTUAL_HEIGHT / 2f;
            startCameraX = mapPixelWidth - VIRTUAL_WIDTH / 2f;
            startCameraY = mapPixelHeight / 2f;
        }

        battleCameraX = clampCameraX(battleCameraX);
        battleCameraY = clampCameraY(battleCameraY);
        startCameraX = clampCameraX(startCameraX);
        startCameraY = clampCameraY(startCameraY);
    }

    private float numberProperty(MapProperties properties, String key, float fallback) {
        if (properties == null) {
            return fallback;
        }

        Object value = properties.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }

        if (value != null) {
            try {
                return Float.parseFloat(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        return fallback;
    }

    private void setWorldCamera(float x, float y) {
        worldCamera.position.set(clampCameraX(x), clampCameraY(y), 0f);
        worldCamera.update();
    }

    private float clampCameraX(float x) {
        float half = VIRTUAL_WIDTH / 2f;
        if (mapPixelWidth <= VIRTUAL_WIDTH) {
            return mapPixelWidth / 2f;
        }
        return MathUtils.clamp(x, half, mapPixelWidth - half);
    }

    private float clampCameraY(float y) {
        float half = VIRTUAL_HEIGHT / 2f;
        if (mapPixelHeight <= VIRTUAL_HEIGHT) {
            return mapPixelHeight / 2f;
        }
        return MathUtils.clamp(y, half, mapPixelHeight - half);
    }

    private void initialisePvzAssets() {
        pvzAssetsRoot = findPvzAssetsRoot();
        if (pvzAssetsRoot == null) {
            Gdx.app.log(
                "GameView",
                "PVZ extracted assets were not found; PlantTable will use text cards."
            );
            return;
        }

        try {
            textureBank = new TextureBank(PVZ_ASSET_RESOLUTION, pvzAssetsRoot);
        } catch (RuntimeException e) {
            textureBank = null;
            Gdx.app.error("GameView", "Failed to initialise libPVZ TextureBank", e);
        }
    }

    private void initialiseWorldEntities() {
        if (lawnBounds.width <= 0f || lawnBounds.height <= 0f) {
            Gdx.app.error(
                "GameView",
                "World entity rendering disabled: TMX pitch bounds are invalid."
            );
            return;
        }

        worldEntities = new WorldEntityRenderer(
            worldViewport,
            controller,
            lawnBounds,
            pvzAssetsRoot,
            textureBank,
            safeDrawable("image_ui_hud_ingame_sun")
        );
    }


    private FileHandle findPvzAssetsRoot() {
        List<FileHandle> roots = new ArrayList<>();
        String configured = System.getProperty("pvz.assets");

        if (configured != null && !configured.isBlank()) {
            roots.add(new FileHandle(new File(configured)));
        }

        // Desktop run working directory in this project is pvz2/assets, therefore
        // ../../Assets is included for the sibling external asset folder described by the team.
        String[] developmentPaths = {
            "Assets",
            "../Assets",
            "../../Assets",
            "pvz-assets",
            "../pvz-assets",
            "../../pvz-assets"
        };

        for (String path : developmentPaths) {
            roots.add(new FileHandle(new File(path)));
        }

        roots.add(Gdx.files.internal("pvz-assets"));

        for (FileHandle candidate : roots) {
            FileHandle resolved = resolvePvzAssetRoot(candidate);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    private FileHandle resolvePvzAssetRoot(FileHandle root) {
        if (root == null || !root.exists()) {
            return null;
        }

        if (isPvzAssetRoot(root)) {
            return root;
        }

        String[] possibleChildren = {
            "Base Assets",
            "base assets",
            "BaseAssets",
            "pvz-assets",
            "assets"
        };

        for (String childName : possibleChildren) {
            FileHandle child = root.child(childName);
            if (isPvzAssetRoot(child)) {
                return child;
            }
        }

        try {
            for (FileHandle child : root.list()) {
                if (child.isDirectory() && isPvzAssetRoot(child)) {
                    return child;
                }
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }

    private boolean isPvzAssetRoot(FileHandle root) {
        if (root == null || !root.exists()) {
            return false;
        }

        boolean hasResources = root.child("resources.json").exists()
            || root.child("RESOURCES.json").exists();

        boolean hasAtlases = root.child("atlases").exists()
            || root.child("ATLASES").exists();

        return hasResources && hasAtlases;
    }

    private Drawable safeDrawable(String name) {
        try {
            return skin.getDrawable(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void setPreparationStatus(String text) {
        if (preparationStatusLabel != null) {
            preparationStatusLabel.setText(text == null ? "" : text);
        }
    }

    private static String chapterSlug(Chapters chapter) {
        return switch (chapter) {
            case AncientEgypt -> "ancient-egypt";
            case FrozenCaves -> "frozen-caves";
            case BigWaveBeach -> "big-wave-beach";
            case DarkAge -> "dark-age";
        };
    }

    private static String shortPlantName(PlantType type) {
        if (type == null) {
            return "";
        }

        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1));
        }

        return result.toString();
    }

    private static String lastMeaningfulLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i].trim();
            }
        }
        return "";
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
    public void pause() {
        if (controller.getGame().getState() == BaseGame.GameState.PLAYING) {
            appPausedGame = true;
            controller.pauseGame();
        }
    }

    @Override
    public void resume() {
        if (appPausedGame && controller.getGame().getState() == BaseGame.GameState.PAUSE) {
            controller.resumeGame();
        }
        appPausedGame = false;
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
            mapRenderer = null;
        }

        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }


        if (worldEntities != null) {
            worldEntities.dispose();
            worldEntities = null;
        }

        if (projectileRenderer != null) {
            projectileRenderer.dispose();
            projectileRenderer = null;
        }

        if (entityBatch != null) {
            entityBatch.dispose();
            entityBatch = null;
        }
        if (textureBank != null) {
            textureBank.dispose();
            textureBank = null;
        }
    }
}

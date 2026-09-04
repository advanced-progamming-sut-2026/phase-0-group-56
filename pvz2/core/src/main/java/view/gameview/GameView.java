package view.gameview;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import models.entity.Plant;
import models.entity.Sun;
import models.entity.RewardDrop;
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
import controllers.datacontroller.Data;
import controllers.datacontroller.SeedPackage;
import controllers.menus.gamecontroller.GameController;
import models.App;
import models.Constants;
import models.factory.builder.PlantType;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import models.games.BaseGame;
import models.utils.AudioManager;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import view.PlayView;
import view.View;
import view.components.PlantTable;

import java.io.File;
import java.util.ArrayDeque;
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
    private static final int REQUIRED_SELECTION_COUNT = Constants.PLANTS_COUNT_IN_A_GAME;
    private static final String PVZ_ASSET_RESOLUTION = "768";
    private static final String SHOVEL_CURSOR_ID = "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";

    private final Chapters chapter;
    private final Level level;
    private final GameController controller;

    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final Vector2 pointerWorld = new Vector2();
    private final Rectangle lawnBounds = new Rectangle();
    private int hoverColumn = -1;
    private int hoverRow = -1;

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
    private Table levelStartOverlay;
    private boolean levelStartNeedsPreparation;
    private boolean levelStartPausedGame;
    private Table selectedSlotsTable;
    private Label selectionCountLabel;
    private Label preparationStatusLabel;
    private TextButton letsRockButton;
    private PlantTable plantTable;
    private CrazyDaveIntro crazyDaveIntro;
    private boolean crazyDaveIntroActive;

    private ToolsStack toolsStack;
    private Table pauseOverlay;
    private Table announcementOverlay;
    private Label announcementLabel;
    private Table outcomeOverlay;
    private Label outcomeLabel;
    private final ArrayDeque<String> announcementQueue = new ArrayDeque<>();
    private String activeAnnouncement = "";
    private float announcementRemaining;
    private static final float ANNOUNCEMENT_DURATION = 1.8f;

    private FileHandle pvzAssetsRoot;
    private TextureBank textureBank;
    private SpriteBatch entityBatch;
    private ShapeRenderer interactionShapes;
    private ProjectileRenderer projectileRenderer;
    private WorldEntityRenderer worldEntities;

    // Cursor/placement feedback. Kept visual-only; none of these objects enter BaseGame.
    private PlantRenderer cursorPlantRenderer;
    private CursorPreviewPlant cursorPlant;
    private PlantType cursorPlantType;
    private TextureRegion shovelCursorRegion;
    private boolean customCursorHidden;

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
        interactionShapes = new ShapeRenderer();
        initialiseInteractionCursorAssets();
        if (pvzAssetsRoot != null && textureBank != null) {
            projectileRenderer = new ProjectileRenderer(pvzAssetsRoot, textureBank);
        }
        initialiseWorldEntities();

        toolsStack = new ToolsStack(controller, textureBank);
        toolsStack.setVisible(false);
        stage.addActor(toolsStack);
        buildPauseOverlay();
        buildAnnouncementOverlay();
        buildOutcomeOverlay();

        levelStartNeedsPreparation =
            controller.getGame().getState() == BaseGame.GameState.STARTING;

        if (levelStartNeedsPreparation) {
            buildPreparationUI();
            setWorldCamera(startCameraX, startCameraY);
        } else {
            // Special modes such as ConveyorBelt may already start in PLAYING.
            levelStartPausedGame =
                controller.getGame().getState() == BaseGame.GameState.PLAYING;
            if (levelStartPausedGame) {
                controller.pauseGame();
            }
            setWorldCamera(battleCameraX, battleCameraY);
            toolsStack.setVisible(false);

            if (worldEntities != null) {
                worldEntities.preloadPlants(controller.getSelectedPlants());
            }
        }

        buildLevelStartMenu();

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
        if (cursorPlantRenderer != null) {
            cursorPlantRenderer.update();
        }

        BaseGame.GameState state = controller.getGame().getState();
        syncPauseOverlay(state);
        syncOutcomeOverlay(state);
        updatePointerFromScreen(Gdx.input.getX(), Gdx.input.getY());
        updateCursorPlant(safeDelta, state);
        syncSystemCursor(state);

        if (cameraSliding) {
            updateCameraSlide(safeDelta);
        } else if (state == BaseGame.GameState.PLAYING && !crazyDaveIntroActive) {
            float scaledDelta = safeDelta * (toolsStack == null ? 1f : toolsStack.getTimeScale());
            String log = controller.playGame(scaledDelta);

            if (toolsStack != null) {
                toolsStack.refresh();
                if (log != null && !log.isBlank()) {
                    toolsStack.setStatus(lastMeaningfulLine(log));
                }
            }
            queueGameAnnouncements();
            syncOutcomeOverlay(controller.getGame().getState());
        } else if (toolsStack != null && state == BaseGame.GameState.PAUSE) {
            toolsStack.refresh();
        }

        updateAnnouncementOverlay(safeDelta);
        syncOutcomeOverlay(controller.getGame().getState());

        ScreenUtils.clear(0.04f, 0.08f, 0.06f, 1f);

        renderMap();

        if (state == BaseGame.GameState.PLAYING && !crazyDaveIntroActive) {
            renderPlacementHighlight();
        }

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

        // Projectiles and the custom interaction cursor use this batch because
        // WorldEntityRenderer manages its own Stage/batch internally.
        if (entityBatch == null) {
            return;
        }

        entityBatch.setProjectionMatrix(worldCamera.combined);

        float animationDelta =
            controller.getGame().getState() == BaseGame.GameState.PLAYING
                ? delta * (toolsStack == null ? 1f : toolsStack.getTimeScale())
                : 0f;

        entityBatch.begin();
        try {
            if (projectileRenderer != null) {
                projectileRenderer.render(
                    entityBatch,
                    controller.getGame().getBullets(),
                    lawnBounds,
                    animationDelta
                );
            }

            renderInteractionCursor(entityBatch);
        } finally {
            entityBatch.setColor(Color.WHITE);
            entityBatch.end();
        }
    }

    /**
     * VaseBreaker-style translucent row + column feedback for plant/shovel placement.
     * It is rendered after the TMX map and before world entities, so plants/zombies
     * stay visually above it.
     */
    private void renderPlacementHighlight() {
        if (interactionShapes == null
            || toolsStack == null
            || hoverColumn < 0
            || hoverRow < 0
            || lawnBounds.width <= 0f
            || lawnBounds.height <= 0f) {
            return;
        }

        ToolsStack.InteractionMode mode = toolsStack.getInteractionMode();
        if (mode != ToolsStack.InteractionMode.PLANT
            && mode != ToolsStack.InteractionMode.SHOVEL) {
            return;
        }

        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float columnX = lawnBounds.x + hoverColumn * cellWidth;
        float rowY = lawnBounds.y + hoverRow * cellHeight;

        worldViewport.apply();
        worldCamera.update();
        interactionShapes.setProjectionMatrix(worldCamera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        interactionShapes.begin(ShapeRenderer.ShapeType.Filled);
        try {
            interactionShapes.setColor(1f, 1f, 1f, 0.12f);
            interactionShapes.rect(
                columnX,
                lawnBounds.y,
                cellWidth,
                lawnBounds.height
            );
            interactionShapes.rect(
                lawnBounds.x,
                rowY,
                lawnBounds.width,
                cellHeight
            );

            interactionShapes.setColor(1f, 1f, 1f, 0.16f);
            interactionShapes.rect(
                columnX,
                rowY,
                cellWidth,
                cellHeight
            );
        } finally {
            interactionShapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    /**
     * Draws the idle plant animation or shovel image at the current mouse position.
     */
    private void renderInteractionCursor(Batch batch) {
        if (batch == null
            || toolsStack == null
            || cameraSliding
            || controller.getGame().getState() != BaseGame.GameState.PLAYING) {
            return;
        }

        ToolsStack.InteractionMode mode = toolsStack.getInteractionMode();
        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;

        if (mode == ToolsStack.InteractionMode.PLANT
            && cursorPlant != null
            && cursorPlantRenderer != null) {

            Color old = batch.getColor().cpy();
            batch.setColor(old.r, old.g, old.b, 0.60f);
            try {
                cursorPlantRenderer.render(
                    batch,
                    cursorPlant,
                    pointerWorld.x,
                    pointerWorld.y,
                    cellWidth,
                    cellHeight
                );
            } finally {
                batch.setColor(old);
            }
            return;
        }

        if (mode == ToolsStack.InteractionMode.SHOVEL
            && shovelCursorRegion != null) {

            float width = 72f;
            float height = width
                * shovelCursorRegion.getRegionHeight()
                / (float) shovelCursorRegion.getRegionWidth();

            batch.draw(
                shovelCursorRegion,
                pointerWorld.x - width * 0.22f,
                pointerWorld.y - height * 0.72f,
                width,
                height
            );
        }
    }

    private void buildPauseOverlay() {
        pauseOverlay = new Table();
        pauseOverlay.setFillParent(true);
        pauseOverlay.center();
        pauseOverlay.setTouchable(Touchable.enabled);
        pauseOverlay.setVisible(false);

        Drawable background = safeDrawable("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            pauseOverlay.setBackground(background);
        }

        Table panel = new Table();
        panel.center().pad(26f);

        Label title = new Label("GAME PAUSED", skin, "big_outline");
        title.setAlignment(Align.center);
        panel.add(title).width(420f).padBottom(20f).row();

        TextButton resume = new TextButton("RESUME", skin, "green");
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.resumeGame();
                syncPauseOverlay(controller.getGame().getState());
                toolsStack.refresh();
            }
        });

        TextButton restart = new TextButton("RESTART LEVEL", skin, "brown");
        restart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new GameView(chapter, level));
            }
        });

        TextButton saveAndExit = new TextButton("SAVE & EXIT", skin, "green");
        saveAndExit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Data.saveUser();
                App.setScreen(new PlayView());
            }
        });

        TextButton exit = new TextButton("EXIT TO ADVENTURE", skin, "brown");
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Data.saveUser();
                App.setScreen(new PlayView());
            }
        });

        panel.add(resume).width(300f).height(52f).pad(5f).row();
        panel.add(restart).width(300f).height(52f).pad(5f).row();
        panel.add(saveAndExit).width(300f).height(52f).pad(5f).row();
        panel.add(exit).width(300f).height(52f).pad(5f).row();

        pauseOverlay.add(panel).center();
        stage.addActor(pauseOverlay);
    }

    private void buildAnnouncementOverlay() {
        announcementOverlay = new Table();
        announcementOverlay.setFillParent(true);
        announcementOverlay.center();
        announcementOverlay.setTouchable(Touchable.disabled);
        announcementOverlay.setVisible(false);

        announcementLabel = new Label("", skin, "big_outline");
        announcementLabel.setAlignment(Align.center);
        announcementLabel.setWrap(true);
        announcementLabel.setColor(new Color(0.95f, 0.04f, 0.04f, 1f));
        announcementOverlay.add(announcementLabel).width(980f).pad(20f);
        stage.addActor(announcementOverlay);
    }

    private void buildOutcomeOverlay() {
        outcomeOverlay = new Table();
        outcomeOverlay.setFillParent(true);
        outcomeOverlay.center();
        outcomeOverlay.setTouchable(Touchable.disabled);
        outcomeOverlay.setVisible(false);

        Table card = new Table();
        card.pad(28f);
        Drawable background = safeDrawable("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }

        outcomeLabel = new Label("", skin, "big_outline");
        outcomeLabel.setAlignment(Align.center);
        outcomeLabel.setWrap(true);
        card.add(outcomeLabel).colspan(2).width(460f).padBottom(18f).row();

        TextButton retry = new TextButton("RETRY", skin, "green");
        retry.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new GameView(chapter, level));
            }
        });
        TextButton exit = new TextButton("EXIT TO ADVENTURE", skin, "brown");
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Data.saveUser();
                App.setScreen(new PlayView());
            }
        });
        card.add(retry).width(190f).height(58f).pad(5f);
        card.add(exit).width(230f).height(58f).pad(5f);
        outcomeOverlay.add(card).center();
        stage.addActor(outcomeOverlay);
    }

    private void queueGameAnnouncements() {
        enqueueAnnouncement(controller.getLastWaveAnnouncement());
        enqueueAnnouncement(controller.getLastEventAnnouncement());
    }

    private void enqueueAnnouncement(String announcement) {
        if (announcement == null || announcement.isBlank()) {
            return;
        }
        String text = announcement.trim();
        if (text.equals(activeAnnouncement) || announcementQueue.contains(text)) {
            return;
        }
        announcementQueue.addLast(text);
    }

    private void updateAnnouncementOverlay(float delta) {
        if (announcementOverlay == null || announcementLabel == null) {
            return;
        }
        if (announcementRemaining > 0f) {
            announcementRemaining -= delta;
        }
        if (announcementRemaining <= 0f && !announcementQueue.isEmpty()) {
            activeAnnouncement = announcementQueue.removeFirst();
            announcementLabel.setText(activeAnnouncement);
            announcementOverlay.setVisible(true);
            announcementRemaining = ANNOUNCEMENT_DURATION;
        } else if (announcementRemaining <= 0f) {
            activeAnnouncement = "";
            announcementLabel.setText("");
            announcementOverlay.setVisible(false);
        }
    }

    private void syncOutcomeOverlay(BaseGame.GameState state) {
        if (outcomeOverlay == null || outcomeLabel == null) {
            return;
        }
        boolean ended = state == BaseGame.GameState.END && controller.isResultHandled();
        outcomeOverlay.setVisible(ended);
        outcomeOverlay.setTouchable(ended ? Touchable.enabled : Touchable.disabled);
        if (ended) {
            boolean won = controller.isWon();
            outcomeLabel.setText(won ? "YOU WIN!" : "YOU LOSE!");
            outcomeLabel.setColor(
                won
                    ? new Color(0.20f, 0.95f, 0.22f, 1f)
                    : new Color(0.95f, 0.04f, 0.04f, 1f)
            );
        }
    }

    private void syncPauseOverlay(BaseGame.GameState state) {
        if (pauseOverlay == null) {
            return;
        }

        boolean paused = state == BaseGame.GameState.PAUSE;
        pauseOverlay.setVisible(paused);
        pauseOverlay.setTouchable(paused ? Touchable.enabled : Touchable.disabled);
    }

    private void initialiseInteractionCursorAssets() {
        if (pvzAssetsRoot != null) {
            try {
                cursorPlantRenderer = new PlantRenderer(pvzAssetsRoot);
                for (PlantType type : controller.getSelectedPlants()) {
                    if (type != null) {
                        cursorPlantRenderer.preload(type);
                    }
                }
            } catch (RuntimeException e) {
                cursorPlantRenderer = null;
                Gdx.app.error(
                    "GameView",
                    "Failed to initialise plant cursor preview renderer.",
                    e
                );
            }
        }

        if (textureBank != null) {
            try {
                shovelCursorRegion = textureBank.region(SHOVEL_CURSOR_ID);
                if (shovelCursorRegion == null) {
                    Gdx.app.error(
                        "GameView",
                        "Shovel cursor asset was not found: " + SHOVEL_CURSOR_ID
                    );
                }
            } catch (RuntimeException e) {
                shovelCursorRegion = null;
                Gdx.app.error(
                    "GameView",
                    "Failed to load shovel cursor asset: " + SHOVEL_CURSOR_ID,
                    e
                );
            }
        }
    }

    private void updateCursorPlant(float delta, BaseGame.GameState state) {
        if (toolsStack == null
            || crazyDaveIntroActive
            || cameraSliding
            || state != BaseGame.GameState.PLAYING
            || toolsStack.getInteractionMode() != ToolsStack.InteractionMode.PLANT) {
            cursorPlant = null;
            cursorPlantType = null;
            return;
        }

        PlantType selected = toolsStack.getSelectedPlant();
        if (selected == null) {
            cursorPlant = null;
            cursorPlantType = null;
            return;
        }

        if (cursorPlant == null || cursorPlantType != selected) {
            cursorPlantType = selected;
            cursorPlant = new CursorPreviewPlant(selected);

            if (cursorPlantRenderer != null) {
                cursorPlantRenderer.preload(selected);
            }
        }

        float timeScale = toolsStack.getTimeScale();
        cursorPlant.advance(Math.max(0f, delta) * timeScale);
    }

    /**
     * Updates both the world-space pointer and the hovered 9x5 tile.
     */
    private void updatePointerFromScreen(int screenX, int screenY) {
        if (worldViewport == null) {
            hoverColumn = -1;
            hoverRow = -1;
            return;
        }

        pointerWorld.set(screenX, screenY);
        worldViewport.unproject(pointerWorld);

        if (lawnBounds.width <= 0f
            || lawnBounds.height <= 0f
            || !lawnBounds.contains(pointerWorld)) {
            hoverColumn = -1;
            hoverRow = -1;
            return;
        }

        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;

        hoverColumn = MathUtils.clamp(
            (int) ((pointerWorld.x - lawnBounds.x) / cellWidth),
            0,
            8
        );
        hoverRow = MathUtils.clamp(
            (int) ((pointerWorld.y - lawnBounds.y) / cellHeight),
            0,
            4
        );
    }

    private void syncSystemCursor(BaseGame.GameState state) {
        boolean customVisualAvailable = false;

        if (state == BaseGame.GameState.PLAYING
            && !crazyDaveIntroActive
            && !cameraSliding
            && toolsStack != null) {

            ToolsStack.InteractionMode mode = toolsStack.getInteractionMode();
            customVisualAvailable =
                (mode == ToolsStack.InteractionMode.PLANT
                    && cursorPlant != null
                    && cursorPlantRenderer != null)
                    || (mode == ToolsStack.InteractionMode.SHOVEL
                    && shovelCursorRegion != null);
        }

        if (customVisualAvailable == customCursorHidden) {
            return;
        }

        try {
            Gdx.graphics.setSystemCursor(
                customVisualAvailable
                    ? Cursor.SystemCursor.None
                    : Cursor.SystemCursor.Arrow
            );
            customCursorHidden = customVisualAvailable;
        } catch (RuntimeException ignored) {
            customCursorHidden = false;
        }
    }

    private void restoreSystemCursor() {
        try {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        } catch (RuntimeException ignored) {
        }
        customCursorHidden = false;
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
                    if (shouldShowCrazyDaveIntro()) {
                        beginCrazyDaveIntro();
                    } else {
                        beginBattleCameraSlide();
                    }
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
        // Keep the action button visible after expanding the selection to
        // eight plants (the selected slots occupy two rows).
        panel.add(scrollPane).colspan(2).width(565f).height(315f).left().row();
        panel.add(preparationStatusLabel).colspan(2).expandX().fillX().padTop(8f).row();
        panel.add(letsRockButton).colspan(2).width(230f).height(64f).center().padTop(8f);

        // Only the left part is occupied by UI. The right side remains the visible
        // zombie staging side of the Tiled map, exactly as requested.
        preparationRoot.add(panel).width(610f).top().left();
        preparationRoot.add().expand();

        stage.addActor(preparationRoot);
        refreshPreparationUI();
    }

    /**
     * Shows the level objectives before the plant-selection screen.
     *
     * <p>The underlying game is still in STARTING state while this panel is
     * visible. Consequently no wave, timer, or gameplay update can begin
     * until the player explicitly continues and presses LET'S ROCK.</p>
     */
    private void buildLevelStartMenu() {
        levelStartOverlay = new Table();
        levelStartOverlay.setFillParent(true);
        levelStartOverlay.center();
        levelStartOverlay.setTouchable(Touchable.enabled);
        levelStartOverlay.setBackground(
            solidDrawable(new Color(0.015f, 0.035f, 0.045f, 0.78f))
        );

        // Use a dedicated outer frame so the level briefing remains visibly
        // boxed even when a skin drawable is unavailable. The inner PvZ
        // panel keeps the normal dialog artwork and padding.
        Table frame = new Table();
        frame.setBackground(
            solidDrawable(new Color(0.01f, 0.025f, 0.035f, 0.98f))
        );
        frame.pad(7f);

        Table card = pvzPanel();
        card.center();

        Label title = new Label("LEVEL " + level.getId(), skin, "big_outline");
        title.setAlignment(Align.center);

        Label chapterLabel = new Label(
            humanizeLevelText(chapter.name()),
            skin,
            "medium_outline"
        );
        chapterLabel.setAlignment(Align.center);

        Label missionTitle = new Label("MISSION", skin, "medium_outline");
        missionTitle.setAlignment(Align.left);

        Label missionLabel = new Label(buildLevelMissionText(), skin);
        missionLabel.setWrap(true);
        missionLabel.setAlignment(Align.left);

        card.add(title)
            .width(650f)
            .center()
            .padBottom(2f)
            .row();
        card.add(chapterLabel)
            .width(650f)
            .center()
            .padBottom(18f)
            .row();
        card.add(missionTitle)
            .width(650f)
            .left()
            .padBottom(7f)
            .row();
        card.add(missionLabel)
            .width(650f)
            .minHeight(150f)
            .left()
            .padBottom(18f)
            .row();

        TextButton backButton = new TextButton("BACK", skin, "brown");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new PlayView());
            }
        });

        TextButton continueButton = new TextButton("CONTINUE", skin, "green");
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dismissLevelStartMenu();
            }
        });

        card.add(backButton)
            .width(180f)
            .height(58f)
            .padRight(10f);
        card.add(continueButton)
            .width(220f)
            .height(58f);

        frame.add(card)
            .width(760f)
            .minHeight(420f)
            .center();

        levelStartOverlay.add(frame)
            .width(774f)
            .center();

        if (preparationRoot != null) {
            preparationRoot.setVisible(false);
            preparationRoot.setTouchable(Touchable.disabled);
        }

        stage.addActor(levelStartOverlay);
    }

    private void dismissLevelStartMenu() {
        if (levelStartOverlay == null) {
            return;
        }

        levelStartOverlay.setVisible(false);
        levelStartOverlay.setTouchable(Touchable.disabled);

        if (levelStartNeedsPreparation && preparationRoot != null) {
            preparationRoot.setVisible(true);
            preparationRoot.setTouchable(Touchable.enabled);
            preparationRoot.toFront();
            return;
        }

        if (levelStartPausedGame
            && controller.getGame().getState() == BaseGame.GameState.PAUSE) {
            controller.resumeGame();
            levelStartPausedGame = false;
        }

        if (toolsStack != null) {
            toolsStack.setVisible(true);
        }

        if (shouldShowCrazyDaveIntro()) {
            beginCrazyDaveIntro();
        }
    }

    private String buildLevelMissionText() {
        String type = level.getLevelType() == null
            ? "normal"
            : level.getLevelType().trim().toLowerCase(Locale.ROOT);

        StringBuilder mission = new StringBuilder();
        mission.append("• Defeat all zombies and survive ")
            .append(Math.max(1, level.getWaves()))
            .append(" waves.\n");

        switch (type) {
            case "conveyor belt" -> mission.append(
                "• Use the plants delivered on the conveyor belt.\n"
            );
            case "save our seeds" -> mission.append(
                "• Keep every protected plant alive until the battle ends.\n"
            );
            case "locked plants by category" -> mission.append(
                "• Build your defence using only the permitted plant category.\n"
            );
            case "deadline" -> mission.append(
                "• Finish the battle before the deadline expires.\n"
            );
            case "timed war" -> mission.append(
                "• Keep attacking while the timed battle is running.\n"
            );
            case "love your plants" -> mission.append(
                "• Do not lose any plant during this challenge.\n"
            );
            case "night ops" -> mission.append(
                "• There is no falling sunlight; manage your resources carefully.\n"
            );
            case "plant what you get" -> mission.append(
                "• Adapt your defence to the plants you receive during the level.\n"
            );
            default -> mission.append(
                "• Protect your lawn and stop the zombie invasion.\n"
            );
        }

        if (level.getUnlockingPlants() != null
            && !level.getUnlockingPlants().isEmpty()) {
            mission.append("\nReward: unlock ");
            for (int i = 0; i < level.getUnlockingPlants().size(); i++) {
                if (i > 0) {
                    mission.append(", ");
                }
                mission.append(shortPlantName(level.getUnlockingPlants().get(i)));
            }
            mission.append('.');
        }

        return mission.toString();
    }

    private static String humanizeLevelText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] words = value.replace('_', ' ').split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
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
            if ((i + 1) % 4 == 0) {
                selectedSlotsTable.row();
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

    private boolean shouldShowCrazyDaveIntro() {
        String type = level.getLevelType();
        return type != null && !type.trim().equalsIgnoreCase("normal");
    }

    private void beginCrazyDaveIntro() {
        if (crazyDaveIntroActive || stage == null) {
            return;
        }

        if (preparationRoot != null) {
            preparationRoot.setTouchable(Touchable.disabled);
            preparationRoot.setVisible(false);
        }
        toolsStack.setVisible(false);
        crazyDaveIntroActive = true;

        Drawable bubble = safeDrawable("image_ui_quests_panel_edge_to_edge_ten");
        crazyDaveIntro = new CrazyDaveIntro(
            pvzAssetsRoot,
            textureBank,
            skin,
            chapter,
            level,
            bubble,
            this::finishCrazyDaveIntro
        );
        crazyDaveIntro.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(crazyDaveIntro);
    }

    private void finishCrazyDaveIntro() {
        if (!crazyDaveIntroActive) {
            return;
        }
        crazyDaveIntroActive = false;
        if (crazyDaveIntro != null) {
            crazyDaveIntro.remove();
            crazyDaveIntro.dispose();
            crazyDaveIntro = null;
        }
        beginBattleCameraSlide();
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
            public boolean mouseMoved(int screenX, int screenY) {
                updatePointerFromScreen(screenX, screenY);
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                updatePointerFromScreen(screenX, screenY);
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (levelStartOverlay != null && levelStartOverlay.isVisible()) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                        dismissLevelStartMenu();
                    }
                    return true;
                }
                if (crazyDaveIntroActive) {
                    if (keycode == Input.Keys.ENTER) {
                        crazyDaveIntro.advance();
                    }
                    return true;
                }
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
                if (crazyDaveIntroActive) {
                    return true;
                }
                if (button != Input.Buttons.LEFT
                    || cameraSliding
                    || controller.getGame().getState() != BaseGame.GameState.PLAYING) {
                    return false;
                }

                updatePointerFromScreen(screenX, screenY);

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

                    RewardDrop clickedDrop = worldEntities.hitTestRewardDrop(
                        pointerWorld.x,
                        pointerWorld.y
                    );
                    if (clickedDrop != null) {
                        String result = controller.collectRewardDrop(clickedDrop);
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

        restoreSystemCursor();

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

        restoreSystemCursor();

        crazyDaveIntroActive = false;
        if (crazyDaveIntro != null) {
            crazyDaveIntro.dispose();
            crazyDaveIntro = null;
        }

        if (stage != null) {
            stage.dispose();
            stage = null;
        }

        levelStartOverlay = null;
        preparationRoot = null;

        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }

        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }
        AudioManager.getInstance().stop();


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
        if (interactionShapes != null) {
            interactionShapes.dispose();
            interactionShapes = null;
        }
        if (cursorPlantRenderer != null) {
            cursorPlantRenderer.dispose();
            cursorPlantRenderer = null;
        }
        shovelCursorRegion = null;
        cursorPlant = null;
        cursorPlantType = null;

        if (textureBank != null) {
            textureBank.dispose();
            textureBank = null;
        }
    }

    /**
     * Visual-only Plant accepted by PlantRenderer for the animated planting cursor.
     * It is never added to BaseGame.
     */
    private static final class CursorPreviewPlant extends Plant {
        private CursorPreviewPlant(PlantType type) {
            this.type = type;
            this.hp = 1f;
            this.isAlive = true;
        }

        private void advance(float delta) {
            stateTime += Math.max(0f, delta);
        }
    }
}

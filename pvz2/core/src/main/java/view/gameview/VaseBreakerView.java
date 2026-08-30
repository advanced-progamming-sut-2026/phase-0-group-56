package view.gameview;

import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controllers.menus.gamecontroller.VaseBreakerController;
import models.App;
import models.entity.Plant;
import models.factory.builder.PlantType;
import models.games.minigames.Vase;
import models.games.minigames.VaseBreakResult;
import models.games.minigames.VaseSeedDrop;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import view.MiniGamesView;
import view.View;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Graphical Vase Breaker screen. */
public final class VaseBreakerView extends View {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final float MAX_DELTA = 1f / 15f;
    private static final String ASSET_RESOLUTION = "768";
    private static final String GLOVE_ID =
        "IMAGE_UI_JOUST_JOUST_METER_ASSETS_JOUST_GLOVE_RED_JOUST_GLOVE_RED_211X138";

    private enum InteractionMode {
        NORMAL,
        PLANT,
        SHOVEL
    }

    private final VaseBreakerController controller;
    private final OrthographicCamera worldCamera = new OrthographicCamera();
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final Rectangle lawnBounds = new Rectangle();
    private final Vector2 pointerWorld = new Vector2();
    private final Vector2 pointerUi = new Vector2();
    private final ArrayList<BrokenVaseFx> brokenVases = new ArrayList<>();

    private FitViewport worldViewport;
    private FitViewport uiViewport;
    private Stage uiStage;
    private TiledMap tiledMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private InputMultiplexer inputMultiplexer;
    private InputAdapter worldInput;

    private FileHandle pvzAssetsRoot;
    private TextureBank textureBank;
    private VaseRenderer vaseRenderer;
    private PlantRenderer plantRenderer;
    private ProjectileRenderer projectileRenderer;
    private VaseBreakerZombieRenderer zombieRenderer;
    private TextureRegion gloveRegion;

    private InteractionMode mode = InteractionMode.NORMAL;
    private CursorPreviewPlant cursorPlant;
    private float cursorPlantTime;
    private float startupTime;
    private int hoverColumn = -1;
    private int hoverRow = -1;
    private Label statusLabel;
    private Label modeLabel;
    private Label resultLabel;
    private Table resultPanel;

    private Cursor blankCursor;
    private boolean disposed;

    public VaseBreakerView() {
        this(new VaseBreakerController());
    }

    public VaseBreakerView(int level) {
        this(new VaseBreakerController(level));
    }

    public VaseBreakerView(VaseBreakerController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("VaseBreakerController cannot be null.");
        }
        this.controller = controller;
    }

    @Override
    public void show() {
        disposed = false;
        skin = PvzSkin.get();

        worldViewport = new FitViewport(WIDTH, HEIGHT, worldCamera);
        uiViewport = new FitViewport(WIDTH, HEIGHT, uiCamera);
        uiStage = new Stage(uiViewport);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();

        loadMap();
        configureCamera();
        initialisePvzAssets();
        buildUi();
        installCursor();

        worldInput = createWorldInput();
        inputMultiplexer = new InputMultiplexer(uiStage, worldInput);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        if (disposed || uiStage == null) {
            return;
        }

        float safeDelta = Math.min(Math.max(delta, 0f), MAX_DELTA);
        startupTime += safeDelta;
        cursorPlantTime += safeDelta;

        if (textureBank != null) {
            textureBank.update();
            gloveRegion = textureBank.region(GLOVE_ID);
        }
        if (plantRenderer != null) {
            plantRenderer.update();
        }

        updatePointerFromScreen(Gdx.input.getX(), Gdx.input.getY());
        updateBreakEffects(safeDelta);

        String gameLog = controller.playGame(safeDelta);
        if (gameLog != null && !gameLog.isBlank()) {
            setStatus(gameLog);
        }
        refreshResultPanel();

        ScreenUtils.clear(0.04f, 0.08f, 0.06f, 1f);
        renderMap();
        renderHighlight();
        renderSeedPacketCards();
        renderWorld(safeDelta);

        uiViewport.apply();
        uiStage.act(safeDelta);
        uiStage.draw();
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

    private void renderHighlight() {
        if (shapes == null || hoverColumn < 0 || hoverRow < 0
            || (mode != InteractionMode.PLANT && mode != InteractionMode.SHOVEL)) {
            return;
        }

        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float columnX = lawnBounds.x + hoverColumn * cellWidth;
        float rowY = lawnBounds.y + hoverRow * cellHeight;

        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.12f);
        shapes.rect(columnX, lawnBounds.y, cellWidth, lawnBounds.height);
        shapes.rect(lawnBounds.x, rowY, lawnBounds.width, cellHeight);
        shapes.setColor(1f, 1f, 1f, 0.16f);
        shapes.rect(columnX, rowY, cellWidth, cellHeight);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderSeedPacketCards() {
        if (shapes == null || controller.getSeedDrops().isEmpty()) {
            return;
        }

        worldViewport.apply();
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (VaseSeedDrop drop : controller.getSeedDrops()) {
            if (drop == controller.getGame().getSelectedSeedDrop()) {
                continue;
            }
            Rectangle rect = seedDropRect(drop);
            shapes.setColor(0.12f, 0.30f, 0.10f, 0.92f);
            shapes.rect(rect.x, rect.y, rect.width, rect.height);
            shapes.setColor(0.75f, 1f, 0.60f, 0.32f);
            shapes.rect(
                rect.x + rect.width * 0.07f,
                rect.y + rect.height * 0.07f,
                rect.width * 0.86f,
                rect.height * 0.86f
            );
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderWorld(float delta) {
        if (batch == null) {
            return;
        }

        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;

        worldViewport.apply();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        try {
            renderVases(batch, cellWidth, cellHeight);
            renderSeedPacketPlants(batch, cellWidth, cellHeight);
            renderPlants(batch, cellWidth, cellHeight);

            if (zombieRenderer != null) {
                zombieRenderer.render(
                    batch,
                    controller.getGame().getZombies(),
                    lawnBounds,
                    delta
                );
            }

            if (projectileRenderer != null) {
                projectileRenderer.render(
                    batch,
                    controller.getGame().getBullets(),
                    lawnBounds,
                    delta
                );
            }

            renderBrokenVases(batch, cellWidth, cellHeight);
            renderCursor(batch, cellWidth, cellHeight);
        } finally {
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void renderVases(Batch batch, float cellWidth, float cellHeight) {
        if (vaseRenderer == null) {
            return;
        }

        for (Vase vase : controller.getGame().getVases()) {
            float delay = vase.getLine() * 0.055f + (8 - vase.getTileIndex()) * 0.035f;
            float localTime = startupTime - delay;
            if (localTime < 0f) {
                continue;
            }

            vaseRenderer.renderVase(
                batch,
                vase,
                localTime,
                cellCenterX(vase.getTileIndex()),
                cellCenterY(vase.getLine()),
                cellWidth,
                cellHeight
            );
        }
    }

    private void renderBrokenVases(Batch batch, float cellWidth, float cellHeight) {
        if (vaseRenderer == null) {
            return;
        }

        for (BrokenVaseFx fx : brokenVases) {
            vaseRenderer.renderBreak(
                batch,
                fx.type,
                fx.time,
                cellCenterX(fx.column),
                cellCenterY(fx.row),
                cellWidth,
                cellHeight
            );
        }
    }

    private void renderPlants(Batch batch, float cellWidth, float cellHeight) {
        if (plantRenderer == null) {
            return;
        }

        for (Plant plant : controller.getGame().getPlantsInField()) {
            if (plant == null) {
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

    private void renderSeedPacketPlants(Batch batch, float cellWidth, float cellHeight) {
        if (plantRenderer == null) {
            return;
        }

        for (VaseSeedDrop drop : controller.getSeedDrops()) {
            if (drop == controller.getGame().getSelectedSeedDrop()) {
                continue;
            }

            CursorPreviewPlant preview = new CursorPreviewPlant(drop.getPlantType());
            preview.advance(cursorPlantTime);
            Color old = batch.getColor().cpy();
            batch.setColor(old.r, old.g, old.b, 0.82f);
            plantRenderer.render(
                batch,
                preview,
                cellCenterX(drop.getTileIndex()),
                cellCenterY(drop.getLine()),
                cellWidth * 0.58f,
                cellHeight * 0.58f
            );
            batch.setColor(old);
        }
    }

    private void renderCursor(Batch batch, float cellWidth, float cellHeight) {
        if (mode == InteractionMode.PLANT && cursorPlant != null && plantRenderer != null) {
            cursorPlant.advance(Math.max(0f, Gdx.graphics.getDeltaTime()));
            Color old = batch.getColor().cpy();
            batch.setColor(old.r, old.g, old.b, 0.58f);
            plantRenderer.render(
                batch,
                cursorPlant,
                pointerWorld.x,
                pointerWorld.y,
                cellWidth,
                cellHeight
            );
            batch.setColor(old);
            return;
        }

        if ((mode == InteractionMode.NORMAL || mode == InteractionMode.SHOVEL) && gloveRegion != null) {
            float width = 82f;
            float height = width * gloveRegion.getRegionHeight() / (float) gloveRegion.getRegionWidth();
            Color old = batch.getColor().cpy();
            if (mode == InteractionMode.SHOVEL) {
                batch.setColor(old.r, old.g, old.b, 0.72f);
            }
            batch.draw(
                gloveRegion,
                pointerWorld.x - width * 0.22f,
                pointerWorld.y - height * 0.70f,
                width,
                height
            );
            batch.setColor(old);
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
                if (keycode == Input.Keys.ESCAPE) {
                    if (mode != InteractionMode.NORMAL) {
                        setNormalMode(true);
                    } else {
                        App.setScreen(new MiniGamesView());
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                updatePointerFromScreen(screenX, screenY);

                if (controller.getGame().getState() == models.games.BaseGame.GameState.END) {
                    return true;
                }

                if (button == Input.Buttons.RIGHT) {
                    setNormalMode(true);
                    return true;
                }
                if (button != Input.Buttons.LEFT || !lawnBounds.contains(pointerWorld)) {
                    return false;
                }

                if (startupTime < 0.35f) {
                    return true;
                }

                return switch (mode) {
                    case NORMAL -> handleNormalClick();
                    case PLANT -> handlePlantClick();
                    case SHOVEL -> handleShovelClick();
                };
            }
        };
    }

    private boolean handleNormalClick() {
        VaseSeedDrop clickedDrop = hitSeedDrop(pointerWorld.x, pointerWorld.y);
        if (clickedDrop != null && controller.selectSeedDrop(clickedDrop)) {
            setPlantMode(clickedDrop.getPlantType());
            setStatus("Choose a tile for " + pretty(clickedDrop.getPlantType()) + ".");
            return true;
        }

        if (hoverColumn < 0 || hoverRow < 0) {
            return false;
        }

        VaseBreakResult result = controller.breakVase(hoverColumn, hoverRow);
        setStatus(result.getMessage());

        if (result.isBroken() && result.getVase() != null) {
            Vase vase = result.getVase();
            brokenVases.add(new BrokenVaseFx(
                vase.getType(),
                vase.getTileIndex(),
                vase.getLine(),
                vaseRenderer == null ? 0.18f : vaseRenderer.breakDuration(vase.getType())
            ));
            return true;
        }
        return false;
    }

    private boolean handlePlantClick() {
        if (hoverColumn < 0 || hoverRow < 0) {
            return false;
        }

        String result = controller.plantSelectedSeed(hoverColumn, hoverRow);
        setStatus(result);

        if (controller.getGame().getSelectedSeedDrop() == null) {
            setNormalMode(false);
        }
        return true;
    }

    private boolean handleShovelClick() {
        if (hoverColumn < 0 || hoverRow < 0) {
            return false;
        }

        setStatus(controller.pluck(hoverColumn, hoverRow));
        setNormalMode(false);
        return true;
    }

    private void setPlantMode(PlantType type) {
        mode = InteractionMode.PLANT;
        cursorPlant = new CursorPreviewPlant(type);
        cursorPlantTime = 0f;
        refreshModeLabel();
    }

    private void setShovelMode() {
        controller.cancelSeedSelection();
        mode = InteractionMode.SHOVEL;
        cursorPlant = null;
        refreshModeLabel();
        setStatus("Choose a plant to remove.");
    }

    private void setNormalMode(boolean cancelSeed) {
        if (cancelSeed) {
            controller.cancelSeedSelection();
        }
        mode = InteractionMode.NORMAL;
        cursorPlant = null;
        refreshModeLabel();
    }

    private void updatePointerFromScreen(int screenX, int screenY) {
        if (worldViewport == null) {
            return;
        }

        pointerWorld.set(screenX, screenY);
        worldViewport.unproject(pointerWorld);

        if (!lawnBounds.contains(pointerWorld)) {
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

    private VaseSeedDrop hitSeedDrop(float x, float y) {
        for (VaseSeedDrop drop : controller.getSeedDrops()) {
            if (drop == controller.getGame().getSelectedSeedDrop()) {
                continue;
            }
            if (seedDropRect(drop).contains(x, y)) {
                return drop;
            }
        }
        return null;
    }

    private Rectangle seedDropRect(VaseSeedDrop drop) {
        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float width = cellWidth * 0.55f;
        float height = cellHeight * 0.64f;
        float centerX = cellCenterX(drop.getTileIndex());
        float centerY = cellCenterY(drop.getLine());
        return new Rectangle(centerX - width * 0.5f, centerY - height * 0.48f, width, height);
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().left();
        root.pad(14f);

        TextButton back = new TextButton("BACK", skin, "brown");
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new MiniGamesView());
            }
        });

        TextButton shovel = new TextButton("SHOVEL", skin, "green_small");
        shovel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (mode == InteractionMode.SHOVEL) {
                    setNormalMode(false);
                } else {
                    setShovelMode();
                }
            }
        });

        Label title = new Label("VASE BREAKER", skin, "big_outline");
        title.setAlignment(Align.center);
        modeLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label("Break a vase.", skin);
        statusLabel.setAlignment(Align.center);

        root.add(back).size(120f, 48f).padRight(8f);
        root.add(shovel).size(145f, 48f).padRight(14f);
        root.add(title).width(390f).height(52f).center();
        root.add().expandX();
        root.add(modeLabel).width(210f).right().row();

        root.add(statusLabel)
            .colspan(5)
            .width(760f)
            .height(38f)
            .left()
            .padTop(6f);

        uiStage.addActor(root);
        buildResultPanel();
        refreshModeLabel();
    }

    private void buildResultPanel() {
        resultPanel = new Table();
        resultPanel.setFillParent(true);
        resultPanel.center();
        resultPanel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        resultPanel.setVisible(false);

        Table card = new Table();
        card.pad(24f);
        com.badlogic.gdx.scenes.scene2d.utils.Drawable background =
            getSkinDrawableSafe("image_ui_quests_panel_edge_to_edge_ten");
        if (background != null) {
            card.setBackground(background);
        }

        resultLabel = new Label("", skin, "big_outline");
        resultLabel.setAlignment(Align.center);
        card.add(resultLabel).colspan(2).width(440f).padBottom(16f).row();

        TextButton retry = new TextButton("RETRY", skin, "green");
        retry.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new VaseBreakerView(controller.getLevelNumber()));
            }
        });
        TextButton back = new TextButton("MINIGAMES", skin, "brown");
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                App.setScreen(new MiniGamesView());
            }
        });
        card.add(retry).width(170f).height(58f).pad(5f);
        card.add(back).width(200f).height(58f).pad(5f);
        resultPanel.add(card).center();
        uiStage.addActor(resultPanel);
    }

    private void refreshResultPanel() {
        if (resultPanel == null || resultLabel == null) {
            return;
        }

        boolean ended = controller.getGame().getState() == models.games.BaseGame.GameState.END;
        resultPanel.setVisible(ended);
        resultPanel.setTouchable(
            ended
                ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        );
        if (ended) {
            boolean won = "Won".equalsIgnoreCase(controller.getResultMessage());
            resultLabel.setText(won ? "YOU WIN!" : "YOU LOSE!");
            resultLabel.setColor(
                won
                    ? new Color(0.20f, 0.95f, 0.22f, 1f)
                    : new Color(0.95f, 0.04f, 0.04f, 1f)
            );
        }
    }

    private void refreshModeLabel() {
        if (modeLabel == null) {
            return;
        }
        String text = switch (mode) {
            case NORMAL -> "GLOVE";
            case PLANT -> "PLANT: " + pretty(controller.getGame().getSelectedPlantType());
            case SHOVEL -> "SHOVEL";
        };
        modeLabel.setText(text);
    }

    private void setStatus(String text) {
        if (statusLabel != null && text != null && !text.isBlank()) {
            statusLabel.setText(text);
        }
    }

    private void loadMap() {
        try {
            tiledMap = new TmxMapLoader().load("ancientegypt.tmx");
            mapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1f);
            lawnBounds.set(PitchBoundsReader.read(tiledMap));
        } catch (RuntimeException exception) {
            Gdx.app.error("VaseBreakerView", "Could not load Ancient Egypt map/pitch.", exception);
            lawnBounds.set(0f, 0f, 0f, 0f);
        }
    }

    private void configureCamera() {
        worldCamera.setToOrtho(false, WIDTH, HEIGHT);
        if (lawnBounds.width > 0f && lawnBounds.height > 0f) {
            worldCamera.position.set(
                lawnBounds.x + lawnBounds.width * 0.5f,
                lawnBounds.y + lawnBounds.height * 0.5f,
                0f
            );
        } else {
            worldCamera.position.set(WIDTH * 0.5f, HEIGHT * 0.5f, 0f);
        }
        worldCamera.update();
    }

    private void initialisePvzAssets() {
        pvzAssetsRoot = findPvzAssetsRoot();
        if (pvzAssetsRoot == null) {
            Gdx.app.error("VaseBreakerView", "Extracted PVZ assets were not found.");
            return;
        }

        try {
            textureBank = new TextureBank(ASSET_RESOLUTION, pvzAssetsRoot);
            vaseRenderer = new VaseRenderer(pvzAssetsRoot, textureBank);
            zombieRenderer = new VaseBreakerZombieRenderer(pvzAssetsRoot, textureBank);
            projectileRenderer = new ProjectileRenderer(pvzAssetsRoot, textureBank);
            plantRenderer = new PlantRenderer(pvzAssetsRoot);

            for (PlantType type : controller.getGame().getVasePlants()) {
                plantRenderer.preload(type);
            }
        } catch (RuntimeException exception) {
            Gdx.app.error("VaseBreakerView", "Failed to initialise PVZ assets.", exception);
        }
    }

    private FileHandle findPvzAssetsRoot() {
        List<FileHandle> roots = new ArrayList<>();
        String configured = System.getProperty("pvz.assets");
        if (configured != null && !configured.isBlank()) {
            roots.add(new FileHandle(new File(configured)));
        }

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

        String[] children = {
            "Base Assets",
            "base assets",
            "BaseAssets",
            "pvz-assets",
            "assets"
        };
        for (String childName : children) {
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
        boolean resources = root.child("RESOURCES.json").exists()
            || root.child("resources.json").exists();
        boolean atlases = root.child("ATLASES").exists()
            || root.child("atlases").exists();
        return resources && atlases;
    }

    private void installCursor() {
        try {
            Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            blank.setColor(0f, 0f, 0f, 0f);
            blank.fill();
            blankCursor = Gdx.graphics.newCursor(blank, 0, 0);
            blank.dispose();
            Gdx.graphics.setCursor(blankCursor);
        } catch (RuntimeException ignored) {
            // If a backend cannot create custom cursors, gameplay still works.
        }
    }

    private void updateBreakEffects(float delta) {
        Iterator<BrokenVaseFx> iterator = brokenVases.iterator();
        while (iterator.hasNext()) {
            BrokenVaseFx fx = iterator.next();
            fx.time += Math.max(0f, delta);
            if (fx.time >= fx.duration) {
                iterator.remove();
            }
        }
    }

    private float cellCenterX(int column) {
        return lawnBounds.x + (column + 0.5f) * (lawnBounds.width / 9f);
    }

    private float cellCenterY(int row) {
        return lawnBounds.y + (row + 0.5f) * (lawnBounds.height / 5f);
    }

    private static String pretty(PlantType type) {
        return type == null
            ? ""
            : type.name().toLowerCase().replace('_', ' ');
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
        restoreCursor();
        if (!disposed) {
            Gdx.app.postRunnable(this::dispose);
        }
    }

    private void restoreCursor() {
        try {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;

        restoreCursor();

        if (uiStage != null) {
            uiStage.dispose();
            uiStage = null;
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }
        if (tiledMap != null) {
            tiledMap.dispose();
            tiledMap = null;
        }
        if (plantRenderer != null) {
            plantRenderer.dispose();
            plantRenderer = null;
        }
        if (projectileRenderer != null) {
            projectileRenderer.dispose();
            projectileRenderer = null;
        }
        if (textureBank != null) {
            textureBank.dispose();
            textureBank = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapes != null) {
            shapes.dispose();
            shapes = null;
        }
        if (blankCursor != null) {
            blankCursor.dispose();
            blankCursor = null;
        }
    }

    /** Minimal fake Plant accepted by PlantRenderer for an animated ghost preview. */
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

    private static final class BrokenVaseFx {
        private final Vase.Type type;
        private final int column;
        private final int row;
        private final float duration;
        private float time;

        private BrokenVaseFx(Vase.Type type, int column, int row, float duration) {
            this.type = type;
            this.column = column;
            this.row = row;
            this.duration = duration;
        }
    }
}

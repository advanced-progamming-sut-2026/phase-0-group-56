package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import controllers.datacontroller.Data;
import controllers.menus.gamecontroller.PlayMenu;
import models.App;
import models.User;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;

import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class PlayView extends View {

    // -------------------------------------------------------------------------
    // Screen configuration
    // -------------------------------------------------------------------------

    private static final float VIRTUAL_WIDTH = 1280f;
    private static final float VIRTUAL_HEIGHT = 720f;

    private static final String PVZ_ASSET_RESOLUTION = "768";


    // -------------------------------------------------------------------------
    // Exact PvZ2 resource IDs
    // -------------------------------------------------------------------------

    private static final String BACKGROUND_TEXTURE =
        "IMAGE_MAINMENU_BACKGROUND";

    private static final String LOCK_TEXTURE =
        "IMAGE_UI_LOCK_SMALL_GOLD";

    private static final String LEVEL_NODE_TEXTURE =
        "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_140X209";


    private static final Map<Chapters, String> WORLD_TEXTURES =
        Map.of(
            Chapters.AncientEgypt,
            "IMAGE_WORLDMAP_EGYPT_ISLAND3",

            Chapters.FrozenCaves,
            "IMAGE_WORLDMAP_TWISTER_ISLAND83",

            Chapters.BigWaveBeach,
            "IMAGE_WORLDMAP_BEACH_ISLAND1",

            Chapters.DarkAge,
            "IMAGE_WORLDMAP_TWISTER_ISLAND77"
        );


    // -------------------------------------------------------------------------
    // Chapter order
    // -------------------------------------------------------------------------

    private static final Chapters[] CHAPTER_ORDER = {
        Chapters.AncientEgypt,
        Chapters.FrozenCaves,
        Chapters.BigWaveBeach,
        Chapters.DarkAge
    };


    /*
     * Horizontal centers of the four floating islands.
     */
    private static final float[] WORLD_X = {
        165f,
        480f,
        800f,
        1115f
    };


    // -------------------------------------------------------------------------
    // Controller
    // -------------------------------------------------------------------------

    private final PlayMenu playMenu;


    // -------------------------------------------------------------------------
    // Scene2D
    // -------------------------------------------------------------------------

    private Stage stage;
    private FitViewport viewport;

    private Skin skin;


    // -------------------------------------------------------------------------
    // PvZ assets
    // -------------------------------------------------------------------------

    private FileHandle pvzAssetsRoot;
    private TextureBank textureBank;

    private TextureRegion backgroundRegion;
    private TextureRegion lockRegion;
    private TextureRegion levelNodeRegion;

    private final Map<Chapters, TextureRegion> worldRegions =
        new EnumMap<>(Chapters.class);


    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private Chapters selectedChapter;

    private Group worldGroup;
    private Table levelPanel;

    private boolean disposed;


    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public PlayView() {
        playMenu = new PlayMenu();

        /*
         * Keep View's existing menu/controller contract.
         */
        this.menu = playMenu;
    }


    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void show() {

        disposed = false;

        skin = PvzSkin.get();

        viewport =
            new FitViewport(
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT
            );

        stage =
            new Stage(viewport);

        Gdx.input.setInputProcessor(stage);


        // ---------------------------------------------------------------------
        // Initial selected chapter
        // ---------------------------------------------------------------------

        User user =
            App.getCurrentuser();

        if (user != null &&
            user.getChapter() != null) {

            selectedChapter =
                user.getChapter();

        } else {

            selectedChapter =
                Chapters.AncientEgypt;
        }


        /*
         * PlayMenu keeps its own currentChapter.
         * The String returned by changeChapter belonged to the CLI UI,
         * therefore we simply ignore it here.
         */
        playMenu.changeChapter(
            selectedChapter
        );


        // ---------------------------------------------------------------------
        // Assets
        // ---------------------------------------------------------------------

        initialisePvzAssets();
        loadMenuTextures();


        // ---------------------------------------------------------------------
        // UI
        // ---------------------------------------------------------------------

        buildUI();
    }


    @Override
    public void render(float delta) {

        if (disposed ||
            stage == null) {

            return;
        }


        ScreenUtils.clear(
            0.02f,
            0.10f,
            0.14f,
            1f
        );


        if (Gdx.input.isKeyJustPressed(
            Input.Keys.ESCAPE
        )) {

            goBack();
            return;
        }


        stage.act(delta);
        stage.draw();
    }


    @Override
    public void resize(
        int width,
        int height
    ) {

        if (viewport != null) {

            viewport.update(
                width,
                height,
                true
            );
        }
    }


    @Override
    public void pause() {
    }


    @Override
    public void resume() {
    }


    @Override
    public void hide() {

        if (stage != null &&
            Gdx.input.getInputProcessor() == stage) {

            Gdx.input.setInputProcessor(null);
        }


        /*
         * Game.setScreen() may happen inside a Scene2D ClickListener.
         *
         * Post the dispose instead of destroying the Stage while
         * Scene2D is still dispatching the click event.
         */
        if (!disposed) {
            Gdx.app.postRunnable(
                this::dispose
            );
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


        if (textureBank != null) {

            textureBank.dispose();
            textureBank = null;
        }


        worldRegions.clear();

        backgroundRegion = null;
        lockRegion = null;
        levelNodeRegion = null;
    }


    // -------------------------------------------------------------------------
    // Build complete UI
    // -------------------------------------------------------------------------

    private void buildUI() {

        stage.clear();

        buildBackground();
        buildTopHud();
        buildWorldArea();
        buildLevelPanel();

        refreshWorlds(false);
        refreshLevels(false);
    }


    // -------------------------------------------------------------------------
    // Background
    // -------------------------------------------------------------------------

    private void buildBackground() {

        Image background =
            new Image(
                backgroundRegion
            );


        /*
         * Fill our virtual 1280x720 screen.
         */
        background.setScaling(
            Scaling.fill
        );

        background.setBounds(
            0f,
            0f,
            VIRTUAL_WIDTH,
            VIRTUAL_HEIGHT
        );


        /*
         * Background must never intercept clicks.
         */
        background.setTouchable(
            Touchable.disabled
        );


        stage.addActor(
            background
        );
    }


    // -------------------------------------------------------------------------
    // Top HUD
    // -------------------------------------------------------------------------

    private void buildTopHud() {

        Table hud =
            new Table();

        hud.setFillParent(true);
        hud.top();

        hud.pad(
            14f,
            18f,
            0f,
            18f
        );


        // ---------------------------------------------------------------------
        // Back
        // ---------------------------------------------------------------------

        TextButton backButton =
            new TextButton(
                "BACK",
                skin,
                "brown"
            );

        TextButton minigamesButton =
            new TextButton(
                "MINIGAMES",
                skin,
                "green"
            );


        backButton.addListener(
            new ClickListener() {

                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {

                    goBack();
                }
            }
        );

        minigamesButton.addListener(
            new ClickListener() {
                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {
                    App.setScreen(new MiniGamesView());
                }
            }
        );


        // ---------------------------------------------------------------------
        // Title
        // ---------------------------------------------------------------------

        Label title =
            new Label(
                "ADVENTURE",
                skin,
                "big_outline"
            );


        // ---------------------------------------------------------------------
        // User currency
        // ---------------------------------------------------------------------

        User user =
            App.getCurrentuser();


        int coins =
            user == null
                ? 0
                : user.getCoins();


        int gems =
            user == null
                ? 0
                : user.getDiamonds();


        Label coinsLabel =
            new Label(
                "COINS: " + coins,
                skin,
                "medium_outline"
            );


        Label gemsLabel =
            new Label(
                "GEMS: " + gems,
                skin,
                "medium_outline"
            );


        // ---------------------------------------------------------------------
        // Layout
        // ---------------------------------------------------------------------

        hud.add(backButton)
            .width(125f)
            .height(55f)
            .left();

        hud.add(minigamesButton)
            .width(165f)
            .height(55f)
            .padLeft(8f);


        hud.add()
            .expandX();


        hud.add(title)
            .center();


        hud.add()
            .expandX();


        hud.add(coinsLabel)
            .padRight(25f);


        hud.add(gemsLabel);


        stage.addActor(hud);
    }


    // -------------------------------------------------------------------------
    // World area
    // -------------------------------------------------------------------------

    private void buildWorldArea() {

        worldGroup =
            new Group();


        worldGroup.setBounds(
            0f,
            230f,
            VIRTUAL_WIDTH,
            390f
        );


        stage.addActor(
            worldGroup
        );
    }


    // -------------------------------------------------------------------------
    // Level area
    // -------------------------------------------------------------------------

    private void buildLevelPanel() {

        levelPanel =
            new Table();


        levelPanel.setBounds(
            230f,
            18f,
            820f,
            215f
        );


        levelPanel.top();


        stage.addActor(
            levelPanel
        );
    }


    // -------------------------------------------------------------------------
    // Worlds
    // -------------------------------------------------------------------------

    private void refreshWorlds(
        boolean animate
    ) {

        worldGroup.clearChildren();


        for (int i = 0;
             i < CHAPTER_ORDER.length;
             i++) {

            Chapters chapter =
                CHAPTER_ORDER[i];


            boolean selected =
                chapter == selectedChapter;


            Table world =
                createWorldActor(
                    chapter,
                    selected
                );


            float width =
                selected
                    ? 290f
                    : 225f;


            float height =
                selected
                    ? 285f
                    : 235f;


            float x =
                WORLD_X[i]
                    - width / 2f;


            float y =
                selected
                    ? 30f
                    : 58f;


            world.setBounds(
                x,
                y,
                width,
                height
            );


            world.setTransform(true);

            world.setOrigin(
                Align.center
            );


            if (animate) {

                world.getColor().a =
                    0f;

                world.setScale(
                    0.90f
                );


                world.addAction(
                    Actions.parallel(

                        Actions.fadeIn(
                            0.18f
                        ),

                        Actions.scaleTo(
                            1f,
                            1f,
                            0.20f
                        )
                    )
                );
            }


            worldGroup.addActor(
                world
            );
        }
    }


    private Table createWorldActor(
        Chapters chapter,
        boolean selected
    ) {

        Table world =
            new Table();


        world.setTouchable(
            Touchable.enabled
        );


        // ---------------------------------------------------------------------
        // Actual PvZ2 island texture
        // ---------------------------------------------------------------------

        TextureRegion region =
            worldRegions.get(
                chapter
            );


        Image island =
            new Image(region);


        island.setScaling(
            Scaling.fit
        );


        island.setTouchable(
            Touchable.disabled
        );


        float imageWidth =
            selected
                ? 255f
                : 195f;


        float imageHeight =
            selected
                ? 200f
                : 155f;


        world.add(island)
            .size(
                imageWidth,
                imageHeight
            )
            .center()
            .row();


        // ---------------------------------------------------------------------
        // Chapter name
        // ---------------------------------------------------------------------

        Label name =
            new Label(
                getChapterDisplayName(
                    chapter
                ),
                skin,
                selected
                    ? "big_outline"
                    : "medium_outline"
            );


        name.setAlignment(
            Align.center
        );


        name.setTouchable(
            Touchable.disabled
        );


        world.add(name)
            .center()
            .padTop(3f);


        // ---------------------------------------------------------------------
        // Selected marker
        // ---------------------------------------------------------------------

        if (selected) {

            world.row();


            Label selectedLabel =
                new Label(
                    "SELECTED",
                    skin,
                    "secondary"
                );


            selectedLabel.setTouchable(
                Touchable.disabled
            );


            world.add(selectedLabel)
                .center()
                .padTop(2f);
        }


        // ---------------------------------------------------------------------
        // Click
        // ---------------------------------------------------------------------

        world.addListener(
            new ClickListener() {

                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {

                    selectChapter(
                        chapter
                    );
                }
            }
        );


        return world;
    }


    private void selectChapter(
        Chapters chapter
    ) {

        if (chapter == null ||
            chapter == selectedChapter) {

            return;
        }


        selectedChapter =
            chapter;


        /*
         * Keep Controller synchronized.
         */
        playMenu.changeChapter(
            chapter
        );


        refreshWorlds(true);
        refreshLevels(true);
    }


    // -------------------------------------------------------------------------
    // Levels
    // -------------------------------------------------------------------------

    private void refreshLevels(
        boolean animate
    ) {

        levelPanel.clearChildren();


        // ---------------------------------------------------------------------
        // Chapter title
        // ---------------------------------------------------------------------

        Label chapterTitle =
            new Label(
                getChapterDisplayName(
                    selectedChapter
                ),
                skin,
                "big_outline"
            );


        chapterTitle.setAlignment(
            Align.center
        );


        levelPanel.add(chapterTitle)
            .colspan(4)
            .expandX()
            .center()
            .padBottom(5f)
            .row();


        // ---------------------------------------------------------------------
        // Get levels
        // ---------------------------------------------------------------------

        ArrayList<Level> rawLevels =
            Data.getAllLevels()
                .get(selectedChapter);


        if (rawLevels == null ||
            rawLevels.isEmpty()) {

            Label noLevels =
                new Label(
                    "NO LEVELS AVAILABLE",
                    skin,
                    "medium_outline"
                );


            levelPanel.add(noLevels)
                .colspan(4)
                .center()
                .padTop(30f);


            return;
        }


        /*
         * Copy before sorting.
         *
         * We don't want the View to mutate Data's original ArrayList.
         */
        List<Level> levels =
            new ArrayList<>(
                rawLevels
            );


        levels.sort(
            Comparator.comparingInt(
                Level::getId
            )
        );


        int count =
            Math.min(
                4,
                levels.size()
            );


        // ---------------------------------------------------------------------
        // Four level nodes
        // ---------------------------------------------------------------------

        for (int i = 0;
             i < count;
             i++) {

            Level level =
                levels.get(i);


            Actor levelActor =
                createLevelActor(
                    level
                );


            levelPanel.add(levelActor)
                .size(
                    105f,
                    150f
                )
                .padLeft(17f)
                .padRight(17f);
        }


        if (animate) {

            levelPanel.getColor().a =
                0f;


            levelPanel.addAction(
                Actions.fadeIn(
                    0.18f
                )
            );
        }
    }


    private Actor createLevelActor(
        Level level
    ) {

        boolean unlocked =
            isLevelUnlocked(
                level
            );


        boolean current =
            isCurrentLevel(
                level
            );


        Stack stack =
            new Stack();


        stack.setTouchable(
            Touchable.enabled
        );


        stack.setTransform(true);

        stack.setOrigin(
            Align.center
        );


        // ---------------------------------------------------------------------
        // Real PvZ2 level node texture
        // ---------------------------------------------------------------------

        Image node =
            new Image(
                levelNodeRegion
            );


        node.setScaling(
            Scaling.fit
        );


        node.setTouchable(
            Touchable.disabled
        );


        /*
         * Locked nodes appear slightly darker.
         */
        if (!unlocked) {

            node.setColor(
                0.58f,
                0.58f,
                0.58f,
                1f
            );
        }


        stack.add(node);


        // ---------------------------------------------------------------------
        // Node content
        // ---------------------------------------------------------------------

        if (unlocked) {

            Table numberHolder =
                new Table();


            numberHolder.setTouchable(
                Touchable.disabled
            );


            Label number =
                new Label(
                    String.valueOf(
                        level.getId()
                    ),
                    skin,
                    "big_outline"
                );


            number.setAlignment(
                Align.center
            );


            number.setTouchable(
                Touchable.disabled
            );


            numberHolder.add(number)
                .center()
                .padTop(3f);


            stack.add(
                numberHolder
            );


        } else {

            Table lockHolder =
                new Table();


            lockHolder.setTouchable(
                Touchable.disabled
            );


            Image lock =
                new Image(
                    lockRegion
                );


            lock.setScaling(
                Scaling.fit
            );


            lock.setTouchable(
                Touchable.disabled
            );


            lockHolder.add(lock)
                .size(
                    38f,
                    38f
                );


            stack.add(
                lockHolder
            );
        }


        // ---------------------------------------------------------------------
        // Current level indicator
        // ---------------------------------------------------------------------

        if (current &&
            unlocked) {

            Table currentHolder =
                new Table();


            currentHolder.bottom();


            currentHolder.setTouchable(
                Touchable.disabled
            );


            Label currentLabel =
                new Label(
                    "CURRENT",
                    skin,
                    "secondary"
                );


            currentLabel.setTouchable(
                Touchable.disabled
            );


            currentHolder.add(
                    currentLabel
                )
                .padBottom(5f);


            stack.add(
                currentHolder
            );
        }


        // ---------------------------------------------------------------------
        // Click
        // ---------------------------------------------------------------------

        stack.addListener(
            new ClickListener() {

                @Override
                public void clicked(
                    InputEvent event,
                    float x,
                    float y
                ) {

                    if (!unlocked) {

                        playLockedAnimation(
                            stack
                        );

                        return;
                    }


                    /*
                     * PlayMenu itself:
                     *
                     * 1. finds Level
                     * 2. checks unlock state
                     * 3. creates GameView
                     * 4. changes the current Screen
                     *
                     * Its returned String is only legacy CLI output.
                     */
                    playMenu.play(
                        level.getId()
                    );
                }
            }
        );


        return stack;
    }


    // -------------------------------------------------------------------------
    // Progression
    // -------------------------------------------------------------------------

    private boolean isLevelUnlocked(
        Level level
    ) {

        User user =
            App.getCurrentuser();


        if (user == null ||
            level == null) {

            return false;
        }


        /*
         * Same rule currently used by PlayMenu.
         */
        return user.getLevelsPassed()
            >= level.getId() - 1;
    }


    private boolean isCurrentLevel(
        Level level
    ) {

        User user =
            App.getCurrentuser();


        if (user == null ||
            level == null) {

            return false;
        }


        return
            user.getChapter()
                == selectedChapter

                &&

                user.getLevelId()
                    == level.getId();
    }


    // -------------------------------------------------------------------------
    // Locked node feedback
    // -------------------------------------------------------------------------

    private void playLockedAnimation(
        Actor actor
    ) {

        actor.clearActions();


        actor.addAction(
            Actions.sequence(

                Actions.scaleTo(
                    0.90f,
                    0.90f,
                    0.06f
                ),

                Actions.scaleTo(
                    1.06f,
                    1.06f,
                    0.07f
                ),

                Actions.scaleTo(
                    1f,
                    1f,
                    0.08f
                )
            )
        );
    }


    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private void goBack() {

        /*
         * PlayMenu.exitMenu() performs the actual navigation.
         */
        playMenu.exitMenu();
    }


    // -------------------------------------------------------------------------
    // PvZ asset loading
    // -------------------------------------------------------------------------

    private void initialisePvzAssets() {

        pvzAssetsRoot =
            findPvzAssetsRoot();


        if (pvzAssetsRoot == null) {

            throw new IllegalStateException(
                "Could not find the extracted PvZ2 Assets folder. " +
                    "Expected repo/Assets next to repo/pvz2, " +
                    "or set -Dpvz.assets=<path>."
            );
        }


        textureBank =
            new TextureBank(
                PVZ_ASSET_RESOLUTION,
                pvzAssetsRoot
            );


        Gdx.app.log(
            "PlayView",
            "PVZ assets: "
                + pvzAssetsRoot
                .file()
                .getAbsolutePath()
        );
    }


    /*
     * Our repository layout is:
     *
     * repo/
     * ├── Assets/
     * └── pvz2/
     *     └── assets/  <- Gradle desktop working directory
     *
     * Therefore:
     *
     * ../../Assets
     *
     * is the normal development path.
     *
     * ../Assets and Assets are included only because IDEs can use a
     * different working directory.
     */
    private FileHandle findPvzAssetsRoot() {

        // ---------------------------------------------------------------------
        // Explicit override
        // ---------------------------------------------------------------------

        String configured =
            System.getProperty(
                "pvz.assets"
            );


        if (configured != null &&
            !configured.isBlank()) {

            FileHandle root =
                new FileHandle(
                    new File(configured)
                );


            if (isPvzAssetsRoot(root)) {
                return root;
            }
        }


        // ---------------------------------------------------------------------
        // Project development layouts
        // ---------------------------------------------------------------------

        String[] candidates = {
            "../../Assets",
            "../Assets",
            "Assets"
        };


        for (String path :
            candidates) {

            FileHandle root =
                new FileHandle(
                    new File(path)
                );


            if (isPvzAssetsRoot(root)) {

                return root;
            }
        }


        return null;
    }


    private boolean isPvzAssetsRoot(
        FileHandle root
    ) {

        if (root == null ||
            !root.exists() ||
            !root.isDirectory()) {

            return false;
        }


        boolean resources =
            root.child(
                "resources.json"
            ).exists()

                ||

                root.child(
                    "RESOURCES.json"
                ).exists();


        boolean atlases =
            root.child(
                "atlases"
            ).exists()

                ||

                root.child(
                    "ATLASES"
                ).exists();


        return resources &&
            atlases;
    }


    // -------------------------------------------------------------------------
    // Exact texture loading
    // -------------------------------------------------------------------------

    private void loadMenuTextures() {

        /*
         * No searching.
         * No ResourceIndex iteration.
         * No guessing.
         *
         * These are the exact resource IDs found using the PvZ Asset Browser.
         */

        backgroundRegion =
            requireRegion(
                BACKGROUND_TEXTURE
            );


        lockRegion =
            requireRegion(
                LOCK_TEXTURE
            );


        levelNodeRegion =
            requireRegion(
                LEVEL_NODE_TEXTURE
            );


        for (Chapters chapter :
            CHAPTER_ORDER) {

            String resourceId =
                WORLD_TEXTURES.get(
                    chapter
                );


            TextureRegion region =
                requireRegion(
                    resourceId
                );


            worldRegions.put(
                chapter,
                region
            );
        }
    }


    /*
     * Fail fast when an ID is wrong instead of silently showing
     * a blank/fallback UI.
     *
     * Since these IDs come directly from Asset Browser, a null result means
     * either:
     *
     * 1. wrong Assets directory
     * 2. wrong resource ID
     * 3. incompatible asset dump
     */
    private TextureRegion requireRegion(
        String resourceId
    ) {

        TextureRegion region =
            textureBank.region(
                resourceId
            );


        if (region == null) {

            throw new IllegalStateException(
                "PvZ texture not found: "
                    + resourceId
            );
        }


        return region;
    }


    // -------------------------------------------------------------------------
    // Chapter text
    // -------------------------------------------------------------------------

    private String getChapterDisplayName(
        Chapters chapter
    ) {

        return switch (chapter) {

            case AncientEgypt ->
                "ANCIENT EGYPT";

            case FrozenCaves ->
                "FROZEN CAVES";

            case BigWaveBeach ->
                "BIG WAVE BEACH";

            case DarkAge ->
                "DARK AGES";
        };
    }
}

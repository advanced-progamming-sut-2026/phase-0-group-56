package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

import controllers.menus.gamecontroller.GameController;
import models.entity.Sun;
import models.factory.builder.PlantType;
import models.games.BaseGame;
import pvz.libpvz.textures.TextureBank;

/**
 * Owns rendering of gameplay entities that live in world/map coordinates.
 *
 * GameView only tells this object when to render and delegates sun hit-testing
 * to it. Concrete renderers/layers stay out of GameView.
 */
public final class WorldEntityRenderer implements Disposable {

    private static final String TAG = "WorldEntityRenderer";

    private final GameController controller;
    private final Rectangle pitchBounds;
    private final Stage stage;

    private PlantRenderer plantRenderer;
    private SunRenderer sunRenderer;
    private MowerRenderer mowerRenderer;
    private ZombieRenderer zombieRenderer;
    private ChapterElementRenderer chapterElementRenderer;
    private SpecialGameElementRenderer specialGameElementRenderer;

    public WorldEntityRenderer(
        Viewport worldViewport,
        GameController controller,
        Rectangle pitchBounds,
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank,
        Drawable sunFallback
    ) {
        if (worldViewport == null) {
            throw new IllegalArgumentException("worldViewport cannot be null");
        }

        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }

        if (pitchBounds == null
            || pitchBounds.width <= 0f
            || pitchBounds.height <= 0f) {

            throw new IllegalArgumentException(
                "pitchBounds must contain a valid playable rectangle"
            );
        }

        this.controller = controller;
        this.pitchBounds = new Rectangle(pitchBounds);
        this.stage = new Stage(worldViewport);

        // Layer order is intentional:
        // map -> chapter ground/water -> plants -> zombies -> mowers
        // -> chapter foreground -> suns.
        initialiseChapterBackground(pvzAssetsRoot, sharedTextureBank);
        initialiseSpecialGameElements(pvzAssetsRoot, sharedTextureBank);
        initialisePlants(pvzAssetsRoot);
        initialiseZombies(pvzAssetsRoot, sharedTextureBank);
        initialiseMowers(pvzAssetsRoot, sharedTextureBank);
        initialiseChapterForeground();
        initialiseSuns(pvzAssetsRoot, sharedTextureBank, sunFallback);
    }

    private void initialiseZombies(
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank
    ) {
        if (pvzAssetsRoot == null || sharedTextureBank == null) {
            Gdx.app.log(
                TAG,
                "Zombie rendering disabled because extracted PVZ assets are unavailable."
            );
            return;
        }

        try {
            zombieRenderer = new ZombieRenderer(
                pvzAssetsRoot,
                sharedTextureBank
            );

            ZombieLayer zombieLayer = new ZombieLayer(
                controller,
                zombieRenderer
            );
            zombieLayer.setBounds(
                pitchBounds.x,
                pitchBounds.y,
                pitchBounds.width,
                pitchBounds.height
            );
            stage.addActor(zombieLayer);
        } catch (RuntimeException exception) {
            if (zombieRenderer != null) {
                zombieRenderer.dispose();
                zombieRenderer = null;
            }
            Gdx.app.error(TAG, "Failed to initialise zombie rendering.", exception);
        }
    }


    private void initialiseMowers(
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank
    ) {
        if (pvzAssetsRoot == null || sharedTextureBank == null) {
            Gdx.app.log(
                TAG,
                "Mower rendering disabled because extracted PVZ assets are unavailable."
            );
            return;
        }

        try {
            mowerRenderer = new MowerRenderer(
                pvzAssetsRoot,
                sharedTextureBank,
                controller.getChapter()
            );

            MowerLayer mowerLayer = new MowerLayer(
                controller,
                mowerRenderer,
                false
            );

            mowerLayer.setBounds(
                pitchBounds.x,
                pitchBounds.y,
                pitchBounds.width,
                pitchBounds.height
            );

            stage.addActor(mowerLayer);
        } catch (RuntimeException e) {
            if (mowerRenderer != null) {
                mowerRenderer.dispose();
                mowerRenderer = null;
            }

            Gdx.app.error(
                TAG,
                "Failed to initialise mower rendering.",
                e
            );
        }
    }

    private void initialiseSpecialGameElements(
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank
    ) {
        if (pvzAssetsRoot == null
            || sharedTextureBank == null
            || !SpecialGameElementRenderer.supports(controller.getGame())) {
            return;
        }

        try {
            specialGameElementRenderer = new SpecialGameElementRenderer(
                pvzAssetsRoot,
                sharedTextureBank
            );

            SpecialGameElementLayer specialLayer = new SpecialGameElementLayer(
                controller,
                specialGameElementRenderer
            );
            specialLayer.setBounds(
                pitchBounds.x,
                pitchBounds.y,
                pitchBounds.width,
                pitchBounds.height
            );

            // Added before PlantLayer, therefore protected tiles and the
            // deadline marker stay on the lawn instead of covering plants.
            stage.addActor(specialLayer);
        } catch (RuntimeException e) {
            specialGameElementRenderer = null;
            Gdx.app.error(
                TAG,
                "Failed to initialise special-game visuals.",
                e
            );
        }
    }


    private void initialiseChapterBackground(
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank
    ) {
        if (pvzAssetsRoot == null || sharedTextureBank == null) {
            Gdx.app.log(
                TAG,
                "Chapter visuals disabled because extracted PVZ assets are unavailable."
            );
            return;
        }

        try {
            chapterElementRenderer = new ChapterElementRenderer(
                pvzAssetsRoot,
                sharedTextureBank
            );
            chapterElementRenderer.preload(controller.getChapter());

            ChapterElementLayer background = new ChapterElementLayer(
                controller,
                chapterElementRenderer,
                ChapterElementRenderer.Pass.BACKGROUND
            );
            background.setBounds(
                pitchBounds.x,
                pitchBounds.y,
                pitchBounds.width,
                pitchBounds.height
            );
            stage.addActor(background);
        } catch (RuntimeException e) {
            chapterElementRenderer = null;
            Gdx.app.error(TAG, "Failed to initialise chapter visuals.", e);
        }
    }

    private void initialisePlants(FileHandle pvzAssetsRoot) {
        if (pvzAssetsRoot == null) {
            Gdx.app.error(
                TAG,
                "PVZ assets were not found; plant world rendering is disabled."
            );
            return;
        }

        try {
            plantRenderer = new PlantRenderer(pvzAssetsRoot);

            PlantLayer plantLayer = new PlantLayer(
                controller,
                plantRenderer,
                false
            );

            plantLayer.setBounds(
                pitchBounds.x,
                pitchBounds.y,
                pitchBounds.width,
                pitchBounds.height
            );

            stage.addActor(plantLayer);

        } catch (RuntimeException e) {
            if (plantRenderer != null) {
                try {
                    plantRenderer.dispose();
                } catch (RuntimeException ignored) {
                }
                plantRenderer = null;
            }

            Gdx.app.error(
                TAG,
                "Failed to initialise plant rendering.",
                e
            );
        }
    }

    private void initialiseChapterForeground() {
        if (chapterElementRenderer == null) {
            return;
        }

        ChapterElementLayer foreground = new ChapterElementLayer(
            controller,
            chapterElementRenderer,
            ChapterElementRenderer.Pass.FOREGROUND
        );
        foreground.setBounds(
            pitchBounds.x,
            pitchBounds.y,
            pitchBounds.width,
            pitchBounds.height
        );
        stage.addActor(foreground);
    }

    private void initialiseSuns(
        FileHandle pvzAssetsRoot,
        TextureBank sharedTextureBank,
        Drawable sunFallback
    ) {
        sunRenderer = new SunRenderer(
            pvzAssetsRoot,
            sharedTextureBank,
            sunFallback
        );

        SunLayer sunLayer = new SunLayer(
            controller,
            sunRenderer
        );

        sunLayer.setBounds(
            pitchBounds.x,
            pitchBounds.y,
            pitchBounds.width,
            pitchBounds.height
        );

        stage.addActor(sunLayer);
    }

    /**
     * The caller must already have applied the world viewport/camera.
     */
    public void render(float delta) {
        float animationDelta = controller.getGame().getState() == BaseGame.GameState.PLAYING
            ? Math.max(0f, delta)
            : 0f;

        if (chapterElementRenderer != null) {
            chapterElementRenderer.update(animationDelta);
        }
        if (specialGameElementRenderer != null) {
            specialGameElementRenderer.update(animationDelta);
        }

        stage.act(delta);
        stage.draw();
    }

    /**
     * Keeps SunRenderer encapsulated while preserving GameView's existing
     * world-coordinate sun collection behaviour.
     */
    public Sun hitTestSun(float worldX, float worldY) {
        if (sunRenderer == null) {
            return null;
        }

        return sunRenderer.hitTest(
            controller.getGame().getSuns(),
            worldX,
            worldY,
            pitchBounds
        );
    }

    public void preloadPlants(Iterable<PlantType> plantTypes) {
        if (plantRenderer == null || plantTypes == null) {
            return;
        }

        for (PlantType type : plantTypes) {
            if (type != null) {
                plantRenderer.preload(type);
            }
        }
    }

    @Override
    public void dispose() {
        stage.dispose();

        if (chapterElementRenderer != null) {
            chapterElementRenderer.dispose();
            chapterElementRenderer = null;
        }

        if (plantRenderer != null) {
            plantRenderer.dispose();
            plantRenderer = null;
        }

        if (mowerRenderer != null) {
            mowerRenderer.dispose();
            mowerRenderer = null;
        }

        if (zombieRenderer != null) {
            zombieRenderer.dispose();
            zombieRenderer = null;
        }

        if (specialGameElementRenderer != null) {
            specialGameElementRenderer.dispose();
            specialGameElementRenderer = null;
        }
        if (sunRenderer != null) {
            sunRenderer.dispose();
            sunRenderer = null;
        }
    }
}

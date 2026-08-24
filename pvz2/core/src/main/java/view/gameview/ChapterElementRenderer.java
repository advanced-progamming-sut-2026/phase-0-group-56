package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import models.gameadventure.Chapters;
import models.gameadventure.IcyWind;
import models.gameadventure.Tornado;
import models.entity.Zombie;
import models.gamepanes.Tile;
import models.gamepanes.TileType;
import models.games.BaseGame;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders chapter-specific world visuals over the current model state.
 *
 * <p>This class is view-only. It never changes Tile, Field, ChapterSpecialEvent,
 * zombie, plant, or game state.</p>
 */
public final class ChapterElementRenderer implements Disposable {

    public enum Pass {
        BACKGROUND,
        FOREGROUND
    }

    private static final String TAG = "ChapterElementRenderer";
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    // Ancient Egypt
    private static final String EGYPT_GRAVE =
        "768/INITIAL/GRAVESTONES/EGYPT_HIEROGLYPH/EGYPT_HIEROGLYPH.PAM";
    private static final String EGYPT_GRAVE_DAMAGE =
        "768/INITIAL/EFFECTS/TOMBSTONE_EGYPT_HIEROGLYPH_DAMAGE/"
            + "TOMBSTONE_EGYPT_HIEROGLYPH_DAMAGE.PAM";
    private static final String EGYPT_SANDSTORM =
        "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";

    // Frozen Caves
    private static final String SLIPPERY_UP =
        "768/FULL/EFFECTS/TILESLIDER_ICEAGE_UP/TILESLIDER_ICEAGE_UP.PAM";
    private static final String SLIPPERY_DOWN =
        "768/FULL/EFFECTS/TILESLIDER_ICEAGE_DOWN/TILESLIDER_ICEAGE_DOWN.PAM";
    private static final String FROSTBITE_CHILL_WIND =
        "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    private static final String ICE_BLOCK_ZOMBIE =
        "768/INITIAL/EFFECTS/ICEBLOOM_ICE_BLOCK_ZOMBIE/ICEBLOOM_ICE_BLOCK_ZOMBIE.PAM";

    // Big Wave Beach
    private static final String WATER_SQUARE =
        "768/FULL/BACKGROUNDS/WATER_SQUARE/WATER_SQUARE.PAM";
    private static final String WATER_FOAM =
        "768/FULL/EFFECTS/WATER_FOAM/WATER_FOAM.PAM";
    private static final String WATER_UNDERLAYER =
        "768/FULL/BACKGROUNDS/WATER_UNDERLAYER/WATER_UNDERLAYER.PAM";
    private static final String WATER_UPPERLAYER =
        "768/FULL/BACKGROUNDS/WAVE_UPPERLAYER/WAVE_UPPERLAYER.PAM";
    private static final String WATER_TIDE_LINE =
        "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";

    // Dark Age
    private static final String DARK_GRAVE =
        "768/FULL/GRAVESTONES/DARK_NOOP/DARK_NOOP.PAM";
    private static final String NECROMANCY_GRAVE =
        "768/INITIAL/GRAVESTONES/TUTORIAL_GRAVESTONE/TUTORIAL_GRAVESTONE.PAM";

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;

    private final Map<String, PamVisual> loaded = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Set<String> missing = new HashSet<>();

    /** Start time of the first visible damage state for each Egyptian grave tile. */
    private final Map<Tile, Float> egyptDamageStarted = new IdentityHashMap<>();

    private float animationTime;
    private boolean disposed;

    public ChapterElementRenderer(FileHandle assetsRoot, TextureBank sharedTextureBank) {
        if (assetsRoot == null || !assetsRoot.exists()) {
            throw new IllegalArgumentException("assetsRoot must exist");
        }
        if (sharedTextureBank == null) {
            throw new IllegalArgumentException("sharedTextureBank cannot be null");
        }

        FileHandle explicitPamFolder = assetsRoot.child("pam");
        this.pamRoot = explicitPamFolder.exists() && explicitPamFolder.isDirectory()
            ? explicitPamFolder
            : assetsRoot.child("IMAGES");

        this.pamPlayer = new PamPlayer(sharedTextureBank, assetsRoot);
    }

    /** Advances purely visual chapter animations. Pass 0 while the game is paused. */
    public void update(float delta) {
        if (!disposed) {
            animationTime += Math.max(0f, delta);
        }
    }

    /** Queues only the PAMs needed by the selected chapter. */
    public void preload(Chapters chapter) {
        if (chapter == null || disposed) {
            return;
        }

        switch (chapter) {
            case AncientEgypt -> {
                request(EGYPT_GRAVE);
                request(EGYPT_GRAVE_DAMAGE);
                request(EGYPT_SANDSTORM);
            }
            case FrozenCaves -> {
                request(SLIPPERY_UP);
                request(SLIPPERY_DOWN);
                request(FROSTBITE_CHILL_WIND);
                request(ICE_BLOCK_ZOMBIE);
            }
            case BigWaveBeach -> {
                request(WATER_SQUARE);
                request(WATER_FOAM);
                request(WATER_UNDERLAYER);
                request(WATER_UPPERLAYER);
                request(WATER_TIDE_LINE);
            }
            case DarkAge -> {
                request(DARK_GRAVE);
                request(NECROMANCY_GRAVE);
            }
        }
    }

    /**
     * Draws one pass. The Batch is supplied by Scene2D and is already begun.
     */
    public void render(
        Batch batch,
        BaseGame game,
        Chapters chapter,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight,
        Pass pass
    ) {
        if (disposed || batch == null || game == null || chapter == null || pass == null) {
            return;
        }
        if (lawnWidth <= 0f || lawnHeight <= 0f || game.getField() == null) {
            return;
        }

        float cellWidth = lawnWidth / COLUMN_COUNT;
        float cellHeight = lawnHeight / ROW_COUNT;

        if (pass == Pass.BACKGROUND) {
            switch (chapter) {
                case AncientEgypt -> {
                    // Ancient-Egypt graves and the sandstorm are foreground visuals.
                }
                case FrozenCaves -> renderFrozenCavesBackground(
                    batch, game, lawnX, lawnY, cellWidth, cellHeight
                );
                case BigWaveBeach -> renderBigWaveBeachBackground(
                    batch, game, lawnX, lawnY, cellWidth, cellHeight
                );
                case DarkAge -> {
                    // Dark-Age graves are foreground visuals.
                }
            }
            return;
        }

        switch (chapter) {
            case AncientEgypt -> renderAncientEgyptForeground(
                batch, game, lawnX, lawnY, lawnWidth, lawnHeight, cellWidth, cellHeight
            );
            case FrozenCaves -> {
                renderFrozenCavesForeground(
                    batch, game, lawnX, lawnY, cellWidth, cellHeight
                );
            }
            case BigWaveBeach -> renderBigWaveBeachForeground(
                batch, game, lawnX, lawnY, cellWidth, cellHeight
            );
            case DarkAge -> renderDarkAgeForeground(
                batch, game, lawnX, lawnY, cellWidth, cellHeight
            );
        }
    }

    private void renderAncientEgyptForeground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight,
        float cellWidth,
        float cellHeight
    ) {
        List<? extends List<Tile>> rows = game.getField().getTiles();
        for (List<Tile> row : rows) {
            for (Tile tile : row) {
                if (tile == null || tile.getTileType() != TileType.EGYPTIAN_GRAVE) {
                    continue;
                }

                float maxHp = Math.max(1f, TileType.EGYPTIAN_GRAVE.getHp());
                if (tile.getHp() <= 0) {
                    egyptDamageStarted.remove(tile);
                    continue;
                }

                drawTilePam(
                    batch,
                    EGYPT_GRAVE,
                    tile,
                    lawnX,
                    lawnY,
                    cellWidth,
                    cellHeight,
                    0.98f,
                    1.34f,
                    animationTime,
                    true
                );

                if (tile.getHp() < maxHp) {
                    float start = egyptDamageStarted.computeIfAbsent(tile, ignored -> animationTime);
                    drawTilePam(
                        batch,
                        EGYPT_GRAVE_DAMAGE,
                        tile,
                        lawnX,
                        lawnY,
                        cellWidth,
                        cellHeight,
                        1.12f,
                        1.46f,
                        Math.max(0f, animationTime - start),
                        false
                    );
                } else {
                    egyptDamageStarted.remove(tile);
                }
            }
        }

        // Sandstorm is not a full-screen overlay. Ancient Egypt's Tornado event
        // carries a subset of zombies toward their destination columns, so the
        // visual follows those zombies instead of sitting at the centre of the lawn.
        if (game.getEvent() instanceof Tornado tornado) {
            renderTornado(
                batch,
                tornado,
                lawnX,
                lawnY,
                cellWidth,
                cellHeight
            );
        }
    }


    private void renderTornado(
        Batch batch,
        Tornado tornado,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        for (Zombie zombie : tornado.getCarriedZombies()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            int row = zombie.getLine();
            if (row < 0 || row >= ROW_COUNT) {
                continue;
            }

            // Zombie/Tornado coordinates use the same tile-width model as the
            // rest of the game.  The old 100+column*50 conversion belonged to
            // the pre-scaling renderer and placed the storm outside the lawn.
            float logicalBoardWidth = COLUMN_COUNT * Math.max(1f, Tile.getWidth());
            float modelCenterX = zombie.getX() + zombie.getWidth() * 0.5f;
            float normalizedX = modelCenterX / logicalBoardWidth;

            // Do not pin an off-screen tornado to the lawn edge. It becomes visible
            // naturally once the carried zombie reaches the playable area.
            if (normalizedX < -0.25f || normalizedX > 1.25f) {
                continue;
            }

            float centerX = lawnX + normalizedX * cellWidth * COLUMN_COUNT;
            float centerY = lawnY + (row + 0.5f) * cellHeight;

            drawPamFit(
                batch,
                EGYPT_SANDSTORM,
                animationTime,
                centerX,
                centerY,
                cellWidth * 1.55f,
                cellHeight * 1.85f,
                true,
                true
            );
        }
    }

    private void renderFrozenCavesBackground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        List<? extends List<Tile>> rows = game.getField().getTiles();
        for (List<Tile> row : rows) {
            for (Tile tile : row) {
                if (tile == null) {
                    continue;
                }

                if (tile.getTileType() == TileType.SLIPPERY_UP) {
                    drawTilePam(
                        batch, SLIPPERY_UP, tile,
                        lawnX, lawnY, cellWidth, cellHeight,
                        1.06f, 1.06f, animationTime, true
                    );
                } else if (tile.getTileType() == TileType.SLIPPERY_DOWN) {
                    drawTilePam(
                        batch, SLIPPERY_DOWN, tile,
                        lawnX, lawnY, cellWidth, cellHeight,
                        1.06f, 1.06f, animationTime, true
                    );
                }
            }
        }
    }

    private void renderFrozenCavesForeground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        renderFrozenZombieBlocks(batch, game, lawnX, lawnY, cellWidth, cellHeight);
        if (!(game.getEvent() instanceof IcyWind wind) || !wind.isBlowing()) {
            return;
        }

        // The wind is a chapter-wide overlay, not a tile effect.  Keeping it in
        // the foreground pass makes it visible over plants and zombies while
        // retaining the map and icy slider tiles underneath it.
        drawPamFit(
            batch,
            FROSTBITE_CHILL_WIND,
            animationTime,
            lawnX + cellWidth * COLUMN_COUNT * 0.5f,
            lawnY + cellHeight * ROW_COUNT * 0.5f,
            cellWidth * COLUMN_COUNT * 1.18f,
            cellHeight * ROW_COUNT * 1.12f,
            false,
            true
        );
    }

    private void renderFrozenZombieBlocks(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        float logicalBoardWidth = COLUMN_COUNT * Math.max(1f, Tile.getWidth());
        for (Zombie zombie : game.getZombies()) {
            if (zombie == null || zombie.isDead() || !zombie.isEncasedInIce()) {
                continue;
            }
            float centerX = lawnX + (zombie.getX() + zombie.getWidth() * 0.5f)
                / logicalBoardWidth * cellWidth * COLUMN_COUNT;
            float centerY = lawnY + (zombie.getLine() + 0.5f) * cellHeight;
            drawPamFit(
                batch,
                ICE_BLOCK_ZOMBIE,
                animationTime,
                centerX,
                centerY,
                cellWidth * 1.12f,
                cellHeight * 1.20f,
                true,
                false
            );
        }
    }

    private void renderBigWaveBeachBackground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        List<? extends List<Tile>> rows = game.getField().getTiles();

        for (int rowIndex = 0; rowIndex < Math.min(ROW_COUNT, rows.size()); rowIndex++) {
            List<Tile> row = rows.get(rowIndex);
            int firstWater = firstWaterColumn(row);
            int lastWater = lastWaterColumn(row);
            if (firstWater < 0 || lastWater < firstWater) {
                continue;
            }

            // One stretched under-layer per wet run keeps draw calls down and lets
            // a changing water surface expand/shrink directly from Tile.isWater().
            float runWidth = (lastWater - firstWater + 1) * cellWidth;
            float runCenterX = lawnX + firstWater * cellWidth + runWidth * 0.5f;
            float centerY = lawnY + (rowIndex + 0.5f) * cellHeight;

            drawPamFit(
                batch,
                WATER_UNDERLAYER,
                animationTime,
                runCenterX,
                centerY,
                runWidth * 1.02f,
                cellHeight * 1.08f,
                false,
                true
            );

            drawPamFit(
                batch,
                WATER_UPPERLAYER,
                animationTime,
                runCenterX,
                centerY - cellHeight * 0.10f,
                runWidth * 1.04f,
                cellHeight * 1.10f,
                false,
                true
            );

            for (int col = 0; col < Math.min(COLUMN_COUNT, row.size()); col++) {
                Tile tile = row.get(col);
                if (tile == null || !tile.isWater()) {
                    continue;
                }

                drawTilePam(
                    batch,
                    WATER_SQUARE,
                    tile,
                    lawnX,
                    lawnY,
                    cellWidth,
                    cellHeight,
                    1.04f,
                    1.05f,
                    animationTime,
                    true
                );
            }
        }
    }

    private void renderBigWaveBeachForeground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        List<? extends List<Tile>> rows = game.getField().getTiles();

        for (int rowIndex = 0; rowIndex < Math.min(ROW_COUNT, rows.size()); rowIndex++) {
            List<Tile> row = rows.get(rowIndex);
            int firstWater = firstWaterColumn(row);
            if (firstWater < 0) {
                continue;
            }

            // Foam belongs on the moving shoreline, not on every water tile.
            float shorelineX = lawnX + firstWater * cellWidth;
            float centerY = lawnY + (rowIndex + 0.5f) * cellHeight;

            drawPamFit(
                batch,
                WATER_FOAM,
                animationTime,
                shorelineX,
                centerY,
                cellWidth * 1.05f,
                cellHeight * 1.16f,
                true,
                true
            );
        }

        int maxColumn = Math.max(0, Math.min(COLUMN_COUNT - 1,
            game.getField().getWaveLimitColumn()));
        float boundaryX = lawnX + maxColumn * cellWidth;
        drawPamFit(
            batch,
            WATER_TIDE_LINE,
            animationTime,
            boundaryX,
            lawnY + cellHeight * ROW_COUNT * 0.5f,
            cellWidth * 1.05f,
            cellHeight * ROW_COUNT * 1.02f,
            false,
            true
        );
    }

    private void renderDarkAgeForeground(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        List<? extends List<Tile>> rows = game.getField().getTiles();
        for (List<Tile> row : rows) {
            for (Tile tile : row) {
                if (tile == null) {
                    continue;
                }

                if (tile.getTileType() == TileType.DARK_AGE_GRAVE) {
                    if (tile.getHp() > 0) {
                        drawTilePam(
                            batch,
                            DARK_GRAVE,
                            tile,
                            lawnX,
                            lawnY,
                            cellWidth,
                            cellHeight,
                            0.98f,
                            1.34f,
                            animationTime,
                            true
                        );
                    }
                } else if (tile.getTileType() == TileType.NECROMANCY) {
                    drawTilePam(
                        batch,
                        NECROMANCY_GRAVE,
                        tile,
                        lawnX,
                        lawnY,
                        cellWidth,
                        cellHeight,
                        0.94f,
                        1.30f,
                        animationTime,
                        true
                    );
                }
            }
        }
    }

    private int firstWaterColumn(List<Tile> row) {
        if (row == null) {
            return -1;
        }
        for (int col = 0; col < Math.min(COLUMN_COUNT, row.size()); col++) {
            Tile tile = row.get(col);
            if (tile != null && tile.isWater()) {
                return col;
            }
        }
        return -1;
    }

    private int lastWaterColumn(List<Tile> row) {
        if (row == null) {
            return -1;
        }
        for (int col = Math.min(COLUMN_COUNT, row.size()) - 1; col >= 0; col--) {
            Tile tile = row.get(col);
            if (tile != null && tile.isWater()) {
                return col;
            }
        }
        return -1;
    }

    private void drawTilePam(
        Batch batch,
        String pamPath,
        Tile tile,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight,
        float targetWidthCells,
        float targetHeightCells,
        float time,
        boolean loop
    ) {
        int col = tile.getCol();
        int row = tile.getLine();
        if (col < 0 || col >= COLUMN_COUNT || row < 0 || row >= ROW_COUNT) {
            return;
        }

        float centerX = lawnX + (col + 0.5f) * cellWidth;
        float centerY = lawnY + (row + 0.5f) * cellHeight;

        drawPamFit(
            batch,
            pamPath,
            time,
            centerX,
            centerY,
            cellWidth * targetWidthCells,
            cellHeight * targetHeightCells,
            true,
            loop
        );
    }

    private void drawPamFit(
        Batch batch,
        String pamPath,
        float time,
        float centerX,
        float centerY,
        float targetWidth,
        float targetHeight,
        boolean preserveAspect,
        boolean loop
    ) {
        PamVisual visual = loaded.get(pamPath);
        if (visual == null) {
            request(pamPath);
            return;
        }

        Rectangle bounds = visual.bounds;
        float sourceWidth = Math.max(1f, bounds.width);
        float sourceHeight = Math.max(1f, bounds.height);

        float scaleX = Math.max(0.0001f, targetWidth / sourceWidth);
        float scaleY = Math.max(0.0001f, targetHeight / sourceHeight);
        if (preserveAspect) {
            float uniform = Math.min(scaleX, scaleY);
            scaleX = uniform;
            scaleY = uniform;
        }

        // libPVZ's scaled draw overload treats x/y as the PAM canvas centre,
        // so the world target centre can be passed directly. Bounds are used only
        // to derive a sensible world scale.
        pamPlayer.draw(
            batch,
            visual.clip,
            Math.max(0f, time),
            centerX,
            centerY,
            scaleX,
            scaleY,
            loop
        );
    }

    private void request(String pamPath) {
        if (disposed || pamPath == null || pamPath.isBlank()
            || loaded.containsKey(pamPath)
            || loading.contains(pamPath)
            || missing.contains(pamPath)) {
            return;
        }

        if (pamRoot == null || !pamRoot.exists() || !pamRoot.child(pamPath).exists()) {
            missing.add(pamPath);
            Gdx.app.error(TAG, "PAM not found: " + pamPath);
            return;
        }

        loading.add(pamPath);
        pamPlayer.loadAsync(pamPath, () -> onLoaded(pamPath));
    }

    private void onLoaded(String pamPath) {
        try {
            if (disposed) {
                return;
            }

            List<String> clips = pamPlayer.clips(pamPath);
            String clipName = chooseClip(clips);
            if (clipName == null) {
                missing.add(pamPath);
                Gdx.app.error(TAG, "PAM has no clips: " + pamPath);
                return;
            }

            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            Rectangle bounds = pamPlayer.bounds(pamPath, clipName);
            if (clip == null || bounds == null) {
                missing.add(pamPath);
                Gdx.app.error(TAG, "Could not resolve clip/bounds: " + pamPath + " / " + clipName);
                return;
            }

            loaded.put(pamPath, new PamVisual(clipName, clip, new Rectangle(bounds)));
        } catch (RuntimeException exception) {
            missing.add(pamPath);
            Gdx.app.error(TAG, "Failed to load PAM: " + pamPath, exception);
        } finally {
            loading.remove(pamPath);
        }
    }

    private static String chooseClip(List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }

        String[] preferred = {"animation", "anim", "idle", "loop", "default"};
        for (String candidate : preferred) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(candidate)) {
                    return clip;
                }
            }
        }

        for (String clip : clips) {
            if (clip == null) {
                continue;
            }
            String normalized = clip.toLowerCase(Locale.ROOT);
            if (normalized.contains("idle")
                || normalized.contains("loop")
                || normalized.contains("animation")) {
                return clip;
            }
        }

        return clips.get(0);
    }

    @Override
    public void dispose() {
        disposed = true;
        loaded.clear();
        loading.clear();
        missing.clear();
        egyptDamageStarted.clear();
        // PamPlayer shares TextureBank owned by GameView/WorldEntityRenderer.
    }

    private static final class PamVisual {
        final String clipName;
        final ClipRef clip;
        final Rectangle bounds;

        PamVisual(String clipName, ClipRef clip, Rectangle bounds) {
            this.clipName = clipName;
            this.clip = clip;
            this.bounds = bounds;
        }
    }
}

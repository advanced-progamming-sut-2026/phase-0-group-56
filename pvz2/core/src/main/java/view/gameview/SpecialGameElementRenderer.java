package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import models.entity.Plant;
import models.games.BaseGame;
import models.games.specialgames.Deadline;
import models.games.specialgames.SaveOurSeeds;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.List;
import java.util.Locale;

/**
 * Draws world-space visuals that belong to special level rules rather than to
 * normal entities. It deliberately does not own the shared TextureBank.
 */
public final class SpecialGameElementRenderer implements Disposable {
    private static final String TAG = "SpecialGameElementRenderer";

    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private static final String PROTECTED_TILE_PAM =
        "768/INITIAL/BACKGROUNDS/PROTECT_TILE/PROTECT_TILE.PAM";
    private static final String DEADLINE_ID =
        "IMAGE_ZOMBIE_ZOMBIE_FUTURE_VET_FLAG_ZOMBIE_FUTURE_VET_FLAG_29X200";

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;
    private final TextureRegion deadlineRegion;

    private ClipRef protectedTileClip;
    private Rectangle protectedTileBounds;
    private float animationTime;
    private boolean protectedTileLoading;
    private boolean protectedTileMissing;
    private boolean disposed;

    public SpecialGameElementRenderer(
        FileHandle assetsRoot,
        TextureBank textureBank
    ) {
        if (assetsRoot == null || !assetsRoot.exists()) {
            throw new IllegalArgumentException("assetsRoot must exist");
        }
        if (textureBank == null) {
            throw new IllegalArgumentException("textureBank cannot be null");
        }

        FileHandle explicitPamFolder = assetsRoot.child("pam");
        pamRoot = explicitPamFolder.exists() && explicitPamFolder.isDirectory()
            ? explicitPamFolder
            : assetsRoot.child("IMAGES");
        pamPlayer = new PamPlayer(textureBank, assetsRoot);
        deadlineRegion = safeRegion(textureBank, DEADLINE_ID);
    }

    public static boolean supports(BaseGame game) {
        return game instanceof SaveOurSeeds || game instanceof Deadline;
    }

    public void update(float delta) {
        if (!disposed) {
            animationTime += Math.max(0f, delta);
        }
    }

    public void render(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight
    ) {
        if (disposed
            || batch == null
            || game == null
            || lawnWidth <= 0f
            || lawnHeight <= 0f) {
            return;
        }

        float cellWidth = lawnWidth / COLUMN_COUNT;
        float cellHeight = lawnHeight / ROW_COUNT;

        if (game instanceof SaveOurSeeds saveOurSeeds) {
            renderProtectedTiles(
                batch,
                saveOurSeeds,
                lawnX,
                lawnY,
                cellWidth,
                cellHeight
            );
        }

        if (game instanceof Deadline deadline) {
            renderDeadline(
                batch,
                deadline,
                lawnX,
                lawnY,
                lawnWidth,
                lawnHeight,
                cellWidth
            );
        }
    }

    private void renderProtectedTiles(
        Batch batch,
        SaveOurSeeds game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        if (protectedTileClip == null || protectedTileBounds == null) {
            requestProtectedTile();
            return;
        }

        for (Plant plant : game.getProtectedPlants()) {
            if (plant == null || !plant.isAlive() || plant.getHp() <= 0f) {
                continue;
            }

            int col = plant.getTileIndex();
            int row = plant.getLine();
            if (col < 0 || col >= COLUMN_COUNT || row < 0 || row >= ROW_COUNT) {
                continue;
            }

            float centerX = lawnX + (col + 0.5f) * cellWidth;
            float centerY = lawnY + (row + 0.5f) * cellHeight;
            float sourceWidth = Math.max(1f, protectedTileBounds.width);
            float sourceHeight = Math.max(1f, protectedTileBounds.height);
            float scale = Math.min(
                cellWidth / sourceWidth,
                cellHeight / sourceHeight
            );

            pamPlayer.draw(
                batch,
                protectedTileClip,
                animationTime,
                centerX,
                centerY,
                scale,
                scale,
                true
            );
        }
    }

    private void requestProtectedTile() {
        if (disposed
            || protectedTileClip != null
            || protectedTileLoading
            || protectedTileMissing) {
            return;
        }

        if (pamRoot == null
            || !pamRoot.exists()
            || !pamRoot.child(PROTECTED_TILE_PAM).exists()) {
            protectedTileMissing = true;
            Gdx.app.error(TAG, "PAM not found: " + PROTECTED_TILE_PAM);
            return;
        }

        protectedTileLoading = true;
        pamPlayer.loadAsync(PROTECTED_TILE_PAM, this::onProtectedTileLoaded);
    }

    private void onProtectedTileLoaded() {
        try {
            if (disposed) {
                return;
            }

            List<String> clips = pamPlayer.clips(PROTECTED_TILE_PAM);
            String clipName = chooseClip(clips);
            if (clipName == null) {
                protectedTileMissing = true;
                Gdx.app.error(TAG, "PAM has no clips: " + PROTECTED_TILE_PAM);
                return;
            }

            ClipRef clip = pamPlayer.getClip(PROTECTED_TILE_PAM, clipName);
            Rectangle bounds = pamPlayer.bounds(PROTECTED_TILE_PAM, clipName);
            if (clip == null || bounds == null) {
                protectedTileMissing = true;
                Gdx.app.error(
                    TAG,
                    "Could not resolve clip/bounds: "
                        + PROTECTED_TILE_PAM
                        + " / "
                        + clipName
                );
                return;
            }

            protectedTileClip = clip;
            protectedTileBounds = new Rectangle(bounds);
        } catch (RuntimeException exception) {
            protectedTileMissing = true;
            Gdx.app.error(TAG, "Failed to load PAM: " + PROTECTED_TILE_PAM, exception);
        } finally {
            protectedTileLoading = false;
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

    private void renderDeadline(
        Batch batch,
        Deadline game,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight,
        float cellWidth
    ) {
        if (deadlineRegion == null) {
            return;
        }

        int deadlineColumn = game.getDeadLine();

        // Loss happens at tileIndex <= deadlineColumn, therefore the visible
        // boundary belongs on the right edge of that column.
        float boundaryX = lawnX + (deadlineColumn + 1f) * cellWidth;
        boundaryX = Math.max(lawnX, Math.min(lawnX + lawnWidth, boundaryX));

        // The supplied texture is intentionally very narrow (29x200). Keep it
        // line-like instead of allowing aspect-ratio scaling to make it huge.
        float lineWidth = Math.max(6f, cellWidth * 0.18f);
        float x = boundaryX - lineWidth * 0.5f;

        batch.draw(
            deadlineRegion,
            x,
            lawnY,
            lineWidth,
            lawnHeight
        );
    }

    private TextureRegion safeRegion(TextureBank textureBank, String resourceId) {
        try {
            TextureRegion region = textureBank.region(resourceId);
            if (region == null) {
                Gdx.app.error(TAG, "Texture resource was not found: " + resourceId);
            }
            return region;
        } catch (RuntimeException exception) {
            Gdx.app.error(TAG, "Failed to load texture resource: " + resourceId, exception);
            return null;
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        protectedTileClip = null;
        protectedTileBounds = null;
        // PamPlayer shares the TextureBank owned by GameView.
    }
}

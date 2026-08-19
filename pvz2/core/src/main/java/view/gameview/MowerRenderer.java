package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import models.entity.LawnMower;
import models.gameadventure.Chapters;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders chapter-specific lawn mower PAM animations.
 *
 * The TextureBank is shared with GameView and is NOT owned/disposed here.
 */
public final class MowerRenderer implements Disposable {
    private static final String TAG = "MowerRenderer";

    private static final float PAM_REFERENCE_SIZE = 390f;
    private static final float TARGET_WIDTH_IN_CELLS = 1.18f;
    private static final float TARGET_HEIGHT_IN_ROWS = 0.78f;
    private static final float FALLBACK_FILL = 1.35f;

    private final PamPlayer pamPlayer;
    private final String pamPath;

    private MowerVisual visual;
    private boolean loading;
    private boolean missing;
    private boolean disposed;

    public MowerRenderer(
        FileHandle assetsRoot,
        TextureBank sharedTextureBank,
        Chapters chapter
    ) {
        if (assetsRoot == null) {
            throw new IllegalArgumentException("assetsRoot cannot be null");
        }
        if (sharedTextureBank == null) {
            throw new IllegalArgumentException("sharedTextureBank cannot be null");
        }
        if (chapter == null) {
            throw new IllegalArgumentException("chapter cannot be null");
        }

        this.pamPath = pamForChapter(chapter);
        this.pamPlayer = new PamPlayer(sharedTextureBank, assetsRoot);

        FileHandle pamRoot = assetsRoot.child("pam");
        if (!pamRoot.exists()) {
            pamRoot = assetsRoot.child("IMAGES");
        }

        if (!pamRoot.child(pamPath).exists()) {
            missing = true;
            Gdx.app.error(TAG, "Mower PAM not found: " + pamPath);
            return;
        }

        requestLoad();
    }

    public void render(
        Batch batch,
        LawnMower mower,
        float centerX,
        float centerY,
        float cellWidth,
        float rowHeight
    ) {
        if (disposed
            || missing
            || batch == null
            || mower == null
            || mower.isUsed()) {
            return;
        }

        if (visual == null) {
            requestLoad();
            return;
        }

        float scale = calculateScale(
            visual.referenceBounds,
            cellWidth,
            rowHeight
        );

        float drawX = centerX;
        float drawY = centerY;

        // PamPlayer bounds use canvas-centred coordinates with Y pointing
        // down. Re-centre the visible mower body on the lane centre instead
        // of centring the whole 390x390 transparent PAM canvas.
        Rectangle bounds = visual.referenceBounds;
        if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
            float boundsCenterX = bounds.x + bounds.width * 0.5f;
            float boundsCenterY = bounds.y + bounds.height * 0.5f;

            drawX -= boundsCenterX * scale;
            drawY += boundsCenterY * scale;
        }

        float time = Math.max(0f, mower.getStateTime());

        if (mower.getState() == LawnMower.State.IDLE) {
            drawSequence(
                batch,
                visual.idleClips,
                time,
                drawX,
                drawY,
                scale
            );
            return;
        }

        if (mower.getState() == LawnMower.State.RUNNING) {
            if (visual.transition != null
                && time < visual.transition.duration) {

                pamPlayer.draw(
                    batch,
                    visual.transition,
                    time,
                    drawX,
                    drawY,
                    scale,
                    scale,
                    false
                );
                return;
            }

            float attackTime = time;
            if (visual.transition != null) {
                attackTime -= visual.transition.duration;
            }

            drawSequence(
                batch,
                visual.attackClips,
                Math.max(0f, attackTime),
                drawX,
                drawY,
                scale
            );
        }
    }

    private void requestLoad() {
        if (disposed || loading || missing || visual != null) {
            return;
        }

        loading = true;
        pamPlayer.loadAsync(pamPath, this::onLoaded);
    }

    private void onLoaded() {
        try {
            if (disposed) {
                return;
            }

            List<String> names = pamPlayer.clips(pamPath);
            if (names == null || names.isEmpty()) {
                missing = true;
                Gdx.app.error(TAG, "Mower PAM has no clips: " + pamPath);
                return;
            }

            List<ClipRef> idleClips = clipsStartingWith(names, "idle");
            List<ClipRef> attackClips = clipsStartingWith(names, "attack");
            ClipRef transition = exactClip(names, "transition");

            if (idleClips.isEmpty()) {
                ClipRef fallback = pamPlayer.getClip(pamPath, names.get(0));
                if (fallback != null) {
                    idleClips.add(fallback);
                }
            }

            if (attackClips.isEmpty()) {
                attackClips.addAll(idleClips);
            }

            if (idleClips.isEmpty()) {
                missing = true;
                Gdx.app.error(TAG, "Could not resolve mower clips: " + pamPath);
                return;
            }

            String boundsClipName = firstNameStartingWith(names, "idle");
            if (boundsClipName == null) {
                boundsClipName = names.get(0);
            }

            Rectangle referenceBounds = pamPlayer.bounds(
                pamPath,
                boundsClipName
            );

            visual = new MowerVisual(
                idleClips,
                transition,
                attackClips,
                referenceBounds
            );

            Gdx.app.log(
                TAG,
                "Loaded " + pamPath
                    + " [idle=" + idleClips.size()
                    + ", transition=" + (transition != null)
                    + ", attack=" + attackClips.size() + "]"
            );
        } catch (RuntimeException exception) {
            missing = true;
            Gdx.app.error(TAG, "Failed to prepare mower PAM: " + pamPath, exception);
        } finally {
            loading = false;
        }
    }

    private List<ClipRef> clipsStartingWith(
        List<String> names,
        String prefix
    ) {
        List<ClipRef> result = new ArrayList<>();
        String wanted = prefix.toLowerCase(Locale.ROOT);

        for (String name : names) {
            if (name == null
                || !name.toLowerCase(Locale.ROOT).startsWith(wanted)) {
                continue;
            }

            ClipRef clip = pamPlayer.getClip(pamPath, name);
            if (clip != null) {
                result.add(clip);
            }
        }

        return result;
    }

    private ClipRef exactClip(List<String> names, String wanted) {
        for (String name : names) {
            if (name != null && name.equalsIgnoreCase(wanted)) {
                return pamPlayer.getClip(pamPath, name);
            }
        }
        return null;
    }

    private static String firstNameStartingWith(
        List<String> names,
        String prefix
    ) {
        String wanted = prefix.toLowerCase(Locale.ROOT);
        for (String name : names) {
            if (name != null
                && name.toLowerCase(Locale.ROOT).startsWith(wanted)) {
                return name;
            }
        }
        return null;
    }

    private void drawSequence(
        Batch batch,
        List<ClipRef> clips,
        float time,
        float x,
        float y,
        float scale
    ) {
        if (clips == null || clips.isEmpty()) {
            return;
        }

        if (clips.size() == 1) {
            pamPlayer.draw(
                batch,
                clips.get(0),
                time,
                x,
                y,
                scale,
                scale,
                true
            );
            return;
        }

        float totalDuration = 0f;
        for (ClipRef clip : clips) {
            totalDuration += Math.max(0.0001f, clip.duration);
        }

        float localTime = time % totalDuration;
        if (localTime < 0f) {
            localTime += totalDuration;
        }

        for (ClipRef clip : clips) {
            float duration = Math.max(0.0001f, clip.duration);
            if (localTime < duration) {
                pamPlayer.draw(
                    batch,
                    clip,
                    localTime,
                    x,
                    y,
                    scale,
                    scale,
                    false
                );
                return;
            }
            localTime -= duration;
        }
    }

    private static float calculateScale(
        Rectangle bounds,
        float cellWidth,
        float rowHeight
    ) {
        if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
            float scaleByWidth =
                cellWidth * TARGET_WIDTH_IN_CELLS / bounds.width;
            float scaleByHeight =
                rowHeight * TARGET_HEIGHT_IN_ROWS / bounds.height;

            return Math.min(scaleByWidth, scaleByHeight);
        }

        float cellSize = Math.min(cellWidth, rowHeight);
        return cellSize / PAM_REFERENCE_SIZE * FALLBACK_FILL;
    }

    private static String pamForChapter(Chapters chapter) {
        return switch (chapter) {
            case BigWaveBeach ->
                "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM";
            case DarkAge ->
                "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM";
            case FrozenCaves ->
                "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM";
            case AncientEgypt ->
                "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM";
        };
    }

    @Override
    public void dispose() {
        disposed = true;
        visual = null;

        // PamPlayer does not own the TextureBank, and this renderer receives
        // GameView's shared bank, so there is intentionally nothing to dispose
        // here besides dropping local references/state.
    }

    private static final class MowerVisual {
        private final List<ClipRef> idleClips;
        private final ClipRef transition;
        private final List<ClipRef> attackClips;
        private final Rectangle referenceBounds;

        private MowerVisual(
            List<ClipRef> idleClips,
            ClipRef transition,
            List<ClipRef> attackClips,
            Rectangle referenceBounds
        ) {
            this.idleClips = idleClips;
            this.transition = transition;
            this.attackClips = attackClips;
            this.referenceBounds = referenceBounds;
        }
    }
}

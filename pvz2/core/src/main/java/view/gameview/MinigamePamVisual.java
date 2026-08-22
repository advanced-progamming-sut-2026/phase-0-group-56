package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.List;
import java.util.Locale;

/** One asynchronously loaded PAM visual used by a minigame overlay. */
final class MinigamePamVisual {
    private static final String TAG = "MinigamePamVisual";

    private final String path;
    private final PamPlayer player;
    private final FileHandle pamRoot;
    private ClipRef clip;
    private Rectangle bounds;
    private boolean loading;
    private boolean missing;

    MinigamePamVisual(FileHandle assetsRoot, TextureBank textureBank, String path) {
        this.path = path;
        FileHandle explicit = assetsRoot.child("pam");
        pamRoot = explicit.exists() ? explicit : assetsRoot.child("IMAGES");
        player = new PamPlayer(textureBank, assetsRoot);
        request();
    }

    boolean isReady() {
        return clip != null && bounds != null;
    }

    void draw(Batch batch, float time, float centerX, float centerY, float width, float height) {
        if (!isReady()) {
            request();
            return;
        }
        float sourceWidth = Math.max(1f, bounds.width);
        float sourceHeight = Math.max(1f, bounds.height);
        float scale = Math.min(width / sourceWidth, height / sourceHeight);
        player.draw(batch, clip, time, centerX, centerY, scale, scale, true);
    }

    private void request() {
        if (loading || missing || clip != null) {
            return;
        }
        if (!pamRoot.child(path).exists()) {
            missing = true;
            Gdx.app.error(TAG, "PAM not found: " + path);
            return;
        }
        loading = true;
        player.loadAsync(path, this::finishLoading);
    }

    private void finishLoading() {
        try {
            List<String> clips = player.clips(path);
            String selected = chooseClip(clips);
            if (selected == null) {
                missing = true;
                return;
            }
            clip = player.getClip(path, selected);
            Rectangle loadedBounds = player.bounds(path, selected);
            bounds = loadedBounds == null ? null : new Rectangle(loadedBounds);
            missing = clip == null || bounds == null;
        } catch (RuntimeException exception) {
            missing = true;
            Gdx.app.error(TAG, "Failed to load PAM: " + path, exception);
        } finally {
            loading = false;
        }
    }

    private static String chooseClip(List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }
        String[] preferred = {"animation", "idle", "loop", "anim", "default"};
        for (String wanted : preferred) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(wanted)) {
                    return clip;
                }
            }
        }
        for (String clip : clips) {
            String normalized = clip == null ? "" : clip.toLowerCase(Locale.ROOT);
            if (normalized.contains("idle") || normalized.contains("loop")) {
                return clip;
            }
        }
        return clips.get(0);
    }
}

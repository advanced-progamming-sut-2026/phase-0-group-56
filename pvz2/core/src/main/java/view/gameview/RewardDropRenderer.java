package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import models.entity.RewardDrop;
import models.gamepanes.Tile;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders reward drops with their supplied PAM animations and safe fallbacks. */
public final class RewardDropRenderer implements Disposable {
    private static final int MODEL_COLUMNS = 9;
    private static final int MODEL_ROWS = 5;
    private static final float SIZE = 52f;
    private static final float HIT_PADDING = 12f;
    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;
    private final TextureBank textureBank;
    private final Map<RewardDrop.Type, DropVisual> loaded = new EnumMap<>(RewardDrop.Type.class);
    private final Set<RewardDrop.Type> loading = new HashSet<>();
    private final Set<RewardDrop.Type> missing = new HashSet<>();
    private final Rectangle scratch = new Rectangle();
    private float animationTime;
    private boolean disposed;

    public RewardDropRenderer(FileHandle assetsRoot, TextureBank textureBank) {
        this.textureBank = textureBank;
        this.pamRoot = assetsRoot == null ? null : assetsRoot.child("IMAGES");
        this.pamPlayer = assetsRoot == null || textureBank == null
            ? null : new PamPlayer(textureBank, assetsRoot);
    }

    public void update(float delta) {
        animationTime += Math.max(0f, delta);
    }

    public void render(RewardDrop drop, Batch batch, Rectangle lawnBounds) {
        if (disposed || drop == null || batch == null || lawnBounds == null) return;
        Rectangle bounds = boundsOf(drop, lawnBounds, 0f);
        DropVisual visual = loaded.get(drop.getType());
        if (visual == null && pamPlayer != null) request(drop.getType());
        if (visual != null) {
            float scale = Math.min(bounds.width / Math.max(1f, visual.bounds.width),
                bounds.height / Math.max(1f, visual.bounds.height));
            pamPlayer.draw(batch, visual.clip, animationTime,
                bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f,
                scale, scale, true);
            return;
        }
        TextureRegion fallback = fallback(drop.getType());
        if (fallback != null) batch.draw(fallback, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public RewardDrop hitTest(List<RewardDrop> drops, float worldX, float worldY,
                              Rectangle lawnBounds) {
        if (drops == null) return null;
        for (int i = drops.size() - 1; i >= 0; i--) {
            RewardDrop drop = drops.get(i);
            if (drop != null && boundsOf(drop, lawnBounds, HIT_PADDING).contains(worldX, worldY)) {
                return drop;
            }
        }
        return null;
    }

    private Rectangle boundsOf(RewardDrop drop, Rectangle lawnBounds, float padding) {
        float sx = lawnBounds.width / (MODEL_COLUMNS * Tile.getWidth());
        float sy = lawnBounds.height / (MODEL_ROWS * Tile.getHeight());
        return scratch.set(lawnBounds.x + (drop.getX() - padding) * sx,
            lawnBounds.y + (drop.getY() - padding) * sy,
            (SIZE + 2f * padding) * sx, (SIZE + 2f * padding) * sy);
    }

    private void request(RewardDrop.Type type) {
        if (disposed || pamPlayer == null || type == null || loading.contains(type)
            || loaded.containsKey(type) || missing.contains(type)) return;
        String path = pathFor(type);
        if (pamRoot == null || !pamRoot.child(path).exists()) {
            missing.add(type);
            Gdx.app.error("RewardDropRenderer", "Drop PAM not found: " + path);
            return;
        }
        loading.add(type);
        pamPlayer.loadAsync(path, () -> onLoaded(type, path));
    }

    private void onLoaded(RewardDrop.Type type, String path) {
        try {
            List<String> clips = pamPlayer.clips(path);
            String clipName = clips == null || clips.isEmpty() ? null : clips.get(0);
            for (String preferred : new String[]{"idle", "animation", "loop", "default"}) {
                if (clips != null) for (String clip : clips)
                    if (clip != null && clip.equalsIgnoreCase(preferred)) clipName = clip;
            }
            ClipRef clip = clipName == null ? null : pamPlayer.getClip(path, clipName);
            Rectangle bounds = clipName == null ? null : pamPlayer.bounds(path, clipName);
            if (clip == null || bounds == null) throw new IllegalStateException("No valid drop clip");
            loaded.put(type, new DropVisual(clip, new Rectangle(bounds)));
        } catch (RuntimeException e) {
            missing.add(type);
            Gdx.app.error("RewardDropRenderer", "Failed to load drop PAM: " + path, e);
        } finally {
            loading.remove(type);
        }
    }

    private TextureRegion fallback(RewardDrop.Type type) {
        if (textureBank == null) return null;
        String id = type == RewardDrop.Type.PLANT_FOOD
            ? "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON_DOWN" : null;
        if (id == null) return null;
        try { return textureBank.region(id); } catch (RuntimeException ignored) { return null; }
    }

    private static String pathFor(RewardDrop.Type type) {
        return switch (type) {
            case COIN_GOLD -> "768/INITIAL/EFFECTS/COIN_GOLD/COIN_GOLD.PAM";
            case COIN_SILVER -> "768/INITIAL/EFFECTS/COIN_SILVER/COIN_SILVER.PAM";
            case DIAMOND -> "768/INITIAL/EFFECTS/COIN_DIAMOND/COIN_DIAMOND.PAM";
            case PLANT_FOOD -> "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";
        };
    }

    @Override public void dispose() { disposed = true; loaded.clear(); loading.clear(); missing.clear(); }
    private record DropVisual(ClipRef clip, Rectangle bounds) { }
}

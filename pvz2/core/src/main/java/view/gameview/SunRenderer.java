package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import models.entity.Sun;
import models.gamepanes.Tile;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Renders collectible suns in lawn coordinates, using their PAM animation. */
public final class SunRenderer implements Disposable {
    private static final String TAG = "SunRenderer";
    private static final int MODEL_COLUMNS = 9;
    private static final int MODEL_ROWS = 5;
    private static final float MODEL_SUN_WIDTH = 50f;
    private static final float MODEL_SUN_HEIGHT = 50f;
    private static final float HIT_PADDING_MODEL = 10f;

    private static final String SUN_PAM =
        "768/INITIAL/EFFECTS/SUN/SUN.PAM";
    private static final String SUN_BOMB_PAM =
        "768/FULL/EFFECTS/SUN_BOMB/SUN_BOMB.PAM";

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;
    private final Drawable fallbackDrawable;
    private final TextureRegion fallbackRegion;
    private final Map<Sun.AnimationType, SunVisual> loaded = new HashMap<>();
    private final Set<Sun.AnimationType> loading = new HashSet<>();
    private final Set<Sun.AnimationType> missing = new HashSet<>();
    private final Rectangle boundsScratch = new Rectangle();
    private float animationTime;
    private boolean disposed;

    /** Compatibility constructor for callers that only have a texture bank. */
    public SunRenderer(TextureBank textureBank, Drawable fallbackDrawable) {
        this(null, textureBank, fallbackDrawable);
    }

    /** Preferred constructor; PAM files are resolved relative to assetsRoot. */
    public SunRenderer(
        FileHandle assetsRoot,
        TextureBank textureBank,
        Drawable fallbackDrawable
    ) {
        this.fallbackDrawable = fallbackDrawable;
        this.fallbackRegion = resolveWorldSunRegion(textureBank);
        if (assetsRoot != null && assetsRoot.exists() && textureBank != null) {
            FileHandle explicitPamFolder = assetsRoot.child("pam");
            this.pamRoot = explicitPamFolder.exists()
                ? explicitPamFolder
                : assetsRoot.child("IMAGES");
            this.pamPlayer = new PamPlayer(textureBank, assetsRoot);
        } else {
            this.pamRoot = null;
            this.pamPlayer = null;
        }
        if (fallbackRegion == null && fallbackDrawable == null && pamPlayer == null) {
            Gdx.app.error(TAG, "No sun PAM, texture, or PvzSkin fallback was found.");
        }
    }

    /** Advances presentation-only animation time. */
    public void update(float delta, Iterable<Sun> suns) {
        if (disposed) {
            return;
        }
        animationTime += Math.max(0f, delta);
    }

    /** SpriteBatch must already be between begin()/end(). */
    public void render(Sun sun, Batch batch, Rectangle lawnBounds) {
        if (disposed || sun == null || batch == null || lawnBounds == null
            || lawnBounds.width <= 0f || lawnBounds.height <= 0f) {
            return;
        }
        Rectangle bounds = boundsOf(sun, lawnBounds, 0f);
        Sun.AnimationType type = sun.getAnimationType();
        SunVisual visual = loaded.get(type);
        if (visual == null && pamPlayer != null) {
            request(type);
        }

        Color oldColor = batch.getColor();
        float oldR = oldColor.r;
        float oldG = oldColor.g;
        float oldB = oldColor.b;
        float oldA = oldColor.a;
        try {
            if (type == Sun.AnimationType.RADIOACTIVE) {
                batch.setColor(1f, 0.42f, 0.42f, 1f);
            }
            if (visual != null && pamPlayer != null) {
                float sourceWidth = Math.max(1f, visual.bounds.width);
                float sourceHeight = Math.max(1f, visual.bounds.height);
                float scale = Math.min(bounds.width / sourceWidth, bounds.height / sourceHeight);
                pamPlayer.draw(
                    batch,
                    visual.clip,
                    animationTime,
                    bounds.x + bounds.width * 0.5f,
                    bounds.y + bounds.height * 0.5f,
                    Math.max(0.0001f, scale),
                    Math.max(0.0001f, scale),
                    true
                );
            } else if (fallbackRegion != null) {
                batch.draw(fallbackRegion, bounds.x, bounds.y, bounds.width, bounds.height);
            } else if (fallbackDrawable != null) {
                fallbackDrawable.draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } finally {
            batch.setColor(oldR, oldG, oldB, oldA);
        }
    }

    public Sun hitTest(List<Sun> suns, float worldX, float worldY, Rectangle lawnBounds) {
        if (suns == null || lawnBounds == null) {
            return null;
        }
        for (int i = suns.size() - 1; i >= 0; i--) {
            Sun sun = suns.get(i);
            if (sun != null && contains(sun, worldX, worldY, lawnBounds)) {
                return sun;
            }
        }
        return null;
    }

    public boolean contains(Sun sun, float worldX, float worldY, Rectangle lawnBounds) {
        return sun != null && lawnBounds != null
            && boundsOf(sun, lawnBounds, HIT_PADDING_MODEL).contains(worldX, worldY);
    }

    private Rectangle boundsOf(Sun sun, Rectangle lawnBounds, float padding) {
        float modelLawnWidth = MODEL_COLUMNS * Tile.getWidth();
        float modelLawnHeight = MODEL_ROWS * Tile.getHeight();
        float scaleX = modelLawnWidth <= 0f ? 1f : lawnBounds.width / modelLawnWidth;
        float scaleY = modelLawnHeight <= 0f ? 1f : lawnBounds.height / modelLawnHeight;
        return boundsScratch.set(
            lawnBounds.x + (sun.getX() - padding) * scaleX,
            lawnBounds.y + (sun.getY() - padding) * scaleY,
            (MODEL_SUN_WIDTH + padding * 2f) * scaleX,
            (MODEL_SUN_HEIGHT + padding * 2f) * scaleY
        );
    }

    private void request(Sun.AnimationType type) {
        if (disposed || pamPlayer == null || type == null || loaded.containsKey(type)
            || loading.contains(type) || missing.contains(type)) {
            return;
        }
        String path = pathFor(type);
        if (pamRoot == null || !pamRoot.child(path).exists()) {
            missing.add(type);
            Gdx.app.error(TAG, "Sun PAM not found: " + path);
            return;
        }
        loading.add(type);
        pamPlayer.loadAsync(path, () -> onLoaded(type, path));
    }

    private void onLoaded(Sun.AnimationType type, String path) {
        try {
            if (disposed) {
                return;
            }
            List<String> clips = pamPlayer.clips(path);
            String clipName = chooseClip(type, clips);
            if (clipName == null) {
                throw new IllegalStateException("PAM contains no clips");
            }
            ClipRef clip = pamPlayer.getClip(path, clipName);
            Rectangle bounds = pamPlayer.bounds(path, clipName);
            if (clip == null || bounds == null) {
                throw new IllegalStateException("Could not resolve clip bounds");
            }
            loaded.put(type, new SunVisual(clip, new Rectangle(bounds)));
            Gdx.app.log(TAG, type + " -> " + path + " [" + clipName + "]");
        } catch (RuntimeException exception) {
            missing.add(type);
            Gdx.app.error(TAG, "Failed to prepare sun PAM: " + path, exception);
        } finally {
            loading.remove(type);
        }
    }

    private static String chooseClip(Sun.AnimationType type, List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }
        if (type == Sun.AnimationType.SPECIAL) {
            String blue = findIgnoreCase(clips, "blue");
            if (blue != null) {
                return blue;
            }
        }
        for (String preferred : new String[]{"idle", "animation", "loop", "default", "explode"}) {
            String match = findIgnoreCase(clips, preferred);
            if (match != null) {
                return match;
            }
        }
        return clips.get(0);
    }

    private static String findIgnoreCase(List<String> values, String wanted) {
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(wanted)) {
                return value;
            }
        }
        return null;
    }

    private static String pathFor(Sun.AnimationType type) {
        return type == Sun.AnimationType.RADIOACTIVE ? SUN_BOMB_PAM : SUN_PAM;
    }

    private TextureRegion resolveWorldSunRegion(TextureBank bank) {
        if (bank == null) {
            return null;
        }
        String override = System.getProperty("pvz.sun.resource");
        if (override != null && !override.isBlank()) {
            try {
                TextureRegion region = bank.region(override.trim());
                if (region != null) {
                    return region;
                }
            } catch (RuntimeException ignored) {
            }
        }
        ResourceIndex index = bank.getResourceIndex();
        String bestId = null;
        int bestScore = Integer.MIN_VALUE;
        for (String id : index.imageIds()) {
            ResourceIndex.ImageEntry entry = index.image(id);
            if (entry == null) {
                continue;
            }
            String text = (id + " " + entry.path).toUpperCase(Locale.ROOT);
            if (!text.contains("SUN") || containsAny(text,
                "SUNFLOWER", "SEEDPACKET", "SEED_PACKET", "PLANT/", "/PLANT",
                "ZOMBIE", "HUD", "BUTTON", "STORE", "COIN", "WORLDMAP", "WORLD_MAP")) {
                continue;
            }
            int score = 20;
            if (containsAny(text, "SUNLIGHT", "SUN_LIGHT")) score += 160;
            if (containsAny(text, "COLLECT", "PICKUP")) score += 90;
            if (containsAny(text, "DROP", "DROPPING")) score += 60;
            if (entry.aw >= 32 && entry.aw <= 256 && entry.ah >= 32 && entry.ah <= 256) score += 30;
            if (score > bestScore) {
                bestScore = score;
                bestId = id;
            }
        }
        if (bestId == null || bestScore < 80) {
            return null;
        }
        try {
            return bank.region(bestId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        disposed = true;
        loaded.clear();
        loading.clear();
        missing.clear();
    }

    private static final class SunVisual {
        final ClipRef clip;
        final Rectangle bounds;

        SunVisual(ClipRef clip, Rectangle bounds) {
            this.clip = clip;
            this.bounds = bounds;
        }
    }
}

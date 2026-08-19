package view.gameview;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import models.entity.Sun;
import models.gamepanes.Tile;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;

import java.util.List;
import java.util.Locale;

/**
 * Draws collectible Sun entities in the Tiled-map world coordinate system.
 *
 * <p>The model owns falling, landing, lifetime and radioactive behaviour. This
 * class is intentionally render-only: it reads Sun.x/y and maps the model's
 * 9x5, Tile-sized lawn onto GameView's lawnBounds.</p>
 */
public final class SunRenderer {

    private static final int MODEL_COLUMNS = 9;
    private static final int MODEL_ROWS = 5;

    /* Sun.java currently models a sun as 50x50 model units. */
    private static final float MODEL_SUN_WIDTH = 50f;
    private static final float MODEL_SUN_HEIGHT = 50f;

    /* Slightly forgiving click target without changing the visual size. */
    private static final float HIT_PADDING_MODEL = 10f;

    private final TextureRegion worldSunRegion;
    private final Drawable fallbackDrawable;
    private final Rectangle boundsScratch = new Rectangle();

    public SunRenderer(TextureBank textureBank, Drawable fallbackDrawable) {
        this.fallbackDrawable = fallbackDrawable;
        this.worldSunRegion = resolveWorldSunRegion(textureBank);

        if (worldSunRegion == null && fallbackDrawable == null) {
            Gdx.app.error(
                "SunRenderer",
                "No world-sun resource and no PvzSkin fallback were found."
            );
        }
    }

    /** SpriteBatch must already be between begin()/end(). */
    public void render(Sun sun, Batch batch, Rectangle lawnBounds) {
        if (sun == null || batch == null || lawnBounds == null) {
            return;
        }
        if (worldSunRegion == null && fallbackDrawable == null) {
            return;
        }

        Rectangle bounds = boundsOf(sun, lawnBounds, 0f);
        Color oldColor = batch.getColor();
        float oldR = oldColor.r;
        float oldG = oldColor.g;
        float oldB = oldColor.b;
        float oldA = oldColor.a;

        try {
            if (sun.isRadioActive()) {
                batch.setColor(1f, 0.42f, 0.42f, 1f);
            }

            if (worldSunRegion != null) {
                batch.draw(
                    worldSunRegion,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height
                );
            } else {
                fallbackDrawable.draw(
                    batch,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height
                );
            }
        } finally {
            batch.setColor(oldR, oldG, oldB, oldA);
        }
    }

    /**
     * Returns the visually top-most sun under the pointer. Iterating backwards
     * matches the order in which GameView renders the list.
     */
    public Sun hitTest(
        List<Sun> suns,
        float worldX,
        float worldY,
        Rectangle lawnBounds
    ) {
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

    public boolean contains(
        Sun sun,
        float worldX,
        float worldY,
        Rectangle lawnBounds
    ) {
        if (sun == null || lawnBounds == null) {
            return false;
        }
        return boundsOf(sun, lawnBounds, HIT_PADDING_MODEL).contains(worldX, worldY);
    }

    private Rectangle boundsOf(Sun sun, Rectangle lawnBounds, float modelPadding) {
        float modelLawnWidth = MODEL_COLUMNS * Tile.getWidth();
        float modelLawnHeight = MODEL_ROWS * Tile.getHeight();

        float scaleX = modelLawnWidth <= 0f ? 1f : lawnBounds.width / modelLawnWidth;
        float scaleY = modelLawnHeight <= 0f ? 1f : lawnBounds.height / modelLawnHeight;

        float x = lawnBounds.x + (sun.getX() - modelPadding) * scaleX;
        float y = lawnBounds.y + (sun.getY() - modelPadding) * scaleY;
        float width = (MODEL_SUN_WIDTH + modelPadding * 2f) * scaleX;
        float height = (MODEL_SUN_HEIGHT + modelPadding * 2f) * scaleY;

        return boundsScratch.set(x, y, width, height);
    }

    /**
     * Prefer an explicitly pinned original asset. Otherwise discover only strong
     * world-sun candidates. If discovery is uncertain, PvzSkin's deterministic
     * image_ui_hud_ingame_sun drawable remains the fallback.
     */
    private TextureRegion resolveWorldSunRegion(TextureBank textureBank) {
        if (textureBank == null) {
            return null;
        }

        String override = System.getProperty("pvz.sun.resource");
        if (override != null && !override.isBlank()) {
            try {
                TextureRegion region = textureBank.region(override.trim());
                if (region != null) {
                    Gdx.app.log("SunRenderer", "Using pinned sun resource: " + override.trim());
                    return region;
                }
                Gdx.app.error("SunRenderer", "Pinned sun resource was not found: " + override.trim());
            } catch (RuntimeException e) {
                Gdx.app.error("SunRenderer", "Failed to load pinned sun resource: " + override.trim(), e);
            }
        }

        ResourceIndex index = textureBank.getResourceIndex();
        String bestId = null;
        int bestScore = Integer.MIN_VALUE;

        for (String id : index.imageIds()) {
            ResourceIndex.ImageEntry entry = index.image(id);
            if (entry == null) {
                continue;
            }

            String text = (id + " " + entry.path).toUpperCase(Locale.ROOT);
            if (!text.contains("SUN")) {
                continue;
            }

            /* Reject common false positives before scoring. */
            if (containsAny(
                text,
                "SUNFLOWER", "SEEDPACKET", "SEED_PACKET", "PLANT/", "/PLANT",
                "ZOMBIE", "HUD", "BUTTON", "STORE", "COIN", "WORLDMAP", "WORLD_MAP"
            )) {
                continue;
            }

            int score = 20;
            if (containsAny(text, "SUNLIGHT", "SUN_LIGHT")) {
                score += 160;
            }
            if (containsAny(text, "COLLECT", "PICKUP")) {
                score += 90;
            }
            if (containsAny(text, "DROP", "DROPPING")) {
                score += 60;
            }
            if (text.contains("PARTICLE")) {
                score += 45;
            }
            if (containsAny(text, "UI/", "ICON", "PORTRAIT")) {
                score -= 70;
            }
            if (entry.aw >= 32 && entry.aw <= 256 && entry.ah >= 32 && entry.ah <= 256) {
                score += 30;
            }

            if (score > bestScore) {
                bestScore = score;
                bestId = id;
            }
        }

        if (bestId == null || bestScore < 80) {
            return null;
        }

        try {
            TextureRegion region = textureBank.region(bestId);
            if (region != null) {
                Gdx.app.log(
                    "SunRenderer",
                    "Using discovered sun resource: " + bestId + " (score=" + bestScore + ")"
                );
            }
            return region;
        } catch (RuntimeException e) {
            Gdx.app.error("SunRenderer", "Failed to load discovered sun resource: " + bestId, e);
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

}

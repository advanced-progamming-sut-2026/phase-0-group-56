package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;

/**
 * Small, cached catalogue of the extracted PvZ2 UI artwork used by menus.
 *
 * The game already ships the complete extracted asset tree in the repository
 * root.  The selected files are also copied into {@code pvz2/assets/ui}; this
 * class deliberately loads that packaged copy so the menu remains correct when
 * launched from Gradle, IntelliJ, or the assembled desktop jar.
 */
final class MenuVisualAssets {

    private static final String INTERNAL_ROOT = "ui/";

    private static final Map<String, Texture> TEXTURES = new HashMap<>();

    private static final Map<String, String> FILES = Map.ofEntries(
        Map.entry("background", "main_menu_background.png"),
        Map.entry("logo", "main_menu_logo.png"),
        Map.entry("coin", "coin.png"),
        Map.entry("gem", "gem.png"),
        Map.entry("sun", "sun.png"),
        Map.entry("star", "star.png"),
        Map.entry("quest", "quests_button.png"),
        Map.entry("greenhouse", "greenhouse_button.png"),
        Map.entry("minigames", "minigames_button.png"),
        Map.entry("tasks", "tasks_button.png"),
        Map.entry("pause", "pause_button.png"),
        Map.entry("pot", "pot.png")
    );

    private MenuVisualAssets() {
    }

    static Image image(String key) {
        Texture texture = texture(key);
        return texture == null ? null : new Image(new TextureRegion(texture));
    }

    static Drawable drawable(String key) {
        Texture texture = texture(key);
        return texture == null
            ? null
            : new TextureRegionDrawable(new TextureRegion(texture));
    }

    private static Texture texture(String key) {
        if (key == null) {
            return null;
        }

        Texture cached = TEXTURES.get(key);
        if (cached != null) {
            return cached;
        }

        String fileName = FILES.get(key);
        if (fileName == null || !Gdx.files.internal(INTERNAL_ROOT + fileName).exists()) {
            return null;
        }

        try {
            Texture loaded = new Texture(
                Gdx.files.internal(INTERNAL_ROOT + fileName),
                false
            );
            loaded.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            TEXTURES.put(key, loaded);
            return loaded;
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.log("MenuVisualAssets", "Could not load " + fileName, exception);
            }
            return null;
        }
    }

    /** Dispose only at application shutdown; individual screens share these textures. */
    static void dispose() {
        for (Texture texture : TEXTURES.values()) {
            texture.dispose();
        }
        TEXTURES.clear();
    }
}

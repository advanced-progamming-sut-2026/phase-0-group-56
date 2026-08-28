package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central catalogue for menu artwork packaged under {@code assets/ui/menu}.
 *
 * Every screen goes through this class instead of knowing an asset's physical
 * path. The loader keeps the old {@code ui/} names as fallbacks so an older
 * checkout can still start while the menus branch uses the curated bundle.
 */
final class MenuVisualAssets {

    private static final String[] INTERNAL_ROOTS = {
        "ui/menu/",
        "ui/"
    };

    private static final Map<String, Texture> TEXTURES = new HashMap<>();
    private static final Map<String, String[]> FILES;

    static {
        Map<String, String[]> files = new HashMap<>();

        register(files, "background", "mainmenu_background.png", "main_menu_background.png");
        register(files, "logo", "mainmenu_logo.png", "main_menu_logo.png");

        register(files, "coin", "coin.png");
        register(files, "gem", "gem.png");
        register(files, "sun", "sun.png");
        register(files, "plantfood", "plantfood.png");
        register(files, "star", "star.png");
        register(files, "pot", "pot.png");
        register(files, "pause", "pause_button.png");

        register(files, "quest", "quest_icon_up.png", "quests_button.png");
        register(files, "quest_down", "quest_icon_down.png");
        register(files, "greenhouse", "greenhouse_button.png");
        register(files, "minigames", "minigames_button.png");
        register(files, "tasks", "tasks_button.png");
        register(files, "news", "news_icon.png");
        register(files, "settings", "settings_icon.png");
        register(files, "shop", "shop_icon.png");

        register(files, "event_shop", "buttons_hud_event_shop_normal.png");
        register(files, "event_shop_down", "buttons_hud_event_shop_selected.png");
        register(files, "plant_boost", "buttons_hud_plant_boost_normal.png");
        register(files, "plant_boost_down", "buttons_hud_plant_boost_selected.png");
        register(files, "premium", "buttons_premium_normal.png");
        register(files, "premium_down", "buttons_premium_selected.png");
        register(files, "tickets", "buttons_tickets_normal.png");
        register(files, "tickets_down", "buttons_tickets_selected.png");
        register(files, "coin_buy", "buttons_coin_buy_normal.png");
        register(files, "coin_buy_down", "buttons_coin_buy_selected.png");
        register(files, "gems_buy", "GemsBuyButton.png");
        register(files, "gems_buy_down", "GemsBuyButton_Down.png");
        register(files, "generic_currency", "button_generic_currency_normal.png");
        register(files, "generic_currency_down", "button_generic_currency_down.png");
        register(files, "lte_currency", "button_generic_ltecurrency.png");

        register(files, "coin_bg", "coin_bg.png");
        register(files, "coin_small", "coin_icon_small.png");
        register(files, "gem_small", "gem_icon_small.png");
        register(files, "ticket", "ticket_icon.png");
        register(files, "counter_bg", "counter_bg.png");
        register(files, "lock", "lock_small.png");
        register(files, "lock_gold", "lock_small_gold.png");
        register(files, "plantfood_collect", "plantfood_bank_collect.png");
        register(files, "plantfood_slot_filled", "plantfood_bank_filled_slot.png");
        register(files, "plantfood_slot_filling", "plantfood_bank_filling_slot.png");
        register(files, "progress_bg", "progress_bg.png");
        register(files, "progress_fill", "progress_fill.png");
        register(files, "reward1", "reward1_bg.png");
        register(files, "reward3", "reward3_bg.png");
        register(files, "reward4", "reward4_bg.png");
        register(files, "reward5", "reward5_bg.png");
        register(files, "red_dot", "store_red_dot.png");
        register(files, "arrow_right", "button_arrow_right.png");
        register(files, "dave_waist", "dave_waist.png");
        register(files, "settings_tab", "settings_tab.png");

        FILES = Collections.unmodifiableMap(files);
    }

    private MenuVisualAssets() {
    }

    private static void register(
        Map<String, String[]> files,
        String key,
        String... names
    ) {
        files.put(key, names);
    }

    static Image image(String key) {
        Texture texture = texture(key);
        return texture == null
            ? null
            : new Image(new TextureRegion(texture));
    }

    static Drawable drawable(String key) {
        Texture texture = texture(key);
        return texture == null
            ? null
            : new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Creates an ImageButton from extracted artwork. Missing pressed or
     * disabled states safely reuse the normal drawable.
     */
    static ImageButton imageButton(
        String normalKey,
        String pressedKey,
        String disabledKey
    ) {
        Drawable normal = drawable(normalKey);
        if (normal == null) {
            return null;
        }

        Drawable pressed = drawable(pressedKey);
        Drawable disabled = drawable(disabledKey);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = pressed == null ? normal : pressed;
        style.imageChecked = style.imageDown;
        style.imageCheckedDown = style.imageDown;
        style.imageDisabled = disabled == null ? normal : disabled;
        return new ImageButton(style);
    }

    static boolean exists(String key) {
        return locate(key) != null;
    }

    private static Texture texture(String key) {
        if (key == null) {
            return null;
        }

        Texture cached = TEXTURES.get(key);
        if (cached != null) {
            return cached;
        }

        String path = locate(key);
        if (path == null) {
            return null;
        }

        try {
            Texture loaded = new Texture(Gdx.files.internal(path), false);
            loaded.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            TEXTURES.put(key, loaded);
            return loaded;
        } catch (RuntimeException exception) {
            if (Gdx.app != null) {
                Gdx.app.log("MenuVisualAssets", "Could not load " + path, exception);
            }
            return null;
        }
    }

    private static String locate(String key) {
        String[] fileNames = FILES.get(key);
        if (fileNames == null) {
            return null;
        }

        for (String root : INTERNAL_ROOTS) {
            for (String fileName : fileNames) {
                String path = root + fileName;
                if (Gdx.files.internal(path).exists()) {
                    return path;
                }
            }
        }
        return null;
    }

    /** Dispose once at application shutdown; screens share these textures. */
    static void dispose() {
        for (Texture texture : TEXTURES.values()) {
            texture.dispose();
        }
        TEXTURES.clear();
    }
}

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

        // Shared PvZ chrome. Every menu uses the same normal/pressed/disabled
        // language instead of falling back to a generic skin button.
        register(files, "blue_button", "BlueButton.png");
        register(files, "blue_button_down", "BlueButton_Down.png");
        register(files, "brown_button", "BrownButton.png");
        register(files, "brown_button_down", "BrownButton_Down.png");
        register(files, "green_button", "GreenButton.png");
        register(files, "green_button_down", "GreenButton_Down.png");
        register(files, "purple_button", "PurpleButton.png");
        register(files, "purple_button_down", "PurpleButton_Down.png");
        register(files, "disabled_button", "DisabledButton.png");
        register(files, "disabled_button_down", "DisabledButton_Down.png");
        register(files, "green_buy_button", "GreenBuyButton.png");
        register(files, "green_buy_button_down", "GreenBuyButton_Down.png");
        register(files, "close", "close_btn.png");
        register(files, "close_down", "close_down.png");
        register(files, "close_circle", "close_circle.png");
        register(files, "close_circle_down", "close_circle_down.png");
        register(files, "info", "info_button_up.png");
        register(files, "info_down", "info_button_down.png");
        register(files, "arrow_left", "Arrow_Left_Green.png");
        register(files, "arrow_right_green", "Arrow_Right_Green.png");
        register(files, "arrow_up", "Arrow_Up_Green.png");
        register(files, "arrow_down", "Arrow_Down_Orange.png");
        register(files, "leaf_backdrop", "leaf_backdrop.png");
        register(files, "popup_9slice", "popup_9slice.png");

        // Quest, progression, notification and reward artwork.
        register(files, "notification", "Notification_Icon.png");
        register(files, "claim", "claim_small.png");
        register(files, "check", "check_mark_sm.png");
        register(files, "challenge_background", "challenge_background.png");
        register(files, "challenge_progress", "challenge_general_progress.png");
        register(files, "xp_bar", "xp_progress_bar.png");
        register(files, "xp_fill_green", "xp_progress_bar_fill_green.png");
        register(files, "xp_fill_teal", "xp_progress_bar_fill_teal.png");
        register(files, "xp_fill_yellow", "xp_progress_bar_fill_yellow.png");
        register(files, "xp_fill_fuschia", "xp_progress_bar_fill_fuschia.png");
        register(files, "value_badge_1", "value_badge_1.png");
        register(files, "value_badge_2", "value_badge_2.png");
        register(files, "value_badge_3", "value_badge_3.png");
        register(files, "epic_pinata", "epic_reward_pinata.png");
        register(files, "epic_icon", "icon_epic.png");
        register(files, "navdot", "navdot.png");
        register(files, "navdot_fill", "navdot_fill.png");

        // Event currency used by official quest/event screens.
        register(files, "mint", "mint.png");
        register(files, "mint_small", "mint_icon_small.png");
        register(files, "mint_counter", "mint_currency_counter.png");
        register(files, "mint_counter_down", "mint_currency_counter_down.png");

        // A small, curated set of official event tiles for menu cards.
        register(files, "event_beach", "event_icon_beach_up.png");
        register(files, "event_beach_down", "event_icon_beach_down.png");
        register(files, "event_lawn", "event_icon_lawnofdoom_up.png");
        register(files, "event_lawn_down", "event_icon_lawnofdoom_down.png");
        register(files, "event_foodfight", "event_icon_foodfight_up.png");
        register(files, "event_foodfight_down", "event_icon_foodfight_down.png");
        register(files, "event_zcorp", "event_icon_zcorp_up.png");
        register(files, "event_zcorp_down", "event_icon_zcorp_down.png");

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

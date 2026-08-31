package utils;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import models.GameMessage;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.HashMap;
import java.util.Map;

public class MessageAssetManager {
    private static MessageAssetManager instance;

    private final Map<String, TextureRegion> emojiMap = new HashMap<>();
    private final Map<String, String> stickerPamMap = new HashMap<>();
    private final Map<String, String> stickerClipMap = new HashMap<>();
    private final Map<String, String> stickerSoundMap = new HashMap<>();

    private MessageAssetManager() {}

    public static MessageAssetManager getInstance() {
        if (instance == null) instance = new MessageAssetManager();
        return instance;
    }

    public void loadEmojis(TextureBank textures) {
        // 3 simple emojis - using text fallback, but you can load real images from atlas
        // For now we store them as text identifiers; UI will render as Unicode
        emojiMap.put("fire", null);
        emojiMap.put("happy", null);
        emojiMap.put("angry", null);
    }

    public void loadStickers(TextureBank textures, PamPlayer player) {
        // Sticker 1: Banana
        stickerPamMap.put("banana", "768/FULL/PLANT/BANANA/BANANA.PAM");
        stickerClipMap.put("banana", "idle_2");
        stickerSoundMap.put("banana", "banana");
        player.loadSync(stickerPamMap.get("banana"));

        // Sticker 2: Browncoat Zombie shout
        stickerPamMap.put("browncoat", "768/FULL/NPC/BROWNCOATZOMBIE/BROWNCOATZOMBIE.PAM");
        stickerClipMap.put("browncoat", "browncoatzombie_shout");
        stickerSoundMap.put("browncoat", "shout");
        player.loadSync(stickerPamMap.get("browncoat"));

        // Sticker 3: Breakdancer
        stickerPamMap.put("breakdance", "768/FULL/ZOMBIE/ZOMBIE_80S_BREAKDANCER/ZOMBIE_80S_BREAKDANCER.PAM");
        stickerClipMap.put("breakdance", "jam_walk");
        stickerSoundMap.put("breakdance", "dance");
        player.loadSync(stickerPamMap.get("breakdance"));
    }

    public TextureRegion getEmoji(String key) { return emojiMap.get(key); }
    public String getStickerPam(String key) { return stickerPamMap.get(key); }
    public String getStickerClip(String key) { return stickerClipMap.get(key); }
    public String getStickerSound(String key) { return stickerSoundMap.get(key); }
    public boolean isEmoji(String key) { return emojiMap.containsKey(key); }
    public boolean isSticker(String key) { return stickerPamMap.containsKey(key); }

    public void dispose() {
        emojiMap.clear();
        stickerPamMap.clear();
        stickerClipMap.clear();
        stickerSoundMap.clear();
    }
}

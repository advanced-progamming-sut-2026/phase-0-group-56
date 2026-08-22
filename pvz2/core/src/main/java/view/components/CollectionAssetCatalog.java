package view.components;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;
import view.gameview.PvzAssetLocator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Safe, optional portrait lookup for the Collection menu.
 *
 * <p>The menu remains fully usable when the extracted Assets folder is not
 * available or when a particular portrait is missing. No null resource ID is
 * ever passed to TextureBank.</p>
 */
public final class CollectionAssetCatalog implements AutoCloseable {

    private final TextureBank textureBank;
    private final ResourceIndex resourceIndex;
    private final Map<String, String> idCache = new HashMap<>();

    private CollectionAssetCatalog(TextureBank textureBank) {
        this.textureBank = textureBank;
        this.resourceIndex = textureBank.resourceIndex();
    }

    public static CollectionAssetCatalog create() {
        FileHandle assetsRoot = PvzAssetLocator.find();
        if (assetsRoot == null) {
            return null;
        }

        try {
            return new CollectionAssetCatalog(
                new TextureBank("768", assetsRoot)
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public void update() {
        textureBank.update();
    }

    public TextureRegion plantPortrait(PlantType type) {
        if (type == null) {
            return null;
        }
        return regionFor(
            "PLANT_" + type.name(),
            false
        );
    }

    public TextureRegion zombiePortrait(
        ZombieRegistry.ZombieType type
    ) {
        if (type == null) {
            return null;
        }
        return regionFor(
            "ZOMBIE_" + type.name(),
            true
        );
    }

    private TextureRegion regionFor(
        String token,
        boolean zombie
    ) {
        String normalizedToken = normalize(token);
        if (normalizedToken.isEmpty()) {
            return null;
        }

        String cachedId = idCache.get(normalizedToken);
        if (cachedId == null && idCache.containsKey(normalizedToken)) {
            return null;
        }

        if (cachedId == null) {
            cachedId = findBestId(normalizedToken, zombie);
            idCache.put(normalizedToken, cachedId);
        }

        if (cachedId == null || cachedId.isBlank()) {
            return null;
        }

        try {
            return textureBank.region(cachedId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String findBestId(
        String token,
        boolean zombie
    ) {
        String bestId = null;
        int bestScore = Integer.MIN_VALUE;

        for (String id : resourceIndex.imageIds()) {
            if (id == null || id.isBlank()) {
                continue;
            }

            ResourceIndex.ImageEntry entry =
                resourceIndex.image(id);

            if (entry == null) {
                continue;
            }

            String haystack = (
                id + " " + (entry.path == null ? "" : entry.path)
            ).toUpperCase(Locale.ROOT);

            String normalized = normalize(haystack);
            if (!normalized.contains(token)) {
                continue;
            }

            int score = 100;

            if (containsAny(haystack, "PORTRAIT", "ICON", "CARD")) {
                score += 90;
            }

            if (containsAny(haystack, "SEEDPACKET", "SEED_PACKET")) {
                score += zombie ? -100 : 40;
            }

            if (containsAny(haystack, "ZOMBIE")) {
                score += zombie ? 80 : -180;
            } else if (zombie) {
                score -= 120;
            }

            if (containsAny(haystack, "PLANT")) {
                score += zombie ? -140 : 55;
            }

            if (entry.aw >= 40 && entry.aw <= 500
                && entry.ah >= 40 && entry.ah <= 500) {
                score += 25;
            }

            if (score > bestScore) {
                bestScore = score;
                bestId = id;
            }
        }

        return bestId;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");
    }

    private static boolean containsAny(
        String value,
        String... tokens
    ) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        textureBank.dispose();
    }
}

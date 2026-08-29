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

    private static final Map<String, String> PLANT_ALIASES = plantAliases();
    private static final Map<String, String> ZOMBIE_ALIASES = zombieAliases();

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

    /** Returns an exact official UI region from the extracted asset bundle. */
    public TextureRegion uiRegion(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }
        try {
            return textureBank.region(resourceId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public TextureRegion plantPortrait(PlantType type) {
        if (type == null) {
            return null;
        }
        return regionFor(
            "PLANT_" + type.name(),
            "IMAGE_UI_PACKETS_",
            PLANT_ALIASES
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
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_",
            ZOMBIE_ALIASES
        );
    }

    private TextureRegion regionFor(
        String token,
        String resourcePrefix,
        Map<String, String> aliases
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
            cachedId = findExactId(
                normalizedToken,
                resourcePrefix,
                aliases
            );
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

    /**
     * Only resolve an explicit official almanac packet. The old implementation
     * used a loose substring search, which could silently select another plant
     * (for example a mint or a projectile) when a name was missing.
     */
    private String findExactId(
        String token,
        String resourcePrefix,
        Map<String, String> aliases
    ) {
        String alias = aliases.get(token);
        if (alias != null && resourceIndex.image(alias) != null) {
            return alias;
        }

        String normalizedPrefix = normalize(resourcePrefix);
        String normalizedSuffix = token;
        if (token.startsWith("PLANT")) {
            normalizedSuffix = token.substring("PLANT".length());
        } else if (token.startsWith("ZOMBIE")) {
            normalizedSuffix = token.substring("ZOMBIE".length());
        }

        for (String id : resourceIndex.imageIds()) {
            if (id == null || id.isBlank()) {
                continue;
            }

            String normalizedId = normalize(id);
            if (!normalizedId.startsWith(normalizedPrefix)) {
                continue;
            }

            String suffix = normalizedId.substring(normalizedPrefix.length());
            if (suffix.equals(normalizedSuffix)) {
                return id;
            }
        }

        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");
    }

    private static Map<String, String> plantAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("PLANTGOOPEASHOOTER", "IMAGE_UI_PACKETS_POISONPEASHOOTER");
        aliases.put("PLANTMEGAGATLINGPEA", "IMAGE_UI_PACKETS_MEGAGATLING");
        aliases.put("PLANTICEBERGLETTUCE", "IMAGE_UI_PACKETS_ICEBURG");
        aliases.put("PLANTFUMSHROOM", "IMAGE_UI_PACKETS_FUMESHROOM");
        aliases.put("PLANTPIERCEMINT", "IMAGE_UI_PACKETS_SPEARMINT");
        return aliases;
    }

    private static Map<String, String> zombieAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("ZOMBIEARCADE", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_EIGHTIES_ARCADE");
        aliases.put("ZOMBIEALLSTAR", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_ALLSTAR");
        aliases.put("ZOMBIEPIANIST", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_PIANO");
        aliases.put("ZOMBIENEWSPAPER", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_MODERN_NEWSPAPER");
        aliases.put("ZOMBIEBARRELROLLER", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BARRELROLLER");
        aliases.put("ZOMBIEDODORIDER", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_DODO");
        aliases.put("ZOMBIEHUNTER", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_HUNTER");
        aliases.put("ZOMBIETROGLOBITE", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_ICEAGE_TROGLOBITE");
        aliases.put("ZOMBIEFISHERMAN", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_FISHERMAN");
        aliases.put("ZOMBIESNORKEL", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_SNORKEL");
        aliases.put("ZOMBIEOCTOPUS", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_BEACH_OCTOPUS");
        aliases.put("ZOMBIEJUGGLER", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_JUGGLER");
        aliases.put("ZOMBIEWIZARD", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_WIZARD");
        aliases.put("ZOMBIEKING", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_KING");
        aliases.put("ZOMBIEIMPDRAGON", "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_DARK_IMP_DRAGON");
        return aliases;
    }

    @Override
    public void close() {
        textureBank.dispose();
    }
}

package view.components;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;
import view.gameview.PvzAssetLocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;

import models.entity.Zombie;
import view.gameview.ZombieRenderer;

import view.gameview.ZombieRenderer;
import pvz.libpvz.textures.TextureBank;
import com.badlogic.gdx.files.FileHandle;

/**
 * Safe, optional portrait lookup for the Collection menu.
 *
 * <p>The menu remains fully usable when the extracted Assets folder is not
 * available or when a particular portrait is missing. No null resource ID is
 * ever passed to TextureBank.</p>
 */
public final class CollectionAssetCatalog implements AutoCloseable {

    private static final String PLANT_PACKET_PREFIX =
        "IMAGE_UI_PACKETS_";

    private static final String ZOMBIE_PACKET_PREFIX =
        "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_";

    private final TextureBank textureBank;
    private final ResourceIndex resourceIndex;
    private final List<String> imageIds;
    private final Map<String, String> idCache = new HashMap<>();
    private final ZombieRenderer zombieRenderer;

    private CollectionAssetCatalog(TextureBank textureBank) {
        this.textureBank = textureBank;
        this.resourceIndex = textureBank.resourceIndex();
        this.imageIds = new ArrayList<>(resourceIndex.imageIds());
        Collections.sort(this.imageIds);

        FileHandle assetsRoot = PvzAssetLocator.find();

        this.zombieRenderer = new ZombieRenderer(
            assetsRoot,
            textureBank
        );
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
        String packetKey = plantPacketKey(type);
        return regionFor(
            "P:" + type.name(),
            packetKey.isEmpty()
                ? null
                : PLANT_PACKET_PREFIX + packetKey,
            plantSpriteKey(type),
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
            "Z:" + type.name(),
            ZOMBIE_PACKET_PREFIX + zombiePacketKey(type),
            zombieSpriteKey(type),
            true
        );
    }

    private TextureRegion regionFor(
        String cacheKey,
        String preferredId,
        String fallbackToken,
        boolean zombie
    ) {
        String cachedId = idCache.get(cacheKey);
        if (cachedId == null && idCache.containsKey(cacheKey)) {
            return null;
        }

        if (cachedId == null) {
            if (preferredId != null && resourceIndex.image(preferredId) != null) {
                cachedId = preferredId;
            } else {
                cachedId = findBestSpriteId(fallbackToken, zombie);
            }
            idCache.put(cacheKey, cachedId);
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

    private String findBestSpriteId(String token, boolean zombie) {
        String normalizedToken = normalize(token);
        if (normalizedToken.isEmpty()) {
            return null;
        }

        String bestId = null;
        int bestScore = Integer.MIN_VALUE;

        for (String id : imageIds) {
            if (id == null || id.isBlank()) {
                continue;
            }

            ResourceIndex.ImageEntry entry =
                resourceIndex.image(id);

            if (entry == null) {
                continue;
            }

            String path = entry.path == null
                ? ""
                : entry.path.replace('\\', '/')
                .toUpperCase(Locale.ROOT);

            String folder = pathFolder(path, zombie ? "/ZOMBIE/" : "/PLANT/");
            if (!normalizedToken.equals(normalize(folder))) {
                continue;
            }

            if (entry.aw <= 0 || entry.ah <= 0) {
                continue;
            }

            int minDimension = Math.min(entry.aw, entry.ah);
            int maxDimension = Math.max(entry.aw, entry.ah);
            int score = 1000;

            // Prefer a stable, compact character frame over tiny fragments.
            if (path.contains("/INITIAL/")) {
                score += 140;
            }
            if (path.contains("/FULL/")) {
                score += 100;
            }
            if (minDimension >= 40 && maxDimension <= 220) {
                score += 130;
            }
            score += Math.max(0, 100 - Math.abs(maxDimension - 96));
            score -= Math.abs(entry.aw - entry.ah) * 2;

            if (score > bestScore
                || (score == bestScore && (bestId == null
                || id.compareTo(bestId) < 0))) {
                bestScore = score;
                bestId = id;
            }
        }

        return bestId;
    }

    private static String pathFolder(String path, String marker) {
        int markerStart = path.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }

        int folderStart = markerStart + marker.length();
        int folderEnd = path.indexOf('/', folderStart);
        return folderEnd < 0
            ? path.substring(folderStart)
            : path.substring(folderStart, folderEnd);
    }

    private static String plantPacketKey(PlantType type) {
        return switch (type) {
            case CHERRY_BOMB -> "CHERRY_BOMB";
            case GOO_PEASHOOTER -> "POISONPEASHOOTER";
            case MEGA_GATLING_PEA -> "MEGAGATLING";
            case ICEBERG_LETTUCE -> "ICEBURG";
            case FUM_SHROOM -> "FUMESHROOM";
            case PIERCE_MINT -> "SPEARMINT";
            case ROTOBAGA, CAT_TAIL, CATTAIL_MINT -> "";
            default -> normalize(type.name());
        };
    }

    private static String plantSpriteKey(PlantType type) {
        return switch (type) {
            case ROTOBAGA -> "ROTORUTABAGA";
            case GOO_PEASHOOTER -> "GOOPEASHOOTER";
            case MEGA_GATLING_PEA -> "MEGAGATLING";
            case ICEBERG_LETTUCE -> "ICEBURG";
            case FUM_SHROOM -> "FUMESHROOM";
            case KERNEL_PULT -> "KERNALPULT";
            case PHAT_BEET -> "PHATBEETS";
            case CAT_TAIL -> "CATTAIL";
            default -> normalize(type.name());
        };
    }

    private static String zombiePacketKey(
        ZombieRegistry.ZombieType type
    ) {
        return switch (type) {
            case NORMAL -> "TUTORIAL";
            case CONEHEAD -> "TUTORIAL_ARMOR1";
            case BUCKETHEAD -> "TUTORIAL_ARMOR2";
            case BRICKHEAD -> "TUTORIAL_ARMOR4";
            case KNIGHT -> "DARK_ARMOR4";
            case IMP -> "TUTORIAL_IMP";
            case GARGANTUAR -> "TUTORIAL_GARGANTUAR";
            case ALLSTAR -> "MODERN_ALLSTAR";
            case ARCADe -> "EIGHTIES_ARCADE";
            case PARASOL -> "LOSTCITY_JANE";
            case TURQUOISE -> "LOSTCITY_CRYSTALSKULL";
            case PROSPECTOR -> "PROSPECTOR";
            case PIANIST -> "PIANO";
            case NEWSPAPER -> "MODERN_NEWSPAPER";
            case BARREL_ROLLER -> "BARRELROLLER";
            case RA -> "RA";
            case EXPLORER -> "EXPLORER";
            case TOMB_RAISER -> "TOMB_RAISER";
            case DODO_RIDER -> "ICEAGE_DODO";
            case HUNTER -> "ICEAGE_HUNTER";
            case TROGLOBITE -> "ICEAGE_TROGLOBITE";
            case FISHERMAN -> "BEACH_FISHERMAN";
            case SNORKEL -> "BEACH_SNORKEL";
            case OCTOPUS -> "BEACH_OCTOPUS";
            case JUGGLER -> "DARK_JUGGLER";
            case WIZARD -> "DARK_WIZARD";
            case KING -> "DARK_KING";
            case IMP_DRAGON -> "DARK_IMP_DRAGON";
        };
    }

    private static String zombieSpriteKey(
        ZombieRegistry.ZombieType type
    ) {
        return switch (type) {
            case NORMAL, CONEHEAD, BUCKETHEAD, BRICKHEAD -> "ZOMBIETUTORIAL";
            case KNIGHT -> "ZOMBIEDARKBASIC";
            case IMP -> "ZOMBIETUTORIALIMP";
            case GARGANTUAR -> "ZOMBIETUTORIALGARGANTUAR";
            case ALLSTAR -> "ZOMBIEMODERNALLSTAR";
            case ARCADe -> "ZOMBIE80SARCADE";
            case PARASOL -> "ZOMBIELOSTCITYJANE";
            case TURQUOISE -> "ZOMBIELOSTCITYCRYSTALSKULL";
            case PROSPECTOR -> "ZOMBIEPROSPECTOR";
            case PIANIST -> "ZOMBIEPIANO";
            case NEWSPAPER -> "ZOMBIEMODERNNEWSPAPER";
            case BARREL_ROLLER -> "ZOMBIEPIRATEBARRELPUSHERBARREL";
            case RA -> "ZOMBIEEGYPTRA";
            case EXPLORER -> "ZOMBIEEXPLORER";
            case TOMB_RAISER -> "ZOMBIEEGYPTTOMBRAISER";
            case DODO_RIDER -> "ZOMBIEICEAGEDODORIDER";
            case HUNTER -> "ZOMBIEICEAGEHUNTER";
            case TROGLOBITE -> "ZOMBIEICEAGETROGLOBITE";
            case FISHERMAN -> "ZOMBIEBEACHFISHERMAN";
            case SNORKEL -> "ZOMBIEBEACHSNORKELER";
            case OCTOPUS -> "ZOMBIEBEACHOCTOPUS";
            case JUGGLER -> "ZOMBIEDARKJESTER";
            case WIZARD -> "ZOMBIEDARKWIZARD";
            case KING -> "ZOMBIEDARKKING";
            case IMP_DRAGON -> "ZOMBIEDARKIMPDRAGON";
        };
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");
    }

    public void preloadZombie(Zombie zombie) {

        if (zombie == null) {
            return;
        }

        zombieRenderer.preloadSync(zombie);
    }

    public void renderZombie(
        Batch batch,
        Zombie zombie,
        Rectangle bounds
    ) {

        if (batch == null || zombie == null || bounds == null) {
            return;
        }

        ArrayList<Zombie> list = new ArrayList<>();
        list.add(zombie);

        zombieRenderer.render(
            batch,
            list,
            bounds,
            1f / 60f
        );
    }

    @Override
    public void close() {
        textureBank.dispose();
    }
}

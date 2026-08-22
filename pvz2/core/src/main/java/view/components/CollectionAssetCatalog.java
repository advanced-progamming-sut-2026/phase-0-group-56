package view.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import pvz.libpvz.textures.ResourceIndex;
import pvz.libpvz.textures.TextureBank;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Collection-only adapter for official PvZ portraits.
 *
 * <p>Resource IDs differ between extracted asset releases, so this class searches the
 * libPVZ {@link ResourceIndex} instead of guessing a fixed path. Missing external assets
 * simply result in a text fallback in CollectionView.</p>
 */
public final class CollectionAssetCatalog implements AutoCloseable {
    private static final String RESOLUTION = "768";
    private static final String NO_RESOURCE = "";

    private static final Map<ZombieRegistry.ZombieType, List<String>> ZOMBIE_ALIASES =
        buildZombieAliases();

    private final Map<PlantType, String> plantResourceIds = new EnumMap<>(PlantType.class);
    private final Map<ZombieRegistry.ZombieType, String> zombieResourceIds =
        new EnumMap<>(ZombieRegistry.ZombieType.class);

    private TextureBank textureBank;
    private boolean disposed;

    public CollectionAssetCatalog() {
        FileHandle root = findPvzAssetsRoot();
        if (root == null) {
            return;
        }

        try {
            textureBank = new TextureBank(RESOLUTION, root);
            Gdx.app.log("Collection", "Official PvZ portraits: " + root.path());
        } catch (RuntimeException exception) {
            textureBank = null;
            Gdx.app.error("Collection", "Could not initialise official PvZ portraits.", exception);
        }
    }

    public boolean isAvailable() {
        return !disposed && textureBank != null;
    }

    public void update() {
        if (isAvailable()) {
            textureBank.update();
        }
    }

    public TextureRegion plantPortrait(PlantType type) {
        if (!isAvailable() || type == null) {
            return null;
        }

        String id = plantResourceIds.get(type);
        if (id == null) {
            String resolvedId = findBestImage(List.of(type.name()), false);
            id = resolvedId == null ? NO_RESOURCE : resolvedId;
            plantResourceIds.put(type, id);
        }
        return id.isEmpty() ? null : textureBank.region(id);
    }

    public TextureRegion zombiePortrait(ZombieRegistry.ZombieType type) {
        if (!isAvailable() || type == null) {
            return null;
        }

        String id = zombieResourceIds.get(type);
        if (id == null) {
            ArrayList<String> candidates = new ArrayList<>();
            candidates.add(type.name());
            candidates.addAll(ZOMBIE_ALIASES.getOrDefault(type, List.of()));
            String resolvedId = findBestImage(candidates, true);
            id = resolvedId == null ? NO_RESOURCE : resolvedId;
            zombieResourceIds.put(type, id);
        }
        return id.isEmpty() ? null : textureBank.region(id);
    }

    private String findBestImage(List<String> candidates, boolean zombie) {
        ResourceIndex index = textureBank.resourceIndex();
        String bestId = null;
        int bestScore = Integer.MIN_VALUE;

        for (String id : index.imageIds()) {
            ResourceIndex.ImageEntry entry = index.image(id);
            if (entry == null) {
                continue;
            }

            String haystack = (id + " " + entry.path).toUpperCase(Locale.ROOT);
            String normalizedHaystack = normalize(haystack);
            int nameScore = matchScore(normalizedHaystack, candidates);
            if (nameScore < 0) {
                continue;
            }

            int score = 100 + nameScore;
            if (containsAny(haystack, "ALMANAC", "PORTRAIT", "SEEDPACKET", "SEED_PACKET")) {
                score += 180;
            }
            if (containsAny(haystack, "ICON", "CARD", "PACKET")) {
                score += 70;
            }
            if (containsAny(haystack, "PARTICLE", "PROJECTILE", "SHADOW", "RIG", "ANIM")) {
                score -= 220;
            }
            if (zombie && haystack.contains("ZOMBIE")) {
                score += 80;
            }
            if (!zombie && haystack.contains("ZOMBIE")) {
                score -= 400;
            }
            if (entry.aw >= 40 && entry.aw <= 700 && entry.ah >= 40 && entry.ah <= 700) {
                score += 35;
            }

            if (score > bestScore) {
                bestScore = score;
                bestId = id;
            }
        }

        return bestId;
    }

    private static int matchScore(String haystack, List<String> candidates) {
        int best = -1;
        for (String candidate : candidates) {
            String token = normalize(candidate);
            if (token.isEmpty() || !haystack.contains(token)) {
                continue;
            }
            best = Math.max(best, 100 + token.length());
        }
        return best;
    }

    private static FileHandle findPvzAssetsRoot() {
        ArrayList<FileHandle> candidates = new ArrayList<>();
        String configured = System.getProperty("pvz.assets");
        if (configured != null && !configured.isBlank()) {
            candidates.add(new FileHandle(new File(configured)));
        }

        String[] developmentPaths = {
            "Assets", "../Assets", "../../Assets",
            "pvz-assets", "../pvz-assets", "../../pvz-assets"
        };
        for (String path : developmentPaths) {
            candidates.add(new FileHandle(new File(path)));
        }
        candidates.add(Gdx.files.internal("pvz-assets"));

        for (FileHandle candidate : candidates) {
            FileHandle resolved = resolvePvzAssetsRoot(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static FileHandle resolvePvzAssetsRoot(FileHandle root) {
        if (root == null || !root.exists()) {
            return null;
        }
        if (isPvzAssetsRoot(root)) {
            return root;
        }

        String[] childNames = {
            "Base Assets", "base assets", "BaseAssets", "pvz-assets", "assets"
        };
        for (String childName : childNames) {
            FileHandle child = root.child(childName);
            if (isPvzAssetsRoot(child)) {
                return child;
            }
        }

        try {
            for (FileHandle child : root.list()) {
                if (child.isDirectory() && isPvzAssetsRoot(child)) {
                    return child;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static boolean isPvzAssetsRoot(FileHandle root) {
        return root != null
            && root.exists()
            && (root.child("resources.json").exists() || root.child("RESOURCES.json").exists())
            && (root.child("atlases").exists() || root.child("ATLASES").exists());
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Map<ZombieRegistry.ZombieType, List<String>> buildZombieAliases() {
        EnumMap<ZombieRegistry.ZombieType, List<String>> result =
            new EnumMap<>(ZombieRegistry.ZombieType.class);
        result.put(ZombieRegistry.ZombieType.NORMAL, List.of("ZombieDefault"));
        result.put(ZombieRegistry.ZombieType.CONEHEAD, List.of("ZombieArmor1", "Conehead"));
        result.put(ZombieRegistry.ZombieType.BUCKETHEAD, List.of("ZombieArmor2", "Buckethead"));
        result.put(ZombieRegistry.ZombieType.BRICKHEAD, List.of("ZombieArmor4", "Brickhead"));
        result.put(ZombieRegistry.ZombieType.KNIGHT, List.of("ZombieDarkArmor3"));
        result.put(ZombieRegistry.ZombieType.GARGANTUAR, List.of("ZombieGargantuar"));
        result.put(ZombieRegistry.ZombieType.IMP, List.of("ZombieImp"));
        result.put(ZombieRegistry.ZombieType.ALLSTAR, List.of("ZombieModernAllStar"));
        result.put(ZombieRegistry.ZombieType.ARCADe, List.of("ZombieArcade"));
        result.put(ZombieRegistry.ZombieType.TURQUOISE, List.of("ZombieCamelDefault"));
        result.put(ZombieRegistry.ZombieType.PIANIST, List.of("ZombiePiano"));
        result.put(ZombieRegistry.ZombieType.BARREL_ROLLER, List.of("ZombieBarrel"));
        result.put(ZombieRegistry.ZombieType.TOMB_RAISER, List.of("ZombieTombRaiser"));
        result.put(ZombieRegistry.ZombieType.DODO_RIDER, List.of("ZombieIceAgeDodo"));
        result.put(ZombieRegistry.ZombieType.IMP_DRAGON, List.of("ZombieDarkImpDragon"));
        return result;
    }

    @Override
    public void close() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (textureBank != null) {
            textureBank.dispose();
            textureBank = null;
        }
        plantResourceIds.clear();
        zombieResourceIds.clear();
    }
}

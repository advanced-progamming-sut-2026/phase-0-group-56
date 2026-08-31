package view.gameview;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import models.entity.Zombie;
import models.gamepanes.Tile;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small Vase Breaker-only zombie renderer.
 *
 * The current project branch has no shared ZombieRenderer, so this class maps
 * the zombie names produced by ZombieFactory to PAMs that actually exist in
 * Assets/animations.json. Cone and bucket share the basic Egyptian body
 * animation, with model-driven armor visibility and animation state.
 */
public final class VaseBreakerZombieRenderer {
    private static final Map<String, String> PAM_BY_TYPE = Map.ofEntries(
        Map.entry("normal", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("zombiedefault", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("cone", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("zombiearmor1", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("bucket", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("zombiearmor2", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM"),
        Map.entry("gargantuar", "768/FULL/ZOMBIE/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM"),
        Map.entry("zombiegargantuar", "768/FULL/ZOMBIE/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM"),
        Map.entry("imp", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM"),
        Map.entry("zombieimp", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_IMP/ZOMBIE_EGYPT_IMP.PAM"),
        Map.entry("ra", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
        Map.entry("zombiera", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
        Map.entry("explorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
        Map.entry("zombieexplorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
        Map.entry("tombraiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
        Map.entry("zombietombraiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM")
    );

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;
    private final Map<String, Visual> visuals = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Set<String> missing = new HashSet<>();

    public VaseBreakerZombieRenderer(FileHandle assetsRoot, TextureBank textureBank) {
        if (assetsRoot == null || textureBank == null) {
            throw new IllegalArgumentException("Zombie renderer requires PVZ assets and TextureBank.");
        }
        FileHandle explicitPam = assetsRoot.child("pam");
        pamRoot = explicitPam.exists() ? explicitPam : assetsRoot.child("IMAGES");
        pamPlayer = new PamPlayer(textureBank, assetsRoot);
    }

    public void render(
        Batch batch,
        Iterable<Zombie> zombies,
        Rectangle lawnBounds,
        float delta
    ) {
        if (batch == null || zombies == null || lawnBounds == null) {
            return;
        }

        float cellWidth = lawnBounds.width / 9f;
        float cellHeight = lawnBounds.height / 5f;
        float modelBoardWidth = 9f * Tile.getWidth();

        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            String key = normalize(zombie.getType());
            String pamPath = resolvePamPath(key);
            if (pamPath == null) {
                continue;
            }

            Visual visual = visuals.get(pamPath);
            if (visual == null) {
                request(pamPath);
                continue;
            }

            zombie.updateAnimation(Math.max(0f, delta));

            float normalizedX = modelBoardWidth <= 0f ? 0f : zombie.getX() / modelBoardWidth;
            float centerX = lawnBounds.x + normalizedX * lawnBounds.width;
            int row = Math.max(0, Math.min(4, zombie.getLine()));
            float centerY = lawnBounds.y + (row + 0.5f) * cellHeight;

            float scale = 0.65f;
            float drawX = centerX;
            float drawY = centerY;

            if (visual.bounds != null && visual.bounds.width > 1f && visual.bounds.height > 1f) {
                float heightMultiplier = isGargantuar(key) ? 1.72f : 1.35f;
                scale = Math.min(
                    (cellWidth * (isGargantuar(key) ? 1.45f : 1.0f)) / visual.bounds.width,
                    (cellHeight * heightMultiplier) / visual.bounds.height
                );
                drawX = centerX - (visual.bounds.x + visual.bounds.width * 0.5f) * scale;
                drawY = centerY - (visual.bounds.y + visual.bounds.height * 0.5f) * scale;
            }

            ClipRef clip = visual.clipFor(zombie);
            if (clip == null) {
                continue;
            }
            pamPlayer.draw(
                batch,
                clip,
                zombie.getStateTime(),
                drawX,
                drawY,
                scale,
                scale,
                true,
                zombie.getVisibilityMap()
            );
        }
    }

    private String resolvePamPath(String key) {
        String mapped = PAM_BY_TYPE.get(key);
        if (mapped != null) {
            return mapped;
        }
        // Unknown Vase Breaker zombie types still get a visible basic zombie.
        return PAM_BY_TYPE.get("normal");
    }

    private void request(String pamPath) {
        if (pamPath == null || pamPath.isBlank()
            || loading.contains(pamPath)
            || missing.contains(pamPath)
            || visuals.containsKey(pamPath)) {
            return;
        }

        if (!pamRoot.child(pamPath).exists()) {
            missing.add(pamPath);
            return;
        }

        loading.add(pamPath);
        pamPlayer.loadAsync(pamPath, () -> {
            try {
                List<String> availableClips = pamPlayer.clips(pamPath);
                if (availableClips == null || availableClips.isEmpty()) {
                    missing.add(pamPath);
                    return;
                }

                String clipName = chooseWalkClip(availableClips);
                ClipRef fallback = pamPlayer.getClip(pamPath, clipName);
                if (fallback == null) {
                    missing.add(pamPath);
                    return;
                }

                Map<String, ClipRef> clipsByName = new HashMap<>();
                for (String available : availableClips) {
                    if (available == null || available.isBlank()) {
                        continue;
                    }
                    ClipRef ref = pamPlayer.getClip(pamPath, available);
                    if (ref != null) {
                        clipsByName.put(available.toLowerCase(Locale.ROOT), ref);
                    }
                }

                Rectangle bounds = null;
                try {
                    bounds = pamPlayer.bounds(pamPath, clipName);
                } catch (RuntimeException ignored) {
                }
                visuals.put(pamPath, new Visual(clipsByName, fallback, bounds));
            } finally {
                loading.remove(pamPath);
            }
        });
    }

    private static String chooseWalkClip(List<String> clips) {
        String[] preferred = {"walk", "walking", "idle"};
        for (String target : preferred) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(target)) {
                    return clip;
                }
            }
        }
        for (String target : preferred) {
            for (String clip : clips) {
                if (clip != null && clip.toLowerCase(Locale.ROOT).contains(target)) {
                    return clip;
                }
            }
        }
        return clips.get(0);
    }

    private static boolean isGargantuar(String key) {
        return key != null && key.contains("gargantuar");
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.toLowerCase(Locale.ROOT).replace("-", "_").trim();
    }

    private static final class Visual {
        private final Map<String, ClipRef> clips;
        private final ClipRef fallback;
        private final Rectangle bounds;

        private Visual(Map<String, ClipRef> clips, ClipRef fallback, Rectangle bounds) {
            this.clips = clips;
            this.fallback = fallback;
            this.bounds = bounds;
        }

        private ClipRef clipFor(Zombie zombie) {
            if (zombie == null) return fallback;
            String requested = switch (zombie.getCurrentState()) {
                case IDLE -> zombie.getIdle();
                case WALKING -> zombie.getWalk();
                case EATING -> zombie.getEat();
                case DYING -> zombie.getDie();
                case FIRING -> zombie.getFire();
                case EXTRA -> zombie.getExtra();
            };
            if (requested != null) {
                ClipRef exact = clips.get(requested.toLowerCase(Locale.ROOT));
                if (exact != null) return exact;
            }
            ClipRef walk = clips.get("walk");
            return walk != null ? walk : fallback;
        }
    }
}

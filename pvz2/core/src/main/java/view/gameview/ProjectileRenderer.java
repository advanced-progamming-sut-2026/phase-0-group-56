package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import models.entity.Projectile;
import models.entity.ProjectileType;
import models.factory.builder.PlantType;
import models.gamepanes.Tile;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure view-side renderer for live projectiles and their impact effects.
 * It shares GameView's TextureBank and never mutates gameplay state.
 */
public final class ProjectileRenderer implements Disposable {
    private static final String TAG = "ProjectileRenderer";
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    // Same source-space convention currently used by PlantRenderer.
    private static final float PAM_REFERENCE_SIZE = 390f;

    private static final String PEA =
        "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String PEA_HIT =
        "768/INITIAL/EFFECTS/SPLAT_PEA/SPLAT_PEA.PAM";
    private static final String SNOW_PEA_HIT =
        "768/INITIAL/EFFECTS/SPLAT_SNOW_PEA/SPLAT_SNOW_PEA.PAM";
    private static final String FIRE_PEA =
        "768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM";
    private static final String FIRE_PEA_HIT =
        "768/INITIAL/EFFECTS/T_SPLAT_FIRE_PEA/T_SPLAT_FIRE_PEA.PAM";
    private static final String GIANT_PEA =
        "768/FULL/EFFECTS/PEAPOD_PLANTFOOD_GIANTPEA/PEAPOD_PLANTFOOD_GIANTPEA.PAM";

    private static final String CITRON =
        "768/FULL/EFFECTS/CITRON_CITRUS_ORB/CITRON_CITRUS_ORB.PAM";
    private static final String CITRON_HIT =
        "768/FULL/EFFECTS/CITRON_CITRUS_ORB_HIT/CITRON_CITRUS_ORB_HIT.PAM";
    private static final String CITRON_PF =
        "768/FULL/EFFECTS/CITRON_PLANTFOOD_ORB/CITRON_PLANTFOOD_ORB.PAM";
    private static final String CITRON_PF_HIT =
        "768/FULL/EFFECTS/CITRON_PLANTFOOD_ORB_HIT/CITRON_PLANTFOOD_ORB_HIT.PAM";

    private static final String STAR =
        "768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/T_STARFRUIT_PROJECTILE.PAM";
    private static final String STAR_HIT =
        "768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE_HIT/T_STARFRUIT_PROJECTILE_HIT.PAM";

    private static final String BOWLING_1 =
        "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/BOWLINGBULB_PROJECTILE1.PAM";
    private static final String BOWLING_2 =
        "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE2/BOWLINGBULB_PROJECTILE2.PAM";
    private static final String BOWLING_3 =
        "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE3/BOWLINGBULB_PROJECTILE3.PAM";
    private static final String BOWLING_PF =
        "768/FULL/EFFECTS/BOWLINGBULB_PLANTFOOD_PROJECTILE/BOWLINGBULB_PLANTFOOD_PROJECTILE.PAM";

    private static final String GOO =
        "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/GOOPEASHOOTER_PROJECTILES.PAM";

    private static final String MELON =
        "768/INITIAL/EFFECTS/T_MELON_PROJECTILE/T_MELON_PROJECTILE.PAM";
    private static final String MELON_HIT =
        "768/INITIAL/EFFECTS/T_SPLAT_MELONPULT/T_SPLAT_MELONPULT.PAM";
    private static final String WINTER_MELON =
        "768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/T_WINTERMELON_PROJECTILE.PAM";
    private static final String WINTER_MELON_HIT =
        "768/FULL/EFFECTS/T_SPLAT_WINTERMELON/T_SPLAT_WINTERMELON.PAM";

    private static final String CABBAGE =
        "768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/T_CABBAGEPULT_PROJECTILE.PAM";
    private static final String CABBAGE_HIT =
        "768/INITIAL/EFFECTS/SPLAT_CABBAGEPULT/SPLAT_CABBAGEPULT.PAM";

    private static final String PEPPER =
        "768/FULL/EFFECTS/PEPPERPULT_PROJECTILE/PEPPERPULT_PROJECTILE.PAM";
    private static final String PEPPER_HIT =
        "768/FULL/EFFECTS/PEPPERPULT_PROJECTILE_PF_SPLAT/PEPPERPULT_PROJECTILE_PF_SPLAT.PAM";

    private static final String CACTUS =
        "768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/T_CACTUS_PROJECTILE.PAM";
    private static final String CACTUS_HIT =
        "768/INITIAL/EFFECTS/CACTUS_PROJECTILE_HIT/CACTUS_PROJECTILE_HIT.PAM";
    private static final String CACTUS_PF =
        "768/INITIAL/EFFECTS/CACTUS_PROJECTILE_PLANTFOOD/CACTUS_PROJECTILE_PLANTFOOD.PAM";

    private static final String PUFF =
        "768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/T_PUFFSHROOM_PROJECTILE.PAM";
    private static final String PUFF_HIT =
        "768/INITIAL/EFFECTS/T_PUFFSHROOM_HIT/T_PUFFSHROOM_HIT.PAM";
    private static final String SEA =
        "768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM";
    private static final String FUME =
        "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM";
    private static final String FUME_HIT =
        "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES_HIT/FUMESHROOM_BUBBLES_HIT.PAM";

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;

    private final Map<String, PamVisual> loaded = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Set<String> missing = new HashSet<>();
    private final ArrayList<Impact> impacts = new ArrayList<>();
    private final Set<Projectile> impactStarted =
        java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private float flightTime;
    private boolean disposed;

    public ProjectileRenderer(FileHandle assetsRoot, TextureBank sharedTextureBank) {
        if (assetsRoot == null) {
            throw new IllegalArgumentException("assetsRoot cannot be null");
        }
        if (sharedTextureBank == null) {
            throw new IllegalArgumentException("sharedTextureBank cannot be null");
        }

        FileHandle explicitPamFolder = assetsRoot.child("pam");
        this.pamRoot = explicitPamFolder.exists()
            ? explicitPamFolder
            : assetsRoot.child("IMAGES");
        this.pamPlayer = new PamPlayer(sharedTextureBank, assetsRoot);
    }

    public void render(
        Batch batch,
        List<Projectile> projectiles,
        Rectangle lawnBounds,
        float delta
    ) {
        if (disposed || batch == null || projectiles == null || lawnBounds == null) {
            return;
        }

        float safeDelta = Math.max(0f, delta);
        flightTime += safeDelta;

        float modelWidth = COLUMN_COUNT * Tile.getWidth();
        float modelHeight = ROW_COUNT * Tile.getHeight();
        if (modelWidth <= 0f || modelHeight <= 0f) {
            return;
        }

        float worldPerModelX = lawnBounds.width / modelWidth;
        float worldPerModelY = lawnBounds.height / modelHeight;
        float cellSize = Math.min(
            lawnBounds.width / COLUMN_COUNT,
            lawnBounds.height / ROW_COUNT
        );
        float basePamScale = cellSize / PAM_REFERENCE_SIZE;

        for (Projectile projectile : projectiles) {
            if (projectile == null || projectile.getType() == null) {
                continue;
            }

            VisualSpec spec = resolveSpec(projectile);
            if (spec == null || spec.flightPam == null) {
                continue;
            }

            float worldX = lawnBounds.x + projectile.getX() * worldPerModelX;
            float worldY = lawnBounds.y + projectile.getY() * worldPerModelY;
            float scale = basePamScale * spec.scaleMultiplier;

            if (projectile.getPierce() <= 0f) {
                if (impactStarted.add(projectile) && spec.hitPam != null) {
                    impacts.add(new Impact(spec.hitPam, worldX, worldY, scale));
                    request(spec.hitPam);
                }
                continue;
            }

            drawPam(batch, spec.flightPam, flightTime, worldX, worldY, scale, true);
        }

        drawImpacts(batch, safeDelta);

        // Do not retain references to dead projectiles forever.
        impactStarted.retainAll(projectiles);
    }

    private void drawImpacts(Batch batch, float delta) {
        Iterator<Impact> iterator = impacts.iterator();
        while (iterator.hasNext()) {
            Impact impact = iterator.next();
            PamVisual visual = loaded.get(impact.pamPath);

            if (visual == null) {
                if (missing.contains(impact.pamPath)) {
                    iterator.remove();
                } else {
                    request(impact.pamPath);
                }
                continue;
            }

            pamPlayer.draw(
                batch,
                visual.clip,
                impact.stateTime,
                impact.x,
                impact.y,
                impact.scale,
                impact.scale,
                false
            );

            impact.stateTime += delta;
            if (impact.stateTime >= Math.max(visual.clip.duration, 0.05f)) {
                iterator.remove();
            }
        }
    }

    private void drawPam(
        Batch batch,
        String pamPath,
        float time,
        float x,
        float y,
        float scale,
        boolean loop
    ) {
        PamVisual visual = loaded.get(pamPath);
        if (visual == null) {
            request(pamPath);
            return;
        }

        pamPlayer.draw(
            batch,
            visual.clip,
            time,
            x,
            y,
            scale,
            scale,
            loop
        );
    }

    private void request(String pamPath) {
        if (disposed || pamPath == null || pamPath.isBlank()
            || loaded.containsKey(pamPath)
            || loading.contains(pamPath)
            || missing.contains(pamPath)) {
            return;
        }

        if (!pamRoot.child(pamPath).exists()) {
            missing.add(pamPath);
            Gdx.app.error(TAG, "PAM not found: " + pamPath);
            return;
        }

        loading.add(pamPath);
        pamPlayer.loadAsync(pamPath, () -> onLoaded(pamPath));
    }

    private void onLoaded(String pamPath) {
        try {
            if (disposed) {
                return;
            }

            List<String> clips = pamPlayer.clips(pamPath);
            String clipName = chooseProjectileClip(clips);
            if (clipName == null) {
                missing.add(pamPath);
                Gdx.app.error(TAG, "PAM has no clips: " + pamPath);
                return;
            }

            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) {
                missing.add(pamPath);
                Gdx.app.error(TAG, "Could not get clip '" + clipName + "' from " + pamPath);
                return;
            }

            loaded.put(pamPath, new PamVisual(clipName, clip));
            Gdx.app.log(TAG, pamPath + " [" + clipName + "]");
        } catch (RuntimeException e) {
            missing.add(pamPath);
            Gdx.app.error(TAG, "Failed to load PAM: " + pamPath, e);
        } finally {
            loading.remove(pamPath);
        }
    }

    private static String chooseProjectileClip(List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }

        String[] preferred = {"animation", "anim", "projectile", "loop", "idle"};
        for (String name : preferred) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(name)) {
                    return clip;
                }
            }
        }

        for (String clip : clips) {
            if (clip == null) {
                continue;
            }
            String normalized = clip.toLowerCase(Locale.ROOT);
            if (normalized.contains("projectile")
                || normalized.contains("loop")
                || normalized.contains("idle")) {
                return clip;
            }
        }

        return clips.get(0);
    }

    private static VisualSpec resolveSpec(Projectile projectile) {
        ProjectileType type = projectile.getType();
        PlantType source = projectile.getSourcePlantType();

        if (type == ProjectileType.PEA) {
            if (source == PlantType.FIRE_PEASHOOTER) {
                return new VisualSpec(FIRE_PEA, FIRE_PEA_HIT, 1.00f);
            }
            if (source == PlantType.GOO_PEASHOOTER) {
                return new VisualSpec(GOO, null, 1.00f);
            }
            if (source == PlantType.SNOW_PEA) {
                return new VisualSpec(PEA, SNOW_PEA_HIT, 0.95f);
            }
            return new VisualSpec(PEA, PEA_HIT, 0.95f);
        }

        return switch (type) {
            case PEA -> new VisualSpec(PEA, PEA_HIT, 0.95f);
            case GIANT_PEA -> new VisualSpec(GIANT_PEA, PEA_HIT, 1.35f);
            case HEAVY_BULLET -> new VisualSpec(CITRON, CITRON_HIT, 1.30f);
            case PLASMA -> new VisualSpec(CITRON_PF, CITRON_PF_HIT, 1.55f);
            case STAR -> new VisualSpec(STAR, STAR_HIT, 0.95f);
            case POISON -> new VisualSpec(GOO, null, 1.00f);
            case ICE -> new VisualSpec(PEA, SNOW_PEA_HIT, 0.95f);

            case ONION_1 -> new VisualSpec(BOWLING_1, null, 1.15f);
            case ONION_2 -> new VisualSpec(BOWLING_2, null, 1.15f);
            case ONION_3 -> new VisualSpec(BOWLING_3, null, 1.15f);
            case Explosive_Onion -> new VisualSpec(BOWLING_PF, null, 1.30f);

            case MELON -> source == PlantType.WINTER_MELON
                ? new VisualSpec(WINTER_MELON, WINTER_MELON_HIT, 1.15f)
                : new VisualSpec(MELON, MELON_HIT, 1.15f);

            case CABBAGE -> new VisualSpec(CABBAGE, CABBAGE_HIT, 1.00f);
            case PEPPER -> new VisualSpec(PEPPER, PEPPER_HIT, 1.10f);
            case CACTUS -> new VisualSpec(CACTUS, CACTUS_HIT, 0.95f);
            case ELECTRICAL_CACTUS -> new VisualSpec(CACTUS_PF, CACTUS_HIT, 1.05f);

            case BUBBLE -> {
                if (source == PlantType.SEA_SHROOM) {
                    yield new VisualSpec(SEA, null, 1.00f);
                }
                if (source == PlantType.FUM_SHROOM) {
                    yield new VisualSpec(FUME, FUME_HIT, 1.05f);
                }
                yield new VisualSpec(PUFF, PUFF_HIT, 0.95f);
            }

            // No supplied PAM yet for these currently-used projectile families.
            case CORN, BUTTER, MAGIC, LIGHTNING, LETTUCE -> null;
        };
    }

    @Override
    public void dispose() {
        disposed = true;
        loaded.clear();
        loading.clear();
        missing.clear();
        impacts.clear();
        impactStarted.clear();
        // TextureBank is owned and disposed by GameView.
    }

    private record VisualSpec(String flightPam, String hitPam, float scaleMultiplier) {}

    private static final class PamVisual {
        private final String clipName;
        private final ClipRef clip;

        private PamVisual(String clipName, ClipRef clip) {
            this.clipName = clipName;
            this.clip = clip;
        }
    }

    private static final class Impact {
        private final String pamPath;
        private final float x;
        private final float y;
        private final float scale;
        private float stateTime;

        private Impact(String pamPath, float x, float y, float scale) {
            this.pamPath = pamPath;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }
}

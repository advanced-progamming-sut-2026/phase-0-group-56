package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import controllers.datacontroller.Data;
import models.entity.EffectType;
import models.entity.Zombie;
import models.entity.ZombieRegistry;
import models.entity.ZombieState;
import models.gamepanes.Tile;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Draws Zombie model objects without putting LibGDX rendering code in the model.
 *
 * <p>The renderer shares GameView's TextureBank, owns one PamPlayer, loads PAMs
 * asynchronously, converts model coordinates to the real TMX pitch and draws
 * the state/part-visibility selected by each live Zombie instance.</p>
 */
public final class ZombieRenderer implements Disposable {
    private static final String TAG = "ZombieRenderer";
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;
    // Zombie PAMs include considerably more transparent canvas than plant
    // PAMs.  Filling only the old 1.05 x 1.50 cell target made ordinary
    // zombies look undersized beside plants and tiles, so give the visible
    // frame a little more room while keeping special sizes relative.
    private static final float ZOMBIE_FILL = 1.22f;

    private static final String DEFAULT_PAM =
        "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";

    /*
     * These paths are the useful rendering data from commit 3efee31. Keeping
     * them in the view avoids coupling Zombie/ZombieFactory to LibGDX assets.
     */
    private static final Map<String, VisualSpec> VISUALS = Map.ofEntries(
        entry("normal", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("zombiedefault", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("cone", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("zombiearmor1", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("bucket", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("zombiearmor2", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("knight", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("zombiedarkarmor3", "768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM"),
        entry("brick", DEFAULT_PAM),
        entry("zombiearmor4", DEFAULT_PAM),
        entry("gargantuar", "768/INITIAL/ZOMBIE/TUTORIAL_GARGANTUAR/TUTORIAL_GARGANTUAR.PAM", 1.55f, 2.25f),
        entry("zombiegargantuar", "768/INITIAL/ZOMBIE/TUTORIAL_GARGANTUAR/TUTORIAL_GARGANTUAR.PAM", 1.55f, 2.25f),
        entry("imp", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM", 0.82f, 1.10f),
        entry("zombieimp", "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM", 0.82f, 1.10f),
        entry("allstar", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM"),
        entry("zombiemodernallstar", "768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM"),
        entry("arcade", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM"),
        entry("zombiearcade", "768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM"),
        entry("parasol", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM"),
        entry("turquoise", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"),
        entry("zombiecameldefault", "768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM"),
        entry("prospector", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
        entry("zombieprospector", "768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM"),
        entry("piano", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM", "idle", 1.60f, 1.65f),
        entry("zombiepiano", "768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM", "idle", 1.60f, 1.65f),
        entry("newspaper", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM", "walk_newspaper", 1.05f, 1.50f),
        entry("zombienewspaper", "768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM", "walk_newspaper", 1.05f, 1.50f),
        entry("barrel", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM"),
        entry("zombiebarrel", "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM"),
        entry("ra", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
        entry("zombiera", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM"),
        entry("explorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
        entry("zombieexplorer", "768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM"),
        entry("tombraiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
        entry("zombietombraiser", "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM"),
        entry("dodo", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM"),
        entry("zombieiceagedodo", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM"),
        entry("hunter", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM"),
        entry("zombieiceagehunter", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM"),
        entry("troglobite", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM"),
        entry("zombieiceagetroglobite", "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM"),
        entry("fisherman", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM"),
        entry("zombiebeachfisherman", "768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM"),
        entry("snorkel", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
        entry("zombiebeachsnorkel", "768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM"),
        entry("octopus", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
        entry("zombiebeachoctopus", "768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM"),
        entry("juggler", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
        entry("zombiedarkjuggler", "768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM"),
        entry("wizard", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
        entry("zombiewizard", "768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM"),
        entry("king", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM", "idle2", 1.05f, 1.50f),
        entry("zombiedarkking", "768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM", "idle2", 1.05f, 1.50f),
        entry("dragon_imp", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM", 0.82f, 1.10f),
        entry("zombiedarkimpdragon", "768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM", 0.82f, 1.10f)
    );

    private final FileHandle pamRoot;
    private final PamPlayer pamPlayer;
    private final Map<VisualSpec, ZombieVisual> loaded = new HashMap<>();
    private final Set<VisualSpec> loading = new HashSet<>();
    private final Set<VisualSpec> missing = new HashSet<>();
    private boolean disposed;

    public ZombieRenderer(
        FileHandle assetsRoot,
        TextureBank sharedTextureBank
    ) {
        if (assetsRoot == null) {
            throw new IllegalArgumentException("assetsRoot cannot be null");
        }
        if (sharedTextureBank == null) {
            throw new IllegalArgumentException("sharedTextureBank cannot be null");
        }

        FileHandle explicitPamRoot = assetsRoot.child("pam");
        this.pamRoot = explicitPamRoot.exists()
            ? explicitPamRoot
            : assetsRoot.child("IMAGES");
        this.pamPlayer = new PamPlayer(sharedTextureBank, assetsRoot);
    }

    /** Batch is already inside begin/end because this is called by Scene2D. */
    public void render(
        Batch batch,
        Iterable<Zombie> zombies,
        Rectangle pitchBounds,
        float delta
    ) {
        if (disposed
            || batch == null
            || zombies == null
            || pitchBounds == null
            || pitchBounds.width <= 0f
            || pitchBounds.height <= 0f) {
            return;
        }

        List<Zombie> drawOrder = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            int row = zombie.getLine();
            if (row < 0 || row >= ROW_COUNT) {
                continue;
            }

            drawOrder.add(zombie);
            discoverForCurrentUser(zombie);
            zombie.updateAnimation(Math.max(0f, delta));
        }

        // Back/high lanes first, front/low lanes last.
        drawOrder.sort(
            Comparator.comparingInt(Zombie::getLine).reversed()
        );

        float cellWidth = pitchBounds.width / COLUMN_COUNT;
        float rowHeight = pitchBounds.height / ROW_COUNT;
        float logicalBoardWidth = COLUMN_COUNT * Tile.getWidth();

        for (Zombie zombie : drawOrder) {
            drawZombie(
                batch,
                zombie,
                pitchBounds,
                cellWidth,
                rowHeight,
                logicalBoardWidth
            );
        }

    }

    /**
     * Synchronously prepares one zombie PAM for a small UI preview. Gameplay
     * continues to use the non-blocking request path in {@link #render}; the
     * Collection screen calls this once when a detail card is opened so its
     * idle animation is available on the first draw.
     */
    public boolean preloadSync(Zombie zombie) {
        if (disposed || zombie == null) {
            return false;
        }

        VisualSpec spec = visualSpec(zombie);
        if (spec == null || loaded.containsKey(spec)) {
            return spec != null && loaded.containsKey(spec);
        }
        if (!pamRoot.child(spec.pamPath).exists()) {
            missing.add(spec);
            return false;
        }

        loading.add(spec);
        missing.remove(spec);
        try {
            pamPlayer.loadSync(spec.pamPath);
            onLoaded(spec);
            return loaded.containsKey(spec);
        } catch (RuntimeException exception) {
            missing.add(spec);
            Gdx.app.error(TAG, "Synchronous zombie PAM preload failed: " + spec.pamPath, exception);
            return false;
        } finally {
            loading.remove(spec);
        }
    }

    /** A rendered live zombie has been seen by the player. Persist discovery
     * only on the transition from locked to unlocked, never once per frame. */
    private static void discoverForCurrentUser(Zombie zombie) {
        if (zombie == null || zombie.getType() == null) {
            return;
        }
        models.User user = Data.getCurrentUser();
        if (user == null) {
            return;
        }
        ZombieRegistry registry = user.getZombieRegistry();
        if (registry.discover(zombie.getType())) {
            Data.saveUser();
            Gdx.app.log(TAG, "Zombie discovered: " + zombie.getType());
        }
    }

    private void drawZombie(
        Batch batch,
        Zombie zombie,
        Rectangle pitchBounds,
        float cellWidth,
        float rowHeight,
        float logicalBoardWidth
    ) {
        VisualSpec spec = visualSpec(zombie);

        ZombieVisual visual = loaded.get(spec);
        if (visual == null) {
            request(spec);
            return;
        }

        float logicalCenterX = zombie.getX() + zombie.getWidth() * 0.5f;
        float normalizedX = logicalBoardWidth <= 0f
            ? 0f
            : logicalCenterX / logicalBoardWidth;

        float centerX = pitchBounds.x + normalizedX * pitchBounds.width;
        float centerY = pitchBounds.y + (zombie.getLine() + 0.5f) * rowHeight;

        float scale = calculateScale(
            visual.bounds,
            cellWidth,
            rowHeight,
            spec
        );

        float drawX = centerX;
        float drawY = centerY;
        if (visual.bounds != null
            && visual.bounds.width > 0f
            && visual.bounds.height > 0f) {

            float boundsCenterX = visual.bounds.x + visual.bounds.width * 0.5f;
            float boundsCenterY = visual.bounds.y + visual.bounds.height * 0.5f;

            drawX -= boundsCenterX * scale;
            drawY += boundsCenterY * scale;
        }

        ClipRef clip = visual.clipFor(zombie);
        if (clip == null) {
            return;
        }

        Color oldColor = batch.getColor();
        float oldR = oldColor.r;
        float oldG = oldColor.g;
        float oldB = oldColor.b;
        float oldA = oldColor.a;
        try {
            // A temporary FROZEN effect slows the zombie but does not encase
            // it. Tint those zombies blue so the gameplay state is visible;
            // fully encased zombies use the ice-block overlay instead.
            if (zombie.hasEffect(EffectType.FROZEN)
                && !zombie.isFrozen()
                && !zombie.isEncasedInIce()) {
                batch.setColor(0.52f, 0.78f, 1f, 1f);
            }
            pamPlayer.draw(
                batch,
                clip,
                zombie.getStateTime(),
                drawX,
                drawY,
                scale,
                scale,
                zombie.getCurrentState() != ZombieState.DYING,
                zombie.getVisibilityMap()
            );
        } finally {
            batch.setColor(oldR, oldG, oldB, oldA);
        }
    }

    private static VisualSpec visualSpec(Zombie zombie) {
        if (zombie == null) {
            return null;
        }
        VisualSpec mappedSpec = VISUALS.getOrDefault(
            normalize(zombie.getType()),
            new VisualSpec(DEFAULT_PAM, "walk", 1.05f, 1.50f)
        );
        // Factory-created zombies carry the authoritative PAM path. Keep the
        // map for legacy/network instances that do not set one, while using
        // the model path when available (notably the brickhead asset).
        return zombie.getPamPath() == null || zombie.getPamPath().isBlank()
            ? mappedSpec
            : new VisualSpec(
            zombie.getPamPath(),
            mappedSpec.preferredClip,
            mappedSpec.widthInCells,
            mappedSpec.heightInRows
        );
    }

    private void request(VisualSpec spec) {
        if (disposed
            || loaded.containsKey(spec)
            || loading.contains(spec)
            || missing.contains(spec)) {
            return;
        }

        if (!pamRoot.child(spec.pamPath).exists()) {
            missing.add(spec);
            Gdx.app.error(TAG, "Zombie PAM not found: " + spec.pamPath);
            return;
        }

        loading.add(spec);
        pamPlayer.loadAsync(spec.pamPath, () -> onLoaded(spec));
    }

    private void onLoaded(VisualSpec spec) {
        try {
            if (disposed) {
                return;
            }

            List<String> clips = pamPlayer.clips(spec.pamPath);
            String clipName = chooseClip(clips, spec.preferredClip);
            if (clipName == null) {
                missing.add(spec);
                Gdx.app.error(TAG, "Zombie PAM has no clips: " + spec.pamPath);
                return;
            }

            ClipRef preferredClip = pamPlayer.getClip(spec.pamPath, clipName);
            if (preferredClip == null) {
                missing.add(spec);
                Gdx.app.error(TAG, "Could not resolve clip " + clipName + " in " + spec.pamPath);
                return;
            }

            Map<String, ClipRef> clipsByName = new HashMap<>();
            for (String availableClip : clips) {
                if (availableClip == null || availableClip.isBlank()) {
                    continue;
                }
                ClipRef ref = pamPlayer.getClip(spec.pamPath, availableClip);
                if (ref != null) {
                    clipsByName.put(availableClip.toLowerCase(Locale.ROOT), ref);
                }
            }

            Rectangle bounds = null;
            try {
                bounds = pamPlayer.bounds(spec.pamPath, clipName);
            } catch (RuntimeException exception) {
                Gdx.app.error(TAG, "Could not read bounds for " + spec.pamPath, exception);
            }

            loaded.put(spec, new ZombieVisual(clipsByName, preferredClip, bounds));
            Gdx.app.log(TAG, spec.pamPath + " [" + clipName + "]");
        } catch (RuntimeException exception) {
            missing.add(spec);
            Gdx.app.error(TAG, "Failed to prepare " + spec.pamPath, exception);
        } finally {
            loading.remove(spec);
        }
    }

    private static String chooseClip(
        List<String> clips,
        String preferred
    ) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }

        String exact = findIgnoreCase(clips, preferred);
        if (exact != null) {
            return exact;
        }

        for (String prefix : new String[] {"walk", "idle"}) {
            for (String clip : clips) {
                if (clip != null
                    && clip.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    return clip;
                }
            }
        }

        return clips.get(0);
    }

    private static String findIgnoreCase(List<String> clips, String wanted) {
        if (wanted == null) {
            return null;
        }
        for (String clip : clips) {
            if (clip != null && clip.equalsIgnoreCase(wanted)) {
                return clip;
            }
        }
        return null;
    }

    private static float calculateScale(
        Rectangle bounds,
        float cellWidth,
        float rowHeight,
        VisualSpec spec
    ) {
        if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
            return Math.min(cellWidth, rowHeight) / 390f * 1.45f * ZOMBIE_FILL;
        }

        float byWidth = cellWidth * spec.widthInCells / bounds.width;
        float byHeight = rowHeight * spec.heightInRows / bounds.height;
        return Math.min(byWidth, byHeight) * ZOMBIE_FILL;
    }

    private static Map.Entry<String, VisualSpec> entry(
        String type,
        String pamPath
    ) {
        return entry(type, pamPath, "walk", 1.05f, 1.50f);
    }

    private static Map.Entry<String, VisualSpec> entry(
        String type,
        String pamPath,
        float widthInCells,
        float heightInRows
    ) {
        return entry(type, pamPath, "walk", widthInCells, heightInRows);
    }

    private static Map.Entry<String, VisualSpec> entry(
        String type,
        String pamPath,
        String preferredClip,
        float widthInCells,
        float heightInRows
    ) {
        return Map.entry(
            type,
            new VisualSpec(pamPath, preferredClip, widthInCells, heightInRows)
        );
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.toLowerCase(Locale.ROOT).replace("-", "_").trim();
    }

    @Override
    public void dispose() {
        disposed = true;
        loaded.clear();
        loading.clear();
        missing.clear();
        // PamPlayer does not own the TextureBank shared by GameView.
    }

    private record VisualSpec(
        String pamPath,
        String preferredClip,
        float widthInCells,
        float heightInRows
    ) {
    }

    private record ZombieVisual(
        Map<String, ClipRef> clips,
        ClipRef fallback,
        Rectangle bounds
    ) {
        private ClipRef clipFor(Zombie zombie) {
            if (zombie == null) {
                return fallback;
            }

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
                if (exact != null) {
                    return exact;
                }
            }

            // A few PAMs use names such as idle2 or walk_newspaper instead
            // of the model's generic "idle"/"walk" names.  The loader's
            // preferred clip is selected from the actual PAM clip list, so
            // keep that clip for an idle preview instead of silently falling
            // back to a walking cycle.
            if (zombie.getCurrentState() == ZombieState.IDLE
                && fallback != null) {
                return fallback;
            }

            ClipRef walk = clips.get("walk");
            return walk != null ? walk : fallback;
        }
    }
}

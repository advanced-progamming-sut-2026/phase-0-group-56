package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;
import models.entity.Plant;
import models.factory.builder.PlantType;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Responsible only for rendering Plant models.
 *
 * This class:
 * - owns one TextureBank
 * - owns one PamPlayer
 * - resolves PlantType -> PAM path
 * - loads/caches PAM clips
 * - renders a Plant at a position supplied by the view
 *
 * It does NOT:
 * - own a SpriteBatch
 * - begin/end the Batch
 * - update game logic
 * - calculate board coordinates
 */
public final class PlantRenderer implements Disposable {

    private static final String TAG = "PlantRenderer";

    /*
     * Plants are present in more than one PvZ asset package.
     * We deliberately search all known plant roots instead of assuming FULL.
     */
    private static final String[] PLANT_ROOTS = {
        "768/FULL/PLANT",
        "768/INITIAL/PLANT",
        "768/FULL/EMPOWERMINTS/PLANT",
        "768/INITIAL/EMPOWERMINTS/PLANT"
    };

    /*
     * The model enum names and PvZ asset names are not always identical.
     *
     * String -> String is used intentionally instead of PlantType -> String:
     * adding/removing enum constants in the model will not make this class
     * fail to compile.
     */
    private static final Map<String, String> ASSET_ALIASES = Map.ofEntries(
        Map.entry("ROTOBAGA", "ROTORUTABAGA"),
        Map.entry("MEGA_GATLING_PEA", "MEGAGATLING"),
        Map.entry("ICEBERG_LETTUCE", "ICEBURG"),
        Map.entry("KERNEL_PULT", "KERNALPULT"),
        Map.entry("FUM_SHROOM", "FUMESHROOM"),
        Map.entry("PHAT_BEET", "PHATBEETS"),
        Map.entry("CAT_TAIL", "CATTAIL")
    );

    /*
     * PvZ2 plant PAMs use a large source canvas.
     * Keeping a single scale derived from the board cell preserves the
     * relative visual size of different plants much better than stretching
     * each plant according to its visible bounds.
     *
     * If after seeing the result plants are globally a little too large/small,
     * tune CELL_FILL only. No game logic needs to change.
     */
    private static final float PAM_REFERENCE_SIZE = 390f;
    private static final float CELL_FILL = 1.5f;

    private final FileHandle assetsRoot;
    private final FileHandle pamRoot;

    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;

    private final EnumMap<PlantType, PlantVisual> visuals =
        new EnumMap<>(PlantType.class);

    private final EnumSet<PlantType> loading =
        EnumSet.noneOf(PlantType.class);

    private final EnumSet<PlantType> missing =
        EnumSet.noneOf(PlantType.class);

    private boolean disposed = false;


    /**
     * Preferred constructor.
     *
     * @param assetsRoot directory containing:
     *                   RESOURCES.json, ATLASES/, IMAGES/
     */
    public PlantRenderer(FileHandle assetsRoot) {
        validateAssetRoot(assetsRoot);

        this.assetsRoot = assetsRoot;

        /*
         * This mirrors PamPlayer's own lookup convention:
         * newer/extracted layouts may contain pam/,
         * while the supplied PvZ assets use IMAGES/.
         */
        FileHandle explicitPamFolder = assetsRoot.child("pam");

        if (explicitPamFolder.exists()) {
            this.pamRoot = explicitPamFolder;
        } else {
            this.pamRoot = assetsRoot.child("IMAGES");
        }

        this.textureBank = new TextureBank("768", assetsRoot);
        this.pamPlayer = new PamPlayer(textureBank, assetsRoot);
    }


    /**
     * Default renderer for this project.
     *
     * Expected directory:
     * pvz2/assets/pvz/
     */
    public static PlantRenderer createDefault() {
        return new PlantRenderer(Gdx.files.internal("pvz"));
    }


    /**
     * Must be called continuously while this renderer is alive.
     *
     * TextureBank uses this call to finish asynchronous AssetManager loads.
     */
    public void update() {
        if (disposed) {
            return;
        }

        textureBank.update();
    }


    /**
     * Renders one plant at the supplied visual cell center.
     *
     * Batch must already be inside begin()/end().
     *
     * @param batch       Stage/Scene2D batch
     * @param plant       model object
     * @param centerX     visual center of the board cell
     * @param centerY     visual center of the board cell
     * @param cellWidth   board cell width in world coordinates
     * @param cellHeight  board cell height in world coordinates
     */
    public void render(
        Batch batch,
        Plant plant,
        float centerX,
        float centerY,
        float cellWidth,
        float cellHeight
    ) {
        if (disposed || batch == null || plant == null) {
            return;
        }

        PlantType type = plant.getType();

        if (type == null) {
            return;
        }

        /*
         * Do not draw dead entities.
         * This only affects presentation; no model state is changed here.
         */
        if (!plant.isAlive() || plant.getHp() <= 0f) {
            return;
        }

        PlantVisual visual = visuals.get(type);

        if (visual == null) {
            requestVisual(type);
            return;
        }

        /*
         * Use one common source scale rather than resizing every plant to
         * fill its tile. That preserves relative plant sizes.
         */
        float cellSize = Math.min(cellWidth, cellHeight);
        float scale = (cellSize / PAM_REFERENCE_SIZE) * CELL_FILL;

        pamPlayer.draw(
            batch,
            visual.clip,
            plant.getStateTime(),
            centerX,
            centerY,
            scale,
            scale,
            visual.loop
        );
    }


    /**
     * Optional preloading hook.
     *
     * Can later be used for selected plants before gameplay starts.
     */
    public void preload(PlantType type) {
        if (disposed || type == null) {
            return;
        }

        requestVisual(type);
    }


    /**
     * Starts loading the corresponding PAM exactly once.
     */
    private void requestVisual(PlantType type) {
        if (visuals.containsKey(type)
            || loading.contains(type)
            || missing.contains(type)) {

            return;
        }

        String pamPath = resolvePam(type);

        if (pamPath == null) {
            missing.add(type);

            Gdx.app.error(
                TAG,
                "No PAM asset found for PlantType." + type.name()
            );

            return;
        }

        loading.add(type);

        pamPlayer.loadAsync(
            pamPath,
            () -> onPamLoaded(type, pamPath)
        );
    }


    /**
     * Called after PamPlayer has loaded the PAM and all required textures.
     */
    private void onPamLoaded(PlantType type, String pamPath) {
        try {
            if (disposed) {
                return;
            }

            List<String> clips = pamPlayer.clips(pamPath);

            ClipChoice choice = chooseDefaultClip(clips);

            if (choice == null) {
                missing.add(type);

                Gdx.app.error(
                    TAG,
                    "PAM contains no animation clips: " + pamPath
                );

                return;
            }

            ClipRef clip = pamPlayer.getClip(
                pamPath,
                choice.clipName
            );

            if (clip == null) {
                missing.add(type);

                Gdx.app.error(
                    TAG,
                    "Could not obtain clip '" +
                        choice.clipName +
                        "' from " +
                        pamPath
                );

                return;
            }

            PlantVisual visual = new PlantVisual(
                pamPath,
                choice.clipName,
                clip,
                choice.loop
            );

            visuals.put(type, visual);

            Gdx.app.log(
                TAG,
                type.name()
                    + " -> "
                    + pamPath
                    + " ["
                    + choice.clipName
                    + "]"
            );

        } catch (RuntimeException exception) {

            missing.add(type);

            Gdx.app.error(
                TAG,
                "Failed to prepare PAM for PlantType."
                    + type.name()
                    + ": "
                    + pamPath,
                exception
            );

        } finally {
            loading.remove(type);
        }
    }


    /**
     * Resolves a model PlantType into the real PAM file.
     *
     * Example differences handled here:
     *
     * BOWLING_BULB -> BOWLINGBULB
     * SPLIT_PEA    -> SPLITPEA
     * TALL_NUT     -> TALLNUT
     *
     * plus irregular aliases from ASSET_ALIASES.
     */
    private String resolvePam(PlantType type) {
        List<String> candidates =
            assetNameCandidates(type.name());

        for (String candidate : candidates) {

            for (String plantRoot : PLANT_ROOTS) {

                String relativePath =
                    plantRoot
                        + "/"
                        + candidate
                        + "/"
                        + candidate
                        + ".PAM";

                if (pamRoot.child(relativePath).exists()) {
                    return relativePath;
                }
            }
        }

        return null;
    }


    /**
     * Produces all useful filename variants for a PlantType.
     *
     * For example:
     *
     * PRIMAL_POTATO_MINE
     *
     * may produce:
     * PRIMAL_POTATO_MINE
     * PRIMALPOTATOMINE
     * PRIMAL_POTATOMINE
     * PRIMALPOTATO_MINE
     * ...
     */
    private static List<String> assetNameCandidates(
        String modelName
    ) {
        LinkedHashSet<String> result =
            new LinkedHashSet<>();

        String alias = ASSET_ALIASES.get(modelName);

        /*
         * Try explicit irregular alias first.
         */
        if (alias != null) {
            addUnderscoreVariants(alias, result);
        }

        /*
         * Then try systematic transformations of enum name.
         */
        addUnderscoreVariants(modelName, result);

        return new ArrayList<>(result);
    }


    /**
     * Generates every version obtained by keeping/removing underscores.
     *
     * This lets one implementation resolve many enum/asset naming
     * mismatches without a huge hardcoded switch.
     */
    private static void addUnderscoreVariants(
        String name,
        Set<String> output
    ) {
        if (name == null || name.isBlank()) {
            return;
        }

        String[] parts = name.split("_");

        if (parts.length == 1) {
            output.add(name);
            return;
        }

        /*
         * Put the two most common variants first.
         */
        output.add(name);
        output.add(name.replace("_", ""));

        int separatorCount = parts.length - 1;
        int combinationCount = 1 << separatorCount;

        for (int mask = combinationCount - 1;
             mask >= 0;
             mask--) {

            StringBuilder candidate =
                new StringBuilder(parts[0]);

            for (int i = 0; i < separatorCount; i++) {

                boolean keepUnderscore =
                    (mask & (1 << i)) != 0;

                if (keepUnderscore) {
                    candidate.append('_');
                }

                candidate.append(parts[i + 1]);
            }

            output.add(candidate.toString());
        }
    }


    /**
     * Chooses an animation suitable for a plant that currently has no
     * visual state information other than stateTime.
     *
     * Idle clips loop.
     *
     * If a PAM has no idle-like clip at all, its first clip is used
     * non-looping as a conservative fallback.
     */
    private static ClipChoice chooseDefaultClip(
        List<String> clips
    ) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }

        String clip;

        clip = findIgnoreCase(clips, "idle");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findIgnoreCase(clips, "idle1");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findIgnoreCase(clips, "idle_stage1");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findIgnoreCase(clips, "plant_idle");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        /*
         * Some PAMs use more specific names containing "idle".
         */
        for (String current : clips) {

            if (current == null) {
                continue;
            }

            String normalized =
                current.toLowerCase(Locale.ROOT);

            if (normalized.startsWith("idle")) {
                return new ClipChoice(current, true);
            }
        }

        for (String current : clips) {

            if (current == null) {
                continue;
            }

            String normalized =
                current.toLowerCase(Locale.ROOT);

            if (normalized.contains("idle")) {
                return new ClipChoice(current, true);
            }
        }

        /*
         * We deliberately do not loop an arbitrary fallback clip;
         * it could be a planting/death/attack animation.
         */
        return new ClipChoice(clips.get(0), false);
    }


    private static String findIgnoreCase(
        List<String> clips,
        String target
    ) {
        for (String clip : clips) {

            if (clip != null
                && clip.equalsIgnoreCase(target)) {

                return clip;
            }
        }

        return null;
    }


    /**
     * Fail early with a useful message instead of getting obscure
     * ResourceIndex / atlas loading exceptions later.
     */
    private static void validateAssetRoot(
        FileHandle root
    ) {
        if (root == null) {
            throw new IllegalArgumentException(
                "PVZ assets root cannot be null."
            );
        }

        boolean hasResources =
            root.child("RESOURCES.json").exists()
                || root.child("resources.json").exists();

        boolean hasAtlases =
            root.child("ATLASES").exists()
                || root.child("atlases").exists();

        boolean hasImages =
            root.child("IMAGES").exists()
                || root.child("pam").exists();

        if (!hasResources || !hasAtlases || !hasImages) {

            throw new IllegalStateException(
                "Invalid PVZ assets directory: "
                    + root.path()
                    + "\nExpected a directory containing "
                    + "RESOURCES.json, ATLASES/, and IMAGES/.\n"
                    + "For this project the recommended location is: "
                    + "pvz2/assets/pvz/"
            );
        }
    }


    @Override
    public void dispose() {
        if (disposed) {
            return;
        }

        disposed = true;

        /*
         * PamPlayer uses TextureBank but does not own it.
         * TextureBank owns the underlying AssetManager.
         */
        textureBank.dispose();

        visuals.clear();
        loading.clear();
        missing.clear();
    }


    /**
     * Cached resolved visual data for one PlantType.
     */
    private static final class PlantVisual {

        private final String pamPath;
        private final String clipName;
        private final ClipRef clip;
        private final boolean loop;

        private PlantVisual(
            String pamPath,
            String clipName,
            ClipRef clip,
            boolean loop
        ) {
            this.pamPath = pamPath;
            this.clipName = clipName;
            this.clip = clip;
            this.loop = loop;
        }
    }


    /**
     * Internal result of selecting an animation clip.
     */
    private static final class ClipChoice {

        private final String clipName;
        private final boolean loop;

        private ClipChoice(
            String clipName,
            boolean loop
        ) {
            this.clipName = clipName;
            this.loop = loop;
        }
    }
}

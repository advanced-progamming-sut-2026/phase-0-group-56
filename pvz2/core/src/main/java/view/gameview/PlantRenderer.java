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
import java.util.IdentityHashMap;
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
 * - loads/caches idle, ability, Plant Food and damage PAM clips
 * - observes model animation counters and plays one-shot clips safely
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
        "768/INITIAL/EMPOWERMINTS/PLANT",
        /* A few original plants are shipped only in the NPC collection. */
        "768/FULL/NPC"
    };

    /*
     * The model enum names and PvZ asset names are not always identical.
     *
     * String -> String is used intentionally instead of PlantType -> String:
     * adding/removing enum constants in the model will not make this class
     * fail to compile.
     */
    private static final Map<String, String> ASSET_ALIASES = Map.ofEntries(
        Map.entry("TWIN_SUNFLOWER", "SUNFLOWER_TWIN"),
        Map.entry("ROTOBAGA", "ROTORUTABAGA"),
        Map.entry("MEGA_GATLING_PEA", "MEGAGATLING"),
        Map.entry("ICEBERG_LETTUCE", "ICEBURG"),
        Map.entry("KERNEL_PULT", "KERNALPULT"),
        Map.entry("FUM_SHROOM", "FUMESHROOM"),
        Map.entry("PHAT_BEET", "PHATBEETS"),
        Map.entry("CAT_TAIL", "CATTAIL")
    );

    /* The animation sheet explicitly assigns these plants to the two common
     * naming conventions.  Plants not present in either set are idle-only
     * unless they have an explicit entry in one of the maps below. */
    private static final Set<String> TYPE1_PLANTS = Set.of(
        "PEASHOOTER", "FIRE_PEASHOOTER", "GOO_PEASHOOTER",
        "MEGA_GATLING_PEA", "ICEBERG_LETTUCE", "ICE_SHROOM",
        "CABBAGE_PULT", "KERNEL_PULT", "MELON_PULT", "WINTER_MELON",
        "PEPPER_PULT", "CACTUS", "PHAT_BEET", "ELECTRIC_BLUEBERRY"
    );

    private static final Set<String> TYPE2_PLANTS = Set.of(
        "SUNFLOWER", "PRIMAL_SUNFLOWER", "TWIN_SUNFLOWER", "SUN_SHROOM",
        "SNOW_PEA", "ROTOBAGA", "STARFRUIT", "POTATO_MINE", "PRIMAL_POTATO_MINE",
        "TANGLE_KELP", "BONK_CHOY", "WASABI_WHIP", "CHOMPER"
    );

    private static final Map<String, String[]> ACTION_CLIP_OVERRIDES =
        Map.ofEntries(
            Map.entry("GOLD_BLOOM", new String[]{"attack"}),
            Map.entry("ROTOBAGA", new String[]{"attack"}),
            Map.entry("PEA_POD", new String[0]),
            Map.entry("CITRON", new String[]{"attack"}),
            Map.entry("BOWLING_BULB", new String[]{"special"}),
            Map.entry("SEA_SHROOM", new String[]{"attack"}),
            Map.entry("CHERRY_BOMB", new String[]{"attack"}),
            Map.entry("SQUASH", new String[]{"jump_up_right"}),
            Map.entry("GRAPESHOT", new String[]{"attack"}),
            Map.entry("JALAPENO", new String[]{"attack"}),
            Map.entry("DOOM_SHROOM", new String[]{"stage1_explode"}),
            Map.entry("TANGLE_KELP", new String[]{"attack"}),
            Map.entry("ICEBERG_LETTUCE", new String[]{"attack"}),
            Map.entry("ICE_SHROOM", new String[]{"attack"}),
            Map.entry("FUM_SHROOM", new String[]{"special"}),
            Map.entry("CHOMPER", new String[]{"bite"}),
            Map.entry("ENDURIAN", new String[]{"attack_start"}),
            Map.entry("CAULIPOWER", new String[]{"attack"}),
            Map.entry("MAGNET_SHROOM", new String[]{"special"})
        );

    private static final Map<String, String[]> PLANT_FOOD_CLIP_OVERRIDES =
        Map.ofEntries(
            Map.entry("PEA_POD", new String[]{"plantfood_on"}),
            Map.entry("CITRON", new String[]{"plantfood"}),
            Map.entry("BOWLING_BULB", new String[]{"plantfood_idle"}),
            Map.entry("SEA_SHROOM", new String[]{"pf"}),
            Map.entry("SQUASH", new String[]{"plantfood_jump_down_right"}),
            Map.entry("FUM_SHROOM", new String[]{"plantfood"}),
            Map.entry("WALL_NUT", new String[]{"plantfood_on"}),
            Map.entry("GARLIC", new String[]{"plantfood"}),
            Map.entry("SWEET_POTATO", new String[]{"plantfood"}),
            Map.entry("EXPLODE_O_NUT", new String[]{"plantfood_on"}),
            Map.entry("SUN_BEAN", new String[]{"plantfood_on"}),
            Map.entry("TORCHWOOD", new String[]{"plantfood_on"}),
            Map.entry("HYPNO_SHROOM", new String[]{"plantfood_on"}),
            Map.entry("CAULIPOWER", new String[]{"plantfood_start"}),
            Map.entry("MAGNET_SHROOM", new String[]{"plantfood"})
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

    private final FileHandle pamRoot;

    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;

    private final EnumMap<PlantType, PlantVisual> visuals =
        new EnumMap<>(PlantType.class);

    private final EnumSet<PlantType> loading =
        EnumSet.noneOf(PlantType.class);

    private final EnumSet<PlantType> missing =
        EnumSet.noneOf(PlantType.class);

    /* Playback state belongs to the view and is keyed by entity identity. */
    private final IdentityHashMap<Plant, PlantAnimationState> animationStates =
        new IdentityHashMap<>();

    private boolean disposed = false;


    /**
     * Preferred constructor.
     *
     * @param assetsRoot directory containing:
     *                   RESOURCES.json, ATLASES/, IMAGES/
     */
    public PlantRenderer(FileHandle assetsRoot) {
        validateAssetRoot(assetsRoot);

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

        /* Screens that render plants directly (without PlantLayer) still
         * benefit from dropping entities that have died since the last draw. */
        if (!animationStates.isEmpty()) {
            animationStates.keySet().removeIf(
                plant -> plant == null || !plant.isAlive() || plant.getHp() <= 0f
            );
        }
    }


    /**
     * Advances asset loading and removes playback state for plants that have
     * left the model.  The delta is intentionally not used: PAM playback is
     * driven from Plant.stateTime, which already follows the game's time
     * scale and naturally freezes while the game is paused.
     */
    public void update(Iterable<Plant> plants) {
        update();

        if (disposed || plants == null || animationStates.isEmpty()) {
            return;
        }

        IdentityHashMap<Plant, Boolean> present = new IdentityHashMap<>();
        for (Plant plant : plants) {
            if (plant != null) {
                present.put(plant, Boolean.TRUE);
            }
        }
        animationStates.keySet().removeIf(plant -> !present.containsKey(plant));
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

        PlantAnimationState animation = animationStates.get(plant);
        if (animation == null) {
            animation = new PlantAnimationState(plant);
            /* Cursor previews are short-lived objects created by some
             * screens every frame.  Keep state only after a real model event
             * exists, preventing an unbounded identity-map growth. */
            if (animation.hasEvents()) {
                animationStates.put(plant, animation);
            }
        } else {
            animation.observe(plant);
        }

        PlantVisual visual = visuals.get(type);

        if (visual == null) {
            /* Keep observing the entity even while its PAM is asynchronous;
             * an event that happens during loading will play once ready. */
            requestVisual(type);
            return;
        }

        ActiveAnimation active = animation.active(visual, plant.getStateTime());

        /*
         * Use one common source scale rather than resizing every plant to
         * fill its tile. That preserves relative plant sizes.
         */
        float cellSize = Math.min(cellWidth, cellHeight);
        float scale = (cellSize / PAM_REFERENCE_SIZE) * CELL_FILL;

        pamPlayer.draw(
            batch,
            active.clip,
            active.time,
            centerX,
            centerY,
            scale,
            scale,
            active.loop
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

    /** Returns true once the idle PAM for a plant has finished loading. */
    public boolean isReady(PlantType type) {
        return !disposed && type != null && visuals.containsKey(type);
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

            ClipChoice idleChoice = chooseIdleClip(clips);

            if (idleChoice == null) {
                missing.add(type);

                Gdx.app.error(
                    TAG,
                    "PAM contains no animation clips: " + pamPath
                );

                return;
            }

            ClipRef idle = safeClip(pamPath, idleChoice.clipName);
            if (idle == null) {
                missing.add(type);

                Gdx.app.error(
                    TAG,
                    "Could not obtain clip '" +
                        idleChoice.clipName +
                        "' from " +
                        pamPath
                );

                return;
            }

            ClipChoice actionChoice = chooseActionClip(type.name(), clips);
            ClipChoice plantFoodChoice = choosePlantFoodClip(type.name(), clips);
            ClipChoice damageChoice = chooseDamageClip(clips);

            PlantVisual visual = new PlantVisual(
                new ClipTrack(idle),
                idleChoice.loop,
                safeTrack(pamPath, clips, actionChoice),
                safeTrack(pamPath, clips, plantFoodChoice),
                safeTrack(pamPath, clips, damageChoice)
            );

            visuals.put(type, visual);

            Gdx.app.log(
                TAG,
                type.name()
                    + " -> "
                    + pamPath
                    + " [idle="
                    + idleChoice.clipName
                    + ", action="
                    + clipName(actionChoice)
                    + ", plantfood="
                    + clipName(plantFoodChoice)
                    + ", damage="
                    + clipName(damageChoice)
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


    /** Selects the looping resting clip. */
    private static ClipChoice chooseIdleClip(List<String> clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }

        String clip;

        clip = findClip(clips, "idle");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findClip(clips, "idle1");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findClip(clips, "idle_stage1");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        clip = findClip(clips, "plant_idle");

        if (clip != null) {
            return new ClipChoice(clip, true);
        }

        /* Empower Mint assets use intro/loop/outro instead of idle. */
        clip = findClip(clips, "loop");

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

        /* A PAM without an idle still needs a visible fallback.  Do not loop
         * it: the first clip may be a one-shot attack or spawn animation. */
        return new ClipChoice(clips.get(0), false);
    }


    /**
     * Chooses the short one-shot used by a normal plant ability.  The exact
     * names cover the standard PvZ2 PAMs; prefix fallbacks handle plants that
     * expose a stage-specific name (for example special_stage1 or bite).
     */
    private static ClipChoice chooseActionClip(
        String plantName,
        List<String> clips
    ) {
        String[] explicit = ACTION_CLIP_OVERRIDES.get(plantName);
        if (explicit != null) {
            return chooseNamedClip(clips, explicit);
        }
        if (!TYPE1_PLANTS.contains(plantName)
            && !TYPE2_PLANTS.contains(plantName)) {
            return null;
        }
        return chooseGenericActionClip(clips);
    }


    private static ClipChoice chooseNamedClip(
        List<String> clips,
        String[] names
    ) {
        if (names == null || names.length == 0) {
            return null;
        }
        String match = findClip(clips, names);
        return match == null ? null : new ClipChoice(match, false);
    }


    private static ClipChoice chooseGenericActionClip(List<String> clips) {
        String exact = findClip(
            clips,
            "attack",
            "attack_stage1",
            "attack1",
            "bite",
            "special",
            "special_stage1",
            "jump_up"
        );
        if (exact != null) {
            return new ClipChoice(exact, false);
        }

        String[] prefixes = {
            "attack_", "attack", "special_", "special", "bite",
            "jump_", "stage1_explode", "playattack", "down_attack",
            "up_attack", "reload", "sungenerate"
        };
        String prefixed = findByPrefix(clips, prefixes);
        return prefixed == null ? null : new ClipChoice(prefixed, false);
    }


    /** Chooses a plant-food burst, including the common `pf` spelling. */
    private static ClipChoice choosePlantFoodClip(
        String plantName,
        List<String> clips
    ) {
        String[] explicit = PLANT_FOOD_CLIP_OVERRIDES.get(plantName);
        if (explicit != null) {
            return chooseNamedClip(clips, explicit);
        }
        if (TYPE1_PLANTS.contains(plantName)) {
            return chooseNamedClip(clips, new String[]{"plantfood"});
        }
        if (!TYPE2_PLANTS.contains(plantName)) {
            return null;
        }
        return chooseGenericPlantFoodClip(clips);
    }


    private static ClipChoice chooseGenericPlantFoodClip(List<String> clips) {
        String exact = findClip(
            clips,
            "plantfood_on",
            "attack_plantfood",
            "pf_attack",
            "plantfood_start",
            "plantfood",
            "pf",
            "plantfood_stage1",
            "plantfood1"
        );
        if (exact != null) {
            return new ClipChoice(exact, false);
        }

        String prefixed = findByPrefix(
            clips,
            "plantfood_", "plantfood", "pf_", "pf"
        );
        if (prefixed != null) {
            return new ClipChoice(prefixed, false);
        }

        /* Some plants name the burst attack_plantfood or pf_attack. */
        String embedded = findByContains(clips, "plantfood", "pf");
        return embedded == null ? null : new ClipChoice(embedded, false);
    }


    /** Chooses a damage/damaged-state clip for defensive plants. */
    private static ClipChoice chooseDamageClip(List<String> clips) {
        String exact = findClip(clips, "damage", "damage1", "damaged");
        if (exact != null) {
            return new ClipChoice(exact, false);
        }

        String prefixed = findByPrefix(clips, "damage", "idle_damage");
        return prefixed == null ? null : new ClipChoice(prefixed, false);
    }


    private ClipRef safeClip(String pamPath, String clipName) {
        if (clipName == null || clipName.isBlank()) {
            return null;
        }
        try {
            return pamPlayer.getClip(pamPath, clipName);
        } catch (RuntimeException exception) {
            /* Never let a malformed/old asset crash the render loop. */
            Gdx.app.error(
                TAG,
                "Ignoring unavailable clip '" + clipName + "' in " + pamPath,
                exception
            );
            return null;
        }
    }


    /**
     * Builds a one-shot track and includes explicit start/loop/end companions
     * when a PAM provides them.  Missing companions are simply skipped.
     */
    private ClipTrack safeTrack(
        String pamPath,
        List<String> clips,
        ClipChoice choice
    ) {
        if (choice == null) {
            return null;
        }

        LinkedHashSet<String> names = sequenceNames(clips, choice.clipName);
        List<ClipRef> refs = new ArrayList<>();

        for (String name : names) {
            ClipRef ref = safeClip(pamPath, name);
            if (ref != null) {
                refs.add(ref);
            }
        }

        return refs.isEmpty()
            ? null
            : new ClipTrack(refs.toArray(new ClipRef[0]));
    }


    private static LinkedHashSet<String> sequenceNames(
        List<String> clips,
        String selected
    ) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (selected == null || selected.isBlank()) {
            return names;
        }

        /* Tangle Kelp stores its normal action as submerge -> attack ->
         * emerge.  Only enable this special sequence when both companions
         * really exist, so unrelated plants are unaffected. */
        String plainAttack = findClip(clips, "attack");
        if (plainAttack != null
            && plainAttack.equals(selected)
            && findClip(clips, "attack_submerge") != null
            && findClip(clips, "attack_emerge") != null) {
            names.add(findClip(clips, "attack_submerge"));
            names.add(selected);
            names.add(findClip(clips, "attack_emerge"));
            return names;
        }

        /* Citron exposes a charge lead-in before its normal attack. */
        if (plainAttack != null
            && plainAttack.equals(selected)
            && findClip(clips, "charge") != null) {
            names.add(findClip(clips, "charge"));
            names.add(selected);
            return names;
        }

        String selectedLower = selected.toLowerCase(Locale.ROOT);
        if (selectedLower.startsWith("jump_up_")) {
            String direction = selected.substring("jump_up_".length());
            String landing = findClip(clips, "jump_down_" + direction);
            names.add(selected);
            if (landing != null) {
                names.add(landing);
            }
            return names;
        }

        names.add(selected);

        String lower = selectedLower;
        int suffix = lower.lastIndexOf("_start");
        String stem = suffix > 0
            ? selected.substring(0, suffix)
            : selected;

        boolean hasExplicitTrack = suffix > 0
            || findClip(clips, selected + "_loop") != null
            || findClip(clips, selected + "_end") != null;

        /* `plantfood_on`, `plantfood` and `plantfood_off` are a common
         * three-part track.  The selected start clip is retained and only
         * the exact middle/end companions are appended. */
        if (lower.equals("plantfood_on")) {
            String middle = findClip(clips, "plantfood");
            if (middle == null) {
                /* Bowling Bulb exposes numbered bursts but no unnumbered
                 * plantfood clip.  The first burst is its safe base form. */
                middle = findClip(clips, "plantfood1");
            }
            String end = findClip(clips, "plantfood_off");
            if (middle != null) {
                names.add(middle);
            }
            if (end != null) {
                names.add(end);
            }
            return names;
        }

        if (hasExplicitTrack) {
            appendNumberedClips(clips, names, stem + "_loop");
            String end = findClip(clips, stem + "_end");
            if (end != null) {
                names.add(end);
            }
        }

        return names;
    }


    private static void appendNumberedClips(
        List<String> clips,
        Set<String> names,
        String base
    ) {
        String first = findClip(clips, base);
        if (first != null) {
            names.add(first);
        }

        for (int index = 2; index <= 4; index++) {
            String numbered = findClip(clips, base + index);
            if (numbered != null) {
                names.add(numbered);
            }
        }
    }


    private static String clipName(ClipChoice choice) {
        return choice == null ? "none" : choice.clipName;
    }


    private static String findClip(List<String> clips, String... targets) {
        if (clips == null || targets == null) {
            return null;
        }

        for (String target : targets) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(target)) {
                    return clip;
                }
            }
        }

        /* Also accept harmless separators/casing differences in asset names. */
        for (String target : targets) {
            String normalizedTarget = normalizeClipName(target);
            for (String clip : clips) {
                if (clip != null
                    && normalizeClipName(clip).equals(normalizedTarget)) {
                    return clip;
                }
            }
        }

        return null;
    }


    private static String findByPrefix(List<String> clips, String... prefixes) {
        if (clips == null || prefixes == null) {
            return null;
        }

        for (String prefix : prefixes) {
            String normalizedPrefix = normalizeClipName(prefix);
            for (String clip : clips) {
                if (clip != null
                    && normalizeClipName(clip).startsWith(normalizedPrefix)) {
                    return clip;
                }
            }
        }

        return null;
    }


    private static String findByContains(List<String> clips, String... tokens) {
        if (clips == null || tokens == null) {
            return null;
        }

        for (String token : tokens) {
            String normalizedToken = normalizeClipName(token);
            for (String clip : clips) {
                if (clip != null
                    && normalizeClipName(clip).contains(normalizedToken)) {
                    return clip;
                }
            }
        }

        return null;
    }


    private static String normalizeClipName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
        animationStates.clear();
    }


    /**
     * Cached resolved visual data for one PlantType.
     */
    private static final class PlantVisual {

        private final ClipTrack idle;
        private final boolean idleLoop;
        private final ClipTrack action;
        private final ClipTrack plantFood;
        private final ClipTrack damage;

        private PlantVisual(
            ClipTrack idle,
            boolean idleLoop,
            ClipTrack action,
            ClipTrack plantFood,
            ClipTrack damage
        ) {
            this.idle = idle;
            this.idleLoop = idleLoop;
            this.action = action;
            this.plantFood = plantFood;
            this.damage = damage;
        }
    }


    /** Immutable sequence of PAM clips played as one logical event. */
    private static final class ClipTrack {
        private final ClipRef[] clips;
        private final float duration;

        private ClipTrack(ClipRef... clips) {
            this.clips = clips == null ? new ClipRef[0] : clips;

            float total = 0f;
            for (ClipRef clip : this.clips) {
                if (clip != null) {
                    total += durationOf(clip);
                }
            }
            duration = total;
        }

        private ActiveAnimation at(float time, boolean loop) {
            if (clips.length == 0) {
                return new ActiveAnimation(null, 0f, false);
            }

            float localTime = Math.max(0f, time);
            if (loop && duration > 0f) {
                localTime %= duration;
            } else if (duration > 0f) {
                localTime = Math.min(localTime, duration);
            }

            for (ClipRef clip : clips) {
                if (clip == null) {
                    continue;
                }
                float clipDuration = durationOf(clip);
                if (localTime < clipDuration) {
                    return new ActiveAnimation(clip, localTime, false);
                }
                localTime -= clipDuration;
            }

            ClipRef last = clips[clips.length - 1];
            return new ActiveAnimation(
                last,
                last == null ? 0f : durationOf(last),
                false
            );
        }

        private static float durationOf(ClipRef clip) {
            return clip != null && Float.isFinite(clip.duration)
                ? Math.max(0.0001f, clip.duration)
                : 0.0001f;
        }
    }


    /** Per-entity event edge detector and one-shot playback start times. */
    private static final class PlantAnimationState {
        private long actionVersion;
        private long plantFoodVersion;
        private long damageVersion;
        private float actionStart;
        private float plantFoodStart;
        private float damageStart;
        private boolean actionPending;
        private boolean plantFoodPending;
        private boolean damagePending;

        private PlantAnimationState(Plant plant) {
            actionVersion = plant.getAnimationActionVersion();
            plantFoodVersion = plant.getAnimationPlantFoodVersion();
            damageVersion = plant.getAnimationDamageVersion();
            actionStart = plant.getAnimationActionAt();
            plantFoodStart = plant.getAnimationPlantFoodAt();
            damageStart = plant.getAnimationDamageAt();
            actionPending = actionVersion > 0;
            plantFoodPending = plantFoodVersion > 0;
            damagePending = damageVersion > 0;
        }

        private boolean hasEvents() {
            return actionVersion > 0
                || plantFoodVersion > 0
                || damageVersion > 0;
        }

        private void observe(Plant plant) {
            float now = plant.getStateTime();

            long newActionVersion = plant.getAnimationActionVersion();
            if (newActionVersion != actionVersion) {
                actionVersion = newActionVersion;
                actionStart = plant.getAnimationActionAt();
                actionPending = true;
                if (!Float.isFinite(actionStart)) {
                    actionStart = now;
                }
            }

            long newPlantFoodVersion = plant.getAnimationPlantFoodVersion();
            if (newPlantFoodVersion != plantFoodVersion) {
                plantFoodVersion = newPlantFoodVersion;
                plantFoodStart = plant.getAnimationPlantFoodAt();
                plantFoodPending = true;
                if (!Float.isFinite(plantFoodStart)) {
                    plantFoodStart = now;
                }
            }

            long newDamageVersion = plant.getAnimationDamageVersion();
            if (newDamageVersion != damageVersion) {
                damageVersion = newDamageVersion;
                damageStart = plant.getAnimationDamageAt();
                damagePending = true;
                if (!Float.isFinite(damageStart)) {
                    damageStart = now;
                }
            }
        }

        private ActiveAnimation active(PlantVisual visual, float now) {
            preparePending(visual, now);

            /* The event with the newest start time wins; PF can be fired in
             * the same model tick as a normal action and should be visible. */
            if (visual.plantFood != null
                && now >= plantFoodStart
                && now - plantFoodStart < visual.plantFood.duration
                && plantFoodStart >= actionStart
                && plantFoodStart >= damageStart) {
                return visual.plantFood.at(now - plantFoodStart, false);
            }

            if (visual.action != null
                && now >= actionStart
                && now - actionStart < visual.action.duration
                && actionStart >= damageStart
                && actionStart >= plantFoodStart) {
                return visual.action.at(now - actionStart, false);
            }

            if (visual.damage != null
                && now >= damageStart
                && now - damageStart < visual.damage.duration
                && damageStart >= actionStart
                && damageStart >= plantFoodStart) {
                return visual.damage.at(now - damageStart, false);
            }

            return visual.idle.at(Math.max(0f, now), visual.idleLoop);
        }

        /**
         * If an event happened while its PAM was loading, restart only the
         * newest pending event when the visual first becomes available.
         */
        private void preparePending(PlantVisual visual, float now) {
            int selected = 0;
            float selectedAt = Float.NEGATIVE_INFINITY;

            if (damagePending && visual.damage != null) {
                selected = 1;
                selectedAt = damageStart;
            }
            if (actionPending
                && visual.action != null
                && actionStart >= selectedAt) {
                selected = 2;
                selectedAt = actionStart;
            }
            if (plantFoodPending
                && visual.plantFood != null
                && plantFoodStart >= selectedAt) {
                selected = 3;
                selectedAt = plantFoodStart;
            }

            actionPending = false;
            plantFoodPending = false;
            damagePending = false;

            if (selected == 1
                && shouldRestart(now, selectedAt, visual.damage)) {
                damageStart = now;
            } else if (selected == 2
                && shouldRestart(now, selectedAt, visual.action)) {
                actionStart = now;
            } else if (selected == 3
                && shouldRestart(now, selectedAt, visual.plantFood)) {
                plantFoodStart = now;
            }
        }

        private static boolean shouldRestart(
            float now,
            float eventAt,
            ClipTrack track
        ) {
            return track != null
                && (!Float.isFinite(eventAt)
                || now < eventAt
                || now - eventAt >= track.duration);
        }
    }


    private static final class ActiveAnimation {
        private final ClipRef clip;
        private final float time;
        private final boolean loop;

        private ActiveAnimation(ClipRef clip, float time, boolean loop) {
            this.clip = clip;
            this.time = time;
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

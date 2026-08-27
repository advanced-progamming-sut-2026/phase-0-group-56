package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;
import models.gameadventure.Chapters;
import models.gameadventure.levels.Level;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Introductory Crazy Dave sequence shown before selected special stages.
 * Gameplay remains paused by GameView until this actor reports completion.
 */
public final class CrazyDaveIntro extends Group implements Disposable {
    public static final String PAM_PATH =
        "768/INITIAL/CRAZYDAVE/CRAZYDAVE/CRAZYDAVE.PAM";

    private enum Phase { ENTERING, TALKING, LEAVING, DONE }

    private final PamPlayer pamPlayer;
    private final FileHandle pamRoot;
    private final Runnable finished;
    private final Label dialogueLabel;
    private final Label hintLabel;
    private final List<String> dialogue;

    private ClipRef entering;
    private ClipRef idle;
    private ClipRef talking;
    private ClipRef leaving;
    private Rectangle idleBounds;
    private Phase phase = Phase.ENTERING;
    private float phaseTime;
    private int dialogueIndex;
    private boolean loading;
    private boolean disposed;

    public CrazyDaveIntro(
        FileHandle assetsRoot,
        TextureBank textureBank,
        Skin skin,
        Chapters chapter,
        Level level,
        Drawable bubbleBackground,
        Runnable finished
    ) {
        if (skin == null) {
            throw new IllegalArgumentException("skin cannot be null");
        }
        this.finished = finished;
        this.dialogue = createDialogue(chapter, level);

        Table bubble = new Table();
        if (bubbleBackground != null) {
            bubble.setBackground(bubbleBackground);
        }
        bubble.pad(24f, 32f, 20f, 32f);
        dialogueLabel = new Label(dialogue.get(0), skin, "medium_outline");
        dialogueLabel.setWrap(true);
        hintLabel = new Label("PRESS ENTER", skin, "medium_outline");
        bubble.add(dialogueLabel).width(650f).left().row();
        bubble.add(hintLabel).padTop(18f).right();
        bubble.setBounds(475f, 170f, 720f, 230f);
        addActor(bubble);

        if (assetsRoot != null && assetsRoot.exists() && textureBank != null) {
            FileHandle explicit = assetsRoot.child("pam");
            pamRoot = explicit.exists() ? explicit : assetsRoot.child("IMAGES");
            pamPlayer = new PamPlayer(textureBank, assetsRoot);
            requestPam();
        } else {
            pamRoot = null;
            pamPlayer = null;
        }
    }

    private void requestPam() {
        if (pamPlayer == null || pamRoot == null || !pamRoot.child(PAM_PATH).exists()) {
            return;
        }
        loading = true;
        pamPlayer.loadAsync(PAM_PATH, this::onPamLoaded);
    }

    private void onPamLoaded() {
        if (disposed || pamPlayer == null) {
            return;
        }
        try {
            List<String> clips = pamPlayer.clips(PAM_PATH);
            entering = resolveClip(clips, "anim_enter");
            idle = resolveClip(clips, "anim_idle");
            talking = resolveClip(clips, "anim_meduimtalk");
            leaving = resolveClip(clips, "anim_leave");
            if (idle != null) {
                idleBounds = pamPlayer.bounds(PAM_PATH, "anim_idle");
            }
        } catch (RuntimeException exception) {
            Gdx.app.error("CrazyDaveIntro", "Failed to load Crazy Dave PAM", exception);
        } finally {
            loading = false;
        }
    }

    private ClipRef resolveClip(List<String> clips, String wanted) {
        if (clips == null || pamPlayer == null) {
            return null;
        }
        for (String name : clips) {
            if (name != null && name.equalsIgnoreCase(wanted)) {
                try {
                    return pamPlayer.getClip(PAM_PATH, name);
                } catch (RuntimeException exception) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (disposed || phase == Phase.DONE) {
            return;
        }
        phaseTime += Math.max(0f, delta);
        float duration = phaseDuration();
        if (phase == Phase.ENTERING && phaseTime >= duration) {
            phase = Phase.TALKING;
            phaseTime = 0f;
            updateDialogue();
        } else if (phase == Phase.LEAVING && phaseTime >= duration) {
            finish();
        }
    }

    /** Advances/skips the current line. Called by GameView on Enter. */
    public void advance() {
        if (disposed || phase == Phase.DONE) {
            return;
        }
        if (phase == Phase.ENTERING) {
            phase = Phase.TALKING;
            phaseTime = 0f;
            updateDialogue();
        } else if (phase == Phase.TALKING) {
            dialogueIndex++;
            if (dialogueIndex >= dialogue.size()) {
                phase = Phase.LEAVING;
                phaseTime = 0f;
                hintLabel.setText(" ");
            } else {
                updateDialogue();
            }
        } else if (phase == Phase.LEAVING) {
            finish();
        }
    }

    private void updateDialogue() {
        dialogueLabel.setText(dialogue.get(Math.min(dialogueIndex, dialogue.size() - 1)));
    }

    private float phaseDuration() {
        if (phase == Phase.ENTERING && entering != null) {
            return Math.max(0.05f, entering.duration);
        }
        if (phase == Phase.LEAVING && leaving != null) {
            return Math.max(0.05f, leaving.duration);
        }
        return phase == Phase.ENTERING ? 0.8f : 0.7f;
    }

    private void finish() {
        if (phase == Phase.DONE) {
            return;
        }
        phase = Phase.DONE;
        if (finished != null) {
            finished.run();
        }
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        if (!disposed && pamPlayer != null) {
            ClipRef clip = phase == Phase.ENTERING ? entering
                : phase == Phase.LEAVING ? leaving
                : phase == Phase.TALKING && talking != null ? talking : idle;
            if (clip != null) {
                Rectangle bounds = idleBounds;
                float sourceW = bounds == null ? 300f : Math.max(1f, bounds.width);
                float sourceH = bounds == null ? 300f : Math.max(1f, bounds.height);
                float scale = Math.min(300f / sourceW, 360f / sourceH);
                pamPlayer.draw(
                    batch, clip, phaseTime, 300f, 355f,
                    Math.max(0.0001f, scale), Math.max(0.0001f, scale),
                    phase == Phase.TALKING
                );
            }
        }
        super.drawChildren(batch, parentAlpha);
    }

    private static List<String> createDialogue(Chapters chapter, Level level) {
        String chapterName = chapter == null ? "this world" : humanize(chapter.name());
        String type = level == null || level.getLevelType() == null
            ? "special" : humanize(level.getLevelType());
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Crazy Dave here! Welcome to " + chapterName + "!");
        lines.add("This is a " + type + " stage. " + specialRule(level));
        lines.add("Choose your plants wisely, then stop those zombies! Press Enter to continue.");
        return lines;
    }

    private static String specialRule(Level level) {
        String raw = level == null || level.getLevelType() == null
            ? "" : level.getLevelType().toLowerCase(Locale.ROOT);
        if (raw.contains("conveyor")) {
            return "Plants arrive on a conveyor belt, so use each one when it appears.";
        }
        if (raw.contains("save our seeds")) {
            return "Protect the plants already on the lawn while you fight.";
        }
        if (raw.contains("locked plants")) {
            return "Only plants from the permitted category can be selected.";
        }
        if (raw.contains("deadline")) {
            return "Defeat the zombies before the deadline runs out.";
        }
        if (raw.contains("night ops")) {
            return "There is no falling sunlight at night, so plan your economy carefully.";
        }
        if (raw.contains("timed war")) {
            return "The battle is timed, so keep the pressure on.";
        }
        if (raw.contains("plant what you get")) {
            return "You receive plants as you go, so adapt to the hand you are dealt.";
        }
        if (raw.contains("love your plants")) {
            return "Keep every plant alive to complete the challenge.";
        }
        return "Its special rules will change how you play.";
    }

    private static String humanize(String value) {
        String text = value.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    @Override
    public void dispose() {
        disposed = true;
        phase = Phase.DONE;
        entering = null;
        idle = null;
        talking = null;
        leaving = null;
    }
}

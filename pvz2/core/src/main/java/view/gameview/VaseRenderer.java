package view.gameview;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import models.games.minigames.Vase;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Draws Vase Breaker vases and their drop/break animations. */
public final class VaseRenderer {
    private static final float FALLBACK_FILL = 0.88f;

    private final TextureBank textureBank;
    private final PamPlayer pamPlayer;
    private final EnumMap<Vase.Type, Visual> visuals = new EnumMap<>(Vase.Type.class);

    public VaseRenderer(FileHandle assetsRoot, TextureBank textureBank) {
        if (assetsRoot == null || textureBank == null) {
            throw new IllegalArgumentException("VaseRenderer requires PVZ assets and TextureBank.");
        }
        this.textureBank = textureBank;
        this.pamPlayer = new PamPlayer(textureBank, assetsRoot);

        load(
            Vase.Type.RANDOM,
            "768/FULL/VASEBREAKER/VASE_BROWN/VASE_BROWN.PAM",
            "IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150"
        );
        load(
            Vase.Type.ZOMBIE,
            "768/FULL/VASEBREAKER/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM",
            "IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_115X150"
        );
        load(
            Vase.Type.PLANT,
            "768/FULL/VASEBREAKER/VASE_GREEN/VASE_GREEN.PAM",
            "IMAGE_VASEBREAKER_VASE_GREEN_VASE_GREEN_115X150"
        );
    }

    private void load(Vase.Type type, String pamPath, String fallbackImageId) {
        Visual visual = new Visual(pamPath, fallbackImageId);
        visuals.put(type, visual);

        pamPlayer.loadAsync(pamPath, () -> prepare(visual));
    }

    private void prepare(Visual visual) {
        List<String> clips = pamPlayer.clips(visual.pamPath);
        if (clips == null || clips.isEmpty()) {
            return;
        }

        String idle = choose(clips, "idle", "loop", "stand");
        if (idle == null) {
            idle = clips.get(0);
        }
        String drop = choose(clips, "drop", "fall", "intro", "spawn");
        String smash = choose(clips, "break", "smash", "destroy", "death");

        visual.idle = pamPlayer.getClip(visual.pamPath, idle);
        visual.drop = drop == null ? null : pamPlayer.getClip(visual.pamPath, drop);
        visual.smash = smash == null ? null : pamPlayer.getClip(visual.pamPath, smash);

        try {
            visual.idleBounds = pamPlayer.bounds(visual.pamPath, idle);
            if (drop != null) {
                visual.dropBounds = pamPlayer.bounds(visual.pamPath, drop);
            }
            if (smash != null) {
                visual.smashBounds = pamPlayer.bounds(visual.pamPath, smash);
            }
        } catch (RuntimeException ignored) {
            // Static image fallback remains available.
        }
    }

    public void renderVase(
        Batch batch,
        Vase vase,
        float startupTime,
        float centerX,
        float centerY,
        float cellWidth,
        float cellHeight
    ) {
        Visual visual = visuals.get(vase.getType());
        if (visual == null) {
            return;
        }

        if (visual.drop != null && startupTime >= 0f && startupTime < visual.drop.duration) {
            drawClip(
                batch,
                visual.drop,
                visual.dropBounds,
                startupTime,
                false,
                centerX,
                centerY,
                cellWidth,
                cellHeight
            );
            return;
        }

        if (visual.idle != null) {
            float idleTime = Math.max(0f, startupTime);
            drawClip(
                batch,
                visual.idle,
                visual.idleBounds,
                idleTime,
                true,
                centerX,
                centerY,
                cellWidth,
                cellHeight
            );
            return;
        }

        drawFallback(batch, visual.fallbackImageId, centerX, centerY, cellWidth, cellHeight);
    }

    public void renderBreak(
        Batch batch,
        Vase.Type type,
        float time,
        float centerX,
        float centerY,
        float cellWidth,
        float cellHeight
    ) {
        Visual visual = visuals.get(type);
        if (visual == null) {
            return;
        }

        if (visual.smash != null) {
            drawClip(
                batch,
                visual.smash,
                visual.smashBounds,
                time,
                false,
                centerX,
                centerY,
                cellWidth,
                cellHeight
            );
        }
    }

    public float breakDuration(Vase.Type type) {
        Visual visual = visuals.get(type);
        return visual != null && visual.smash != null
            ? Math.max(0.08f, visual.smash.duration)
            : 0.18f;
    }

    private void drawClip(
        Batch batch,
        ClipRef clip,
        Rectangle bounds,
        float time,
        boolean loop,
        float centerX,
        float centerY,
        float cellWidth,
        float cellHeight
    ) {
        if (clip == null) {
            return;
        }

        float scale = 0.78f;
        float originX = centerX;
        float originY = centerY;

        if (bounds != null && bounds.width > 1f && bounds.height > 1f) {
            scale = FALLBACK_FILL * Math.min(
                cellWidth / bounds.width,
                cellHeight / bounds.height
            );
            originX = centerX - (bounds.x + bounds.width * 0.5f) * scale;
            originY = centerY - (bounds.y + bounds.height * 0.5f) * scale;
        }

        pamPlayer.draw(
            batch,
            clip,
            Math.max(0f, time),
            originX,
            originY,
            scale,
            scale,
            loop
        );
    }

    private void drawFallback(
        Batch batch,
        String imageId,
        float centerX,
        float centerY,
        float cellWidth,
        float cellHeight
    ) {
        TextureRegion region = textureBank.region(imageId);
        if (region == null) {
            return;
        }

        float scale = FALLBACK_FILL * Math.min(
            cellWidth / region.getRegionWidth(),
            cellHeight / region.getRegionHeight()
        );
        float width = region.getRegionWidth() * scale;
        float height = region.getRegionHeight() * scale;

        batch.draw(
            region,
            centerX - width * 0.5f,
            centerY - height * 0.5f,
            width,
            height
        );
    }

    private static String choose(List<String> clips, String... needles) {
        for (String needle : needles) {
            for (String clip : clips) {
                if (clip != null && clip.equalsIgnoreCase(needle)) {
                    return clip;
                }
            }
        }

        for (String needle : needles) {
            String normalizedNeedle = needle.toLowerCase(Locale.ROOT);
            for (String clip : clips) {
                if (clip != null && clip.toLowerCase(Locale.ROOT).contains(normalizedNeedle)) {
                    return clip;
                }
            }
        }
        return null;
    }

    private static final class Visual {
        private final String pamPath;
        private final String fallbackImageId;
        private ClipRef idle;
        private ClipRef drop;
        private ClipRef smash;
        private Rectangle idleBounds;
        private Rectangle dropBounds;
        private Rectangle smashBounds;

        private Visual(String pamPath, String fallbackImageId) {
            this.pamPath = pamPath;
            this.fallbackImageId = fallbackImageId;
        }
    }
}

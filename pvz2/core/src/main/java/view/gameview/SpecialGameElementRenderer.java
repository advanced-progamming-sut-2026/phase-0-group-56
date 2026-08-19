package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import models.entity.Plant;
import models.games.BaseGame;
import models.games.specialgames.Deadline;
import models.games.specialgames.SaveOurSeeds;
import pvz.libpvz.textures.TextureBank;

/**
 * Draws world-space visuals that belong to special level rules rather than to
 * normal entities. It deliberately does not own the shared TextureBank.
 */
public final class SpecialGameElementRenderer {
    private static final String TAG = "SpecialGameElementRenderer";

    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private static final String PROTECTED_TILE_ID =
        "IMAGE_BACKGROUNDS_PROTECT_TILE_PROTECT_TILE_112X125";
    private static final String DEADLINE_ID =
        "IMAGE_ZOMBIE_ZOMBIE_FUTURE_VET_FLAG_ZOMBIE_FUTURE_VET_FLAG_29X200";

    private final TextureRegion protectedTileRegion;
    private final TextureRegion deadlineRegion;

    public SpecialGameElementRenderer(TextureBank textureBank) {
        if (textureBank == null) {
            throw new IllegalArgumentException("textureBank cannot be null");
        }

        protectedTileRegion = safeRegion(textureBank, PROTECTED_TILE_ID);
        deadlineRegion = safeRegion(textureBank, DEADLINE_ID);
    }

    public static boolean supports(BaseGame game) {
        return game instanceof SaveOurSeeds || game instanceof Deadline;
    }

    public void render(
        Batch batch,
        BaseGame game,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight
    ) {
        if (batch == null
            || game == null
            || lawnWidth <= 0f
            || lawnHeight <= 0f) {
            return;
        }

        float cellWidth = lawnWidth / COLUMN_COUNT;
        float cellHeight = lawnHeight / ROW_COUNT;

        if (game instanceof SaveOurSeeds saveOurSeeds) {
            renderProtectedTiles(
                batch,
                saveOurSeeds,
                lawnX,
                lawnY,
                cellWidth,
                cellHeight
            );
        }

        if (game instanceof Deadline deadline) {
            renderDeadline(
                batch,
                deadline,
                lawnX,
                lawnY,
                lawnWidth,
                lawnHeight,
                cellWidth
            );
        }
    }

    private void renderProtectedTiles(
        Batch batch,
        SaveOurSeeds game,
        float lawnX,
        float lawnY,
        float cellWidth,
        float cellHeight
    ) {
        if (protectedTileRegion == null) {
            return;
        }

        for (Plant plant : game.getProtectedPlants()) {
            if (plant == null || !plant.isAlive() || plant.getHp() <= 0f) {
                continue;
            }

            int col = plant.getTileIndex();
            int row = plant.getLine();
            if (col < 0 || col >= COLUMN_COUNT || row < 0 || row >= ROW_COUNT) {
                continue;
            }

            float x = lawnX + col * cellWidth;
            float y = lawnY + row * cellHeight;

            batch.draw(
                protectedTileRegion,
                x,
                y,
                cellWidth,
                cellHeight
            );
        }
    }

    private void renderDeadline(
        Batch batch,
        Deadline game,
        float lawnX,
        float lawnY,
        float lawnWidth,
        float lawnHeight,
        float cellWidth
    ) {
        if (deadlineRegion == null) {
            return;
        }

        int deadlineColumn = game.getDeadLine();

        // Loss happens at tileIndex <= deadlineColumn, therefore the visible
        // boundary belongs on the right edge of that column.
        float boundaryX = lawnX + (deadlineColumn + 1f) * cellWidth;
        boundaryX = Math.max(lawnX, Math.min(lawnX + lawnWidth, boundaryX));

        // The supplied texture is intentionally very narrow (29x200). Keep it
        // line-like instead of allowing aspect-ratio scaling to make it huge.
        float lineWidth = Math.max(6f, cellWidth * 0.18f);
        float x = boundaryX - lineWidth * 0.5f;

        batch.draw(
            deadlineRegion,
            x,
            lawnY,
            lineWidth,
            lawnHeight
        );
    }

    private TextureRegion safeRegion(TextureBank textureBank, String resourceId) {
        try {
            TextureRegion region = textureBank.region(resourceId);
            if (region == null) {
                Gdx.app.error(TAG, "Texture resource was not found: " + resourceId);
            }
            return region;
        } catch (RuntimeException exception) {
            Gdx.app.error(TAG, "Failed to load texture resource: " + resourceId, exception);
            return null;
        }
    }
}

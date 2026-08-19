package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;
import models.entity.LawnMower;
import models.gamepanes.Tile;

/**
 * Scene2D bridge between logical mower coordinates and the real TMX lawn.
 */
public final class MowerLayer extends Actor {
    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private final GameController controller;
    private final MowerRenderer renderer;
    private final boolean rowZeroAtTop;

    public MowerLayer(
        GameController controller,
        MowerRenderer renderer
    ) {
        this(controller, renderer, false);
    }

    public MowerLayer(
        GameController controller,
        MowerRenderer renderer,
        boolean rowZeroAtTop
    ) {
        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }

        this.controller = controller;
        this.renderer = renderer;
        this.rowZeroAtTop = rowZeroAtTop;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!isVisible()
            || getWidth() <= 0f
            || getHeight() <= 0f
            || controller.getGame() == null
            || controller.getGame().getField() == null) {
            return;
        }

        float cellWidth = getWidth() / COLUMN_COUNT;
        float rowHeight = getHeight() / ROW_COUNT;
        float logicalBoardWidth = COLUMN_COUNT * Tile.getWidth();

        for (LawnMower mower : controller.getGame().getField().getMoaners()) {
            if (mower == null || mower.isUsed()) {
                continue;
            }

            int modelRow = mower.getLine();
            if (modelRow < 0 || modelRow >= ROW_COUNT) {
                continue;
            }

            int visualRow = rowZeroAtTop
                ? ROW_COUNT - 1 - modelRow
                : modelRow;

            float logicalCenterX =
                mower.getX() + mower.getWidth() * 0.5f;

            float worldCenterX = getX()
                + (logicalCenterX / logicalBoardWidth) * getWidth();

            float worldCenterY = getY()
                + (visualRow + 0.5f) * rowHeight;

            renderer.render(
                batch,
                mower,
                worldCenterX,
                worldCenterY,
                cellWidth,
                rowHeight
            );
        }
    }
}

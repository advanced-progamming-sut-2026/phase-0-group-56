package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;
import models.entity.Plant;

/**
 * Scene2D layer responsible for drawing all plants currently present
 * in the game's field.
 *
 * Its bounds MUST exactly match the playable 9x5 lawn rectangle.
 */
public final class PlantLayer extends Actor {

    private static final int COLUMN_COUNT = 9;
    private static final int ROW_COUNT = 5;

    private final GameController controller;
    private final PlantRenderer renderer;

    /*
     * Model coordinates currently naturally behave as row 0 = bottom
     * because Entity.setLine(...) maps line directly to Y.
     *
     * The flag is kept here so the view can be flipped without ever
     * changing game logic if the visual lawn uses a top-origin convention.
     */
    private final boolean rowZeroAtTop;


    public PlantLayer(
        GameController controller,
        PlantRenderer renderer
    ) {
        this(controller, renderer, false);
    }


    public PlantLayer(
        GameController controller,
        PlantRenderer renderer,
        boolean rowZeroAtTop
    ) {
        if (controller == null) {
            throw new IllegalArgumentException(
                "GameController cannot be null."
            );
        }

        if (renderer == null) {
            throw new IllegalArgumentException(
                "PlantRenderer cannot be null."
            );
        }

        this.controller = controller;
        this.renderer = renderer;
        this.rowZeroAtTop = rowZeroAtTop;

        /*
         * PlantLayer is purely visual.
         * Mouse/touch events must pass through it.
         */
        setTouchable(Touchable.disabled);
    }


    /**
     * Called by Stage.act(delta).
     *
     * TextureBank.update() is deliberately here instead of GameController:
     * asset loading belongs to the view layer.
     */
    @Override
    public void act(float delta) {
        super.act(delta);

        renderer.update(controller.getGame().getPlantsInField());
    }


    /**
     * Stage has already begun its Batch when Actor.draw(...) is called.
     *
     * Therefore PlantRenderer must NOT call begin()/end().
     */
    @Override
    public void draw(
        Batch batch,
        float parentAlpha
    ) {
        if (!isVisible()) {
            return;
        }

        if (getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }

        float cellWidth =
            getWidth() / COLUMN_COUNT;

        float cellHeight =
            getHeight() / ROW_COUNT;

        /*
         * Draw rows from the visually higher/back row toward the
         * visually lower/front row.
         *
         * This produces sensible overlap if plant sprites extend
         * outside their cells.
         */
        for (int visualRow = ROW_COUNT - 1;
             visualRow >= 0;
             visualRow--) {

            drawVisualRow(
                batch,
                visualRow,
                cellWidth,
                cellHeight
            );
        }
    }


    private void drawVisualRow(
        Batch batch,
        int requestedVisualRow,
        float cellWidth,
        float cellHeight
    ) {
        for (Plant plant :
            controller
                .getGame()
                .getPlantsInField()) {

            if (plant == null) {
                continue;
            }

            int column = plant.getTileIndex();
            int modelRow = plant.getLine();

            if (!isValidCell(column, modelRow)) {
                continue;
            }

            int visualRow =
                toVisualRow(modelRow);

            if (visualRow != requestedVisualRow) {
                continue;
            }

            float centerX =
                getX()
                    + (column + 0.5f)
                    * cellWidth;

            float centerY =
                getY()
                    + (visualRow + 0.5f)
                    * cellHeight;

            renderer.render(
                batch,
                plant,
                centerX,
                centerY,
                cellWidth,
                cellHeight
            );
        }
    }


    private int toVisualRow(int modelRow) {
        if (rowZeroAtTop) {
            return ROW_COUNT - 1 - modelRow;
        }

        return modelRow;
    }


    private static boolean isValidCell(
        int column,
        int row
    ) {
        return column >= 0
            && column < COLUMN_COUNT
            && row >= 0
            && row < ROW_COUNT;
    }
}

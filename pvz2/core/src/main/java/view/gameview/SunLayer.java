package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import controllers.menus.gamecontroller.GameController;
import models.entity.Sun;

public final class SunLayer extends Actor {

    private final GameController controller;
    private final SunRenderer renderer;

    private final Rectangle lawnBounds = new Rectangle();

    public SunLayer(
        GameController controller,
        SunRenderer renderer
    ) {
        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }

        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }

        this.controller = controller;
        this.renderer = renderer;

        setTouchable(Touchable.disabled);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!isVisible()) {
            return;
        }

        lawnBounds.set(
            getX(),
            getY(),
            getWidth(),
            getHeight()
        );

        for (Sun sun : controller.getGame().getSuns()) {
            if (sun != null) {
                renderer.render(
                    sun,
                    batch,
                    lawnBounds
                );
            }
        }
    }
}

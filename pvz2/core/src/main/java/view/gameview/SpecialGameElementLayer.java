package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;

/**
 * Thin Scene2D bridge around SpecialGameElementRenderer.
 * WorldEntityRenderer owns the actual layer ordering.
 */
public final class SpecialGameElementLayer extends Actor {
    private final GameController controller;
    private final SpecialGameElementRenderer renderer;

    public SpecialGameElementLayer(
        GameController controller,
        SpecialGameElementRenderer renderer
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

        renderer.render(
            batch,
            controller.getGame(),
            getX(),
            getY(),
            getWidth(),
            getHeight()
        );
    }
}

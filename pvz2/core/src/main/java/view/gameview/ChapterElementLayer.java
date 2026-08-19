package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;

/**
 * Thin Scene2D bridge around ChapterElementRenderer.
 * Layer ordering is controlled by WorldEntityRenderer, not by GameView.
 */
public final class ChapterElementLayer extends Actor {

    private final GameController controller;
    private final ChapterElementRenderer renderer;
    private final ChapterElementRenderer.Pass pass;

    public ChapterElementLayer(
        GameController controller,
        ChapterElementRenderer renderer,
        ChapterElementRenderer.Pass pass
    ) {
        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }
        if (pass == null) {
            throw new IllegalArgumentException("pass cannot be null");
        }

        this.controller = controller;
        this.renderer = renderer;
        this.pass = pass;
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
            controller.getChapter(),
            getX(),
            getY(),
            getWidth(),
            getHeight(),
            pass
        );
    }
}

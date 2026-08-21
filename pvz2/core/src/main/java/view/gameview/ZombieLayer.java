package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;
import models.games.BaseGame;

/**
 * Scene2D bridge between the logical zombie list and the TMX pitch.
 *
 * <p>This actor owns no game state. Its bounds must match the playable 9x5
 * rectangle; {@link ZombieRenderer} performs the actual logical-to-world
 * coordinate conversion.</p>
 */
public final class ZombieLayer extends Actor {
    private final GameController controller;
    private final ZombieRenderer renderer;
    private final Rectangle pitchBounds = new Rectangle();

    private float animationDelta;

    public ZombieLayer(
        GameController controller,
        ZombieRenderer renderer
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
    public void act(float delta) {
        super.act(delta);

        animationDelta = controller.getGame().getState() == BaseGame.GameState.PLAYING
            ? Math.max(0f, delta)
            : 0f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!isVisible() || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }

        pitchBounds.set(getX(), getY(), getWidth(), getHeight());

        renderer.render(
            batch,
            controller.getGame().getZombies(),
            pitchBounds,
            animationDelta
        );
    }
}

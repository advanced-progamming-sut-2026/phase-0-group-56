package view.gameview;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import controllers.menus.gamecontroller.GameController;
import models.entity.RewardDrop;
import models.games.BaseGame;

/** Scene2D layer for coin, diamond and Plant Food collectibles. */
public final class RewardDropLayer extends Actor {
    private final GameController controller;
    private final RewardDropRenderer renderer;
    private final Rectangle lawnBounds = new Rectangle();

    public RewardDropLayer(GameController controller, RewardDropRenderer renderer) {
        if (controller == null || renderer == null) {
            throw new IllegalArgumentException("controller and renderer are required");
        }
        this.controller = controller;
        this.renderer = renderer;
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        float animationDelta = controller.getGame().getState() == BaseGame.GameState.PLAYING
            ? Math.max(0f, delta) : 0f;
        renderer.update(animationDelta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!isVisible()) {
            return;
        }
        lawnBounds.set(getX(), getY(), getWidth(), getHeight());
        for (RewardDrop drop : controller.getGame().getRewardDrops()) {
            renderer.render(drop, batch, lawnBounds);
        }
    }
}

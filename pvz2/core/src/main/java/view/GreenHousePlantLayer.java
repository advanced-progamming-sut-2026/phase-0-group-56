package view;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.files.FileHandle;
import models.entity.Plant;
import models.factory.builder.PlantType;
import view.gameview.PlantRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Scene2D bridge for the real PvZ2 plant PAMs used by the Zen Garden.
 *
 * PlantRenderer already knows how to resolve every PlantType to its supplied
 * 768px PAM and how to select its looping idle clip.  This layer supplies a
 * tiny presentation-only Plant model for each occupied pot, advances its
 * animation clock, and lets PlantRenderer draw it inside the UI batch.
 */
final class GreenHousePlantLayer extends Group implements Disposable {
    private final PlantRenderer renderer;
    private final List<PreviewPlantActor> actors = new ArrayList<>();
    private boolean disposed;

    GreenHousePlantLayer(FileHandle assetsRoot) {
        renderer = assetsRoot == null || !assetsRoot.exists()
            ? null
            : new PlantRenderer(assetsRoot);
        // This layer is visual-only; bed hit targets below the layer must
        // continue receiving input.
        setTouchable(Touchable.disabled);
    }

    boolean isAvailable() {
        return renderer != null;
    }

    void addPlant(PlantType type, float x, float y, float width, float height) {
        if (disposed || renderer == null || type == null) {
            return;
        }

        // A greenhouse slot is rebuilt immediately after planting. Prepare
        // that plant synchronously so the first rendered frame already has an
        // idle clip; retain the async request as a safe fallback for slow or
        // incomplete asset packs.
        if (!renderer.preloadSync(type)) {
            renderer.preload(type);
        }

        PreviewPlantActor actor = new PreviewPlantActor(renderer, type);
        actor.setBounds(x, y, width, height);
        actors.add(actor);
        addActor(actor);
    }

    @Override
    public void act(float delta) {
        if (!disposed && renderer != null) {
            renderer.update();
        }
        super.act(Math.min(Math.max(delta, 0f), 1f / 20f));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!disposed) {
            super.draw(batch, parentAlpha);
        }
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            clearChildren();
            actors.clear();
            if (renderer != null) {
                renderer.dispose();
            }
        }
    }

    private static final class PreviewPlantActor extends Actor {
        private final PlantRenderer renderer;
        private final PreviewPlant plant;

        PreviewPlantActor(PlantRenderer renderer, PlantType type) {
            this.renderer = renderer;
            this.plant = new PreviewPlant(type);
            setTouchable(Touchable.disabled);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            // Greenhouse plants are presentation-only previews. Keep the
            // same idle/alive invariants used by PlantLayer while advancing
            // only the animation clock.
            plant.setAlive(true);
            plant.setHp(1f);
            plant.tick(Math.max(0f, delta));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            renderer.render(
                batch,
                plant,
                getX() + getWidth() / 2f,
                getY() + getHeight() / 2f,
                getWidth(),
                getHeight()
            );
        }
    }

    /** Minimal live model required by PlantRenderer; it never affects game logic. */
    private static final class PreviewPlant extends Plant {
        PreviewPlant(PlantType type) {
            setType(type);
            setHp(1f);
            setAlive(true);
        }

        void tick(float delta) {
            stateTime += delta;
        }
    }

    void clearPlants() {
        actors.clear();
        clearChildren();
    }
}

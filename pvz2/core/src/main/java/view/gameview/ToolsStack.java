package view.gameview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import controllers.datacontroller.SeedPackage;
import controllers.menus.gamecontroller.GameController;
import models.factory.builder.PlantType;
import models.games.BaseGame;
import models.games.specialgames.ConveyorBelt;
import pvz.skin.PvzSkin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-game HUD/tool bar.
 *
 * <p>This class does not advance the simulation and never mutates BaseGame directly.
 * All gameplay actions are delegated to GameController. The only local state here is
 * UI state: selected tool/seed and 1x/2x rendering time scale.</p>
 */
public final class ToolsStack extends Table {

    public enum InteractionMode {
        NORMAL,
        PLANT,
        SHOVEL,
        PLANT_FOOD
    }

    private final GameController controller;
    private final Skin skin;

    private final Label sunLabel;
    private final Label plantFoodLabel;
    private final Label statusLabel;
    private final Label speedLabel;
    private final Label waveLabel;
    private final ProgressBar waveProgress;

    private final Table seedRow;
    private final ImageButton shovelButton;
    private final ImageButton plantFoodButton;
    private final ImageButton pauseButton;
    private final ImageButton speedButton;
    private final TextButton debugButton;
    private final Table debugControls;

    private final Map<PlantType, TextButton> seedButtons = new EnumMap<>(PlantType.class);

    private InteractionMode interactionMode = InteractionMode.NORMAL;
    private PlantType selectedPlant;
    private float timeScale = 1f;
    private String seedSignature = "";

    public ToolsStack(GameController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("controller cannot be null");
        }

        this.controller = controller;
        this.skin = PvzSkin.get();

        setFillParent(true);
        top();
        pad(10f, 12f, 0f, 12f);
        setTouchable(Touchable.childrenOnly);

        Drawable background = safeDrawable("image_ui_quests_panel_edge_to_edge_ten");

        Table hud = new Table();
        if (background != null) {
            hud.setBackground(background);
        }
        hud.pad(8f, 10f, 8f, 10f);

        sunLabel = new Label("SUN: 0", skin, "medium_outline");
        plantFoodLabel = new Label("0", skin, "medium_outline");
        statusLabel = new Label("", skin);
        speedLabel = new Label("1x", skin, "medium_outline");
        waveLabel = new Label("WAVE 0", skin, "medium_outline");

        statusLabel.setAlignment(Align.center);
        statusLabel.setColor(Color.WHITE);

        seedRow = new Table();

        shovelButton = new ImageButton(skin, "ingame_shovel");
        plantFoodButton = new ImageButton(skin, "plantfood");
        pauseButton = new ImageButton(skin, "ingame_pause");
        speedButton = new ImageButton(skin, "ingame_2x");
        debugButton = new TextButton("DEBUG", skin, "brown");
        debugControls = new Table();
        debugControls.setVisible(false);

        waveProgress = new ProgressBar(
            0f,
            1f,
            0.001f,
            false,
            skin,
            "ingame_progress"
        );
        waveProgress.setAnimateDuration(0.15f);

        hookToolButtons();
        hookDebugControls();

        Table plantFoodHolder = new Table();
        plantFoodHolder.add(plantFoodButton).size(55f);
        plantFoodHolder.add(plantFoodLabel).padLeft(2f);

        Table speedHolder = new Table();
        speedHolder.add(speedButton).size(58f);
        speedHolder.add(speedLabel).padLeft(2f);

        hud.add(sunLabel).width(120f).left().padRight(8f);
        hud.add(seedRow).expandX().left();
        hud.add(shovelButton).size(62f).padLeft(6f);
        hud.add(plantFoodHolder).padLeft(6f);
        hud.add(pauseButton).size(58f).padLeft(6f);
        hud.add(speedHolder).padLeft(6f);
        hud.add(debugButton).width(88f).height(52f).padLeft(6f);

        add(hud).expandX().fillX().top().row();

        Table progressRow = new Table();
        progressRow.add(waveLabel).width(120f).left();
        progressRow.add(waveProgress).width(420f).height(24f).center();
        progressRow.add(statusLabel).expandX().fillX().padLeft(16f);

        add(progressRow)
            .expandX()
            .fillX()
            .padTop(4f);
        add(debugControls).right().padTop(4f).row();

        refresh();
    }

    private void hookToolButtons() {
        shovelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (controller.getGame().getState() != BaseGame.GameState.PLAYING) {
                    return;
                }

                selectedPlant = null;
                interactionMode = interactionMode == InteractionMode.SHOVEL
                    ? InteractionMode.NORMAL
                    : InteractionMode.SHOVEL;

                setStatus(interactionMode == InteractionMode.SHOVEL
                    ? "Shovel selected"
                    : "Shovel cancelled");
            }
        });

        plantFoodButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (controller.getGame().getState() != BaseGame.GameState.PLAYING
                    || controller.getGame().getPlantFoodsCount() <= 0) {
                    return;
                }

                selectedPlant = null;
                interactionMode = interactionMode == InteractionMode.PLANT_FOOD
                    ? InteractionMode.NORMAL
                    : InteractionMode.PLANT_FOOD;

                setStatus(interactionMode == InteractionMode.PLANT_FOOD
                    ? "Plant Food selected"
                    : "Plant Food cancelled");
            }
        });

        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.togglePause();
                refresh();
            }
        });

        speedButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                timeScale = timeScale == 1f ? 2f : 1f;
                speedLabel.setText(timeScale == 1f ? "1x" : "2x");
            }
        });
    }

    private void hookDebugControls() {
        TextButton addSunButton = new TextButton("+50 SUN", skin, "green_small");
        TextButton addFoodButton = new TextButton("+1 FOOD", skin, "green_small");
        addSunButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus(controller.debugAddResources(50, 0));
                refresh();
            }
        });
        addFoodButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                setStatus(controller.debugAddResources(0, 1));
                refresh();
            }
        });
        debugControls.add(addSunButton).size(92f, 42f).padRight(4f);
        debugControls.add(addFoodButton).size(92f, 42f);
        debugButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                boolean visible = !debugControls.isVisible();
                debugControls.setVisible(visible);
                setStatus(visible ? "Debug controls opened" : "Debug controls closed");
            }
        });
    }

    public void refresh() {
        BaseGame game = controller.getGame();

        sunLabel.setText("SUN: " + game.getSunCount());
        plantFoodLabel.setText(String.valueOf(game.getPlantFoodsCount()));

        boolean playing = game.getState() == BaseGame.GameState.PLAYING;
        shovelButton.setDisabled(!playing);
        plantFoodButton.setDisabled(!playing || game.getPlantFoodsCount() <= 0);
        debugButton.setDisabled(!playing);
        if (!playing) {
            debugControls.setVisible(false);
        }

        refreshSeedRow();
        refreshSeedButtons();
        refreshWaveProgress();

        if (!playing && game.getState() == BaseGame.GameState.PAUSE) {
            setStatus("PAUSED");
        }
    }

    private void refreshSeedRow() {
        String newSignature = createSeedSignature();
        if (newSignature.equals(seedSignature)) {
            return;
        }

        seedSignature = newSignature;
        seedRow.clearChildren();
        seedButtons.clear();

        BaseGame game = controller.getGame();

        if (game instanceof ConveyorBelt conveyorBelt) {
            List<PlantType> belt = new ArrayList<>(conveyorBelt.getBelt());
            for (PlantType type : belt) {
                addSeedButton(type, null, true);
            }
            return;
        }

        for (Map.Entry<PlantType, SeedPackage> entry : game.getAvailable_plants().entrySet()) {
            addSeedButton(entry.getKey(), entry.getValue(), false);
        }
    }

    private void addSeedButton(PlantType type, SeedPackage packet, boolean conveyor) {
        if (type == null) {
            return;
        }

        TextButton button = new TextButton(shortPlantName(type), skin, "brown");
        button.getLabel().setWrap(true);
        button.getLabel().setAlignment(Align.center);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (controller.getGame().getState() != BaseGame.GameState.PLAYING) {
                    return;
                }

                if (!conveyor && packet != null) {
                    if (!packet.isAvailable()) {
                        setStatus(shortPlantName(type) + " is recharging");
                        return;
                    }

                    if (controller.getGame().getSunCount() < packet.getCost()) {
                        setStatus("Not enough sun");
                        return;
                    }
                }

                if (interactionMode == InteractionMode.PLANT && selectedPlant == type) {
                    selectedPlant = null;
                    interactionMode = InteractionMode.NORMAL;
                    setStatus("Plant selection cancelled");
                } else {
                    selectedPlant = type;
                    interactionMode = InteractionMode.PLANT;
                    setStatus(shortPlantName(type) + " selected");
                }

                refreshSeedButtons();
            }
        });

        seedButtons.put(type, button);
        seedRow.add(button)
            .width(104f)
            .height(64f)
            .padRight(4f);
    }

    private void refreshSeedButtons() {
        BaseGame game = controller.getGame();
        boolean playing = game.getState() == BaseGame.GameState.PLAYING;
        boolean conveyor = game instanceof ConveyorBelt;

        for (Map.Entry<PlantType, TextButton> entry : seedButtons.entrySet()) {
            PlantType type = entry.getKey();
            TextButton button = entry.getValue();

            boolean enabled = playing;
            String text = shortPlantName(type);

            if (!conveyor) {
                SeedPackage packet = game.getAvailable_plants().get(type);
                if (packet == null) {
                    enabled = false;
                } else {
                    if (packet.isAvailable()) {
                        text += "\n" + (int) packet.getCost();
                        enabled = enabled && game.getSunCount() >= packet.getCost();
                    } else {
                        text += "\n" + Math.max(0, (int) Math.ceil(packet.getRecharge())) + "s";
                        enabled = false;
                    }
                }
            }

            button.setText(text);
            button.setDisabled(!enabled);
            button.setColor(
                interactionMode == InteractionMode.PLANT && selectedPlant == type
                    ? new Color(0.72f, 1f, 0.72f, 1f)
                    : Color.WHITE
            );
        }
    }

    private String createSeedSignature() {
        BaseGame game = controller.getGame();

        if (game instanceof ConveyorBelt conveyorBelt) {
            StringBuilder signature = new StringBuilder("BELT:");
            for (PlantType type : conveyorBelt.getBelt()) {
                signature.append(type == null ? "null" : type.name()).append('|');
            }
            return signature.toString();
        }

        StringBuilder signature = new StringBuilder("PACKETS:");
        for (PlantType type : game.getAvailable_plants().keySet()) {
            signature.append(type.name()).append('|');
        }
        return signature.toString();
    }

    private void refreshWaveProgress() {
        BaseGame game = controller.getGame();
        int totalWaves = Math.max(1, game.getWaves() == null ? 0 : game.getWaves().size());
        int current = Math.max(0, game.getWaveID());

        float progress = Math.min(1f, (float) current / totalWaves);
        waveProgress.setValue(progress);
        waveLabel.setText("WAVE " + Math.min(current, totalWaves) + "/" + totalWaves);
    }

    public InteractionMode getInteractionMode() {
        return interactionMode;
    }

    public PlantType getSelectedPlant() {
        return selectedPlant;
    }

    public float getTimeScale() {
        return timeScale;
    }

    public void finishWorldAction() {
        selectedPlant = null;
        interactionMode = InteractionMode.NORMAL;
        refreshSeedButtons();
    }

    public void setStatus(String status) {
        statusLabel.setText(status == null ? "" : status);
    }

    private Drawable safeDrawable(String name) {
        try {
            return skin.getDrawable(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String shortPlantName(PlantType type) {
        if (type == null) {
            return "";
        }

        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1));
        }

        return result.toString();
    }
}

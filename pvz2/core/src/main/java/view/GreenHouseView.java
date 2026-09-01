package view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import controllers.datacontroller.Data;
import controllers.menus.secondarymenus.GreenHouseController;
import models.App;
import models.Pot;
import models.User;
import models.factory.builder.PlantType;
import pvz.skin.PvzSkin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The interactive Zen Garden screen, laid out on the supplied 4 x 3 art. */
public class GreenHouseView extends View {
    private static final int COLUMNS = 4;
    private static final int ROWS = 3;

    /* Centers of the four wooden beds in zen_garden.png after its intentional
     * side matte is cropped by Scaling.fill at the 1280 x 720 viewport. */
    private static final float[] BED_X = {402f, 565f, 728f, 890f};
    private static final float[] BED_Y = {431f, 286f, 141f};
    private static final float SLOT_WIDTH = 156f;
    private static final float SLOT_HEIGHT = 132f;

    private Stack layers;
    private Group gardenBoard;
    private GreenHousePlantLayer plantLayer;
    private FileHandle pvzAssetsRoot;
    private Label coinsLabel;
    private Label gemsLabel;

    public GreenHouseView() {
        menu = new GreenHouseController();
    }

    @Override
    protected String getScreenTitle() {
        return "Greenhouse";
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    /** Every hit target is aligned with a bed painted into the background. */
    @Override
    public void show() {
        skin = PvzSkin.get();
        stage = new com.badlogic.gdx.scenes.scene2d.Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        layers = new Stack();
        layers.setFillParent(true);
        Image background = MenuVisualAssets.image("zen_background");
        if (background != null) {
            background.setScaling(Scaling.fill);
            background.setTouchable(Touchable.disabled);
            layers.add(background);
        } else {
            Image fallback = new Image(solidDrawable(
                new com.badlogic.gdx.graphics.Color(0.025f, 0.09f, 0.055f, 1f)));
            fallback.setTouchable(Touchable.disabled);
            layers.add(fallback);
        }

        pvzAssetsRoot = findPvzAssetsRoot();
        plantLayer = new GreenHousePlantLayer(pvzAssetsRoot);
        rebuildGardenBoard();
        layers.add(gardenBoard);
        layers.add(buildHud());
        stage.addActor(layers);
    }

    private Table buildHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top();
        hud.pad(12f, 18f, 12f, 18f);

        TextButton back = brownButton("BACK", () -> App.setScreen(new HomeView()));
        hud.add(back).width(126f).height(50f).left();

        Table title = new Table();
        Image logo = MenuVisualAssets.image("logo");
        if (logo != null) {
            logo.setScaling(Scaling.fit);
            title.add(logo).width(190f).height(32f).center().row();
        }
        Label heading = titleLabel("ZEN GARDEN");
        heading.setAlignment(Align.center);
        title.add(heading).center();
        hud.add(title).expandX().center();

        Table resources = new Table();
        resources.setBackground(MenuVisualAssets.drawable("counter_bg"));
        resources.pad(5f, 8f, 5f, 8f);
        resources.add(resourceChip("coin_small", "COINS", coinsLabel = resourceValue(0))).padRight(7f);
        resources.add(resourceChip("gem_small", "GEMS", gemsLabel = resourceValue(0)));
        hud.add(resources).width(230f).right().row();

        Table help = new Table();
        help.setBackground(solidDrawable(new com.badlogic.gdx.graphics.Color(0.015f, 0.07f, 0.045f, 0.76f)));
        help.pad(7f, 14f, 7f, 14f);
        Label helpText = secondaryLabel("TAP A BED  •  PLANT  •  WATER  •  COLLECT");
        helpText.setAlignment(Align.center);
        help.add(helpText).center();
        hud.add(help).colspan(3).width(510f).padTop(8f).center().row();

        Table shopBar = new Table();
        shopBar.add(purpleButton("OPEN SHOP", () -> App.setScreen(new ShopView())))
            .width(220f).height(52f);
        hud.add(shopBar).colspan(3).expandY().bottom().padBottom(9f).center();

        refreshGardenResources();
        return hud;
    }

    private Label resourceValue(int amount) {
        Label value = mediumTitle(String.valueOf(amount));
        value.setAlignment(Align.center);
        return value;
    }

    private Table resourceChip(String iconKey, String caption, Label amount) {
        Table chip = new Table();
        Image icon = MenuVisualAssets.image(iconKey);
        if (icon != null) {
            icon.setScaling(Scaling.fit);
            chip.add(icon).size(25f).padRight(3f);
        }
        Table values = new Table();
        Label label = secondaryLabel(caption);
        label.setAlignment(Align.center);
        values.add(label).center().row();
        values.add(amount).center();
        chip.add(values).minWidth(58f).center();
        return chip;
    }

    private void rebuildGardenBoard() {
        if (gardenBoard != null) gardenBoard.remove();
        if (plantLayer != null) plantLayer.clearChildren();

        gardenBoard = new Group();
        gardenBoard.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        User user = Data.getCurrentUser();
        if (user == null) return;

        for (int row = 1; row <= ROWS; row++) {
            for (int column = 1; column <= COLUMNS; column++) {
                Pot pot = user.getGreenHouse().getPotByPosition(column, row);
                gardenBoard.addActor(createBed(pot, column, row));
            }
        }
        if (plantLayer != null && plantLayer.isAvailable()) gardenBoard.addActor(plantLayer);
    }

    private Stack createBed(Pot pot, int column, int row) {
        Stack bed = new Stack();
        bed.setBounds(BED_X[column - 1] - SLOT_WIDTH / 2f, BED_Y[row - 1] - SLOT_HEIGHT / 2f,
            SLOT_WIDTH, SLOT_HEIGHT);

        boolean locked = pot == null || !pot.isUnlocked();
        PlantType type = pot == null ? null : pot.getSeedlingType();
        boolean ready = type != null && pot.getRemainingHours() <= 0;
        boolean growing = type != null && !ready;

        if (ready) {
            Image glow = MenuVisualAssets.image("zen_glow");
            if (glow != null) {
                glow.setScaling(Scaling.fit);
                glow.setColor(1f, 1f, 1f, .42f);
                bed.add(glow);
            }
        }

        if (!locked) {
            Image potImage = MenuVisualAssets.image(ready ? "zen_slot_ready" : "zen_slot");
            if (potImage == null) potImage = MenuVisualAssets.image("pot");
            if (potImage != null) {
                potImage.setScaling(Scaling.fit);
                bed.add(potImage);
            }
        } else {
            Image lock = MenuVisualAssets.image("zen_lock");
            if (lock != null) {
                lock.setScaling(Scaling.fit);
                bed.add(lock);
            }
        }

        if (growing || ready) {
            Image water = MenuVisualAssets.image(ready ? "zen_water" : "zen_drop");
            if (water != null) {
                water.setScaling(Scaling.fit);
                water.setColor(1f, 1f, 1f, ready ? .95f : .72f);
                water.setPosition(SLOT_WIDTH - 35f, SLOT_HEIGHT - 51f);
                water.setSize(25f, 40f);
                if (growing) water.addAction(Actions.forever(Actions.sequence(
                    Actions.moveBy(0f, -4f, .45f, Interpolation.sine),
                    Actions.moveBy(0f, 4f, .45f, Interpolation.sine))));
                Group waterLayer = new Group();
                waterLayer.setSize(SLOT_WIDTH, SLOT_HEIGHT);
                waterLayer.addActor(water);
                bed.add(waterLayer);
            }
        }

        Label state = secondaryLabel(bedText(pot));
        state.setAlignment(Align.center);
        state.setWrap(true);
        state.setPosition(8f, 2f);
        state.setSize(SLOT_WIDTH - 16f, 34f);
        state.setTouchable(Touchable.disabled);
        Group stateLayer = new Group();
        stateLayer.setSize(SLOT_WIDTH, SLOT_HEIGHT);
        stateLayer.addActor(state);
        bed.add(stateLayer);

        if (pot != null && type != null && plantLayer != null && plantLayer.isAvailable()) {
            plantLayer.addPlant(type, BED_X[column - 1] - 59f, BED_Y[row - 1] - 50f, 118f, 112f);
        }

        final int x = column;
        final int y = row;
        TextButton hit = greenButton("", () -> handlePot(x, y));
        hit.setColor(1f, 1f, 1f, 0f);
        bed.add(hit);
        return bed;
    }

    private String bedText(Pot pot) {
        if (pot == null || !pot.isUnlocked()) return "LOCKED";
        if (pot.getSeedlingType() == null) return "EMPTY  •  TAP TO PLANT";
        if (pot.getRemainingHours() <= 0) return "READY  •  TAP TO COLLECT";
        return pot.getSeedlingType().name() + "  •  " + pot.getRemainingHours() + "h";
    }

    private void handlePot(int x, int y) {
        User user = Data.getCurrentUser();
        if (user == null) { showMessage("Please log in first."); return; }
        Pot pot = user.getGreenHouse().getPotByPosition(x, y);
        if (pot == null) { showMessage("Error: invalid bed."); return; }
        if (!pot.isUnlocked()) { showMessage("This bed is locked. Buy more beds from the shop."); return; }

        GreenHouseController controller = (GreenHouseController) menu;
        if (pot.getSeedlingType() == null) {
            showConfirmation("Plant seedling", "Plant a random seedling in this bed?", () -> {
                showMessage(controller.plant(x, y)); reloadGreenhouse();
            });
        } else if (pot.getRemainingHours() <= 0) {
            showConfirmation("Collect reward", "Collect this fully grown plant?", () -> {
                showMessage(controller.collect(x, y)); reloadGreenhouse();
            });
        } else {
            int cost = pot.getRemainingHours();
            showConfirmation("Water instantly", "Spend " + cost + " gem(s) to finish growth?", () -> {
                showMessage(controller.forceGrow(x, y)); reloadGreenhouse();
            });
        }
    }

    private void reloadGreenhouse() {
        if (plantLayer != null) plantLayer.dispose();
        plantLayer = new GreenHousePlantLayer(pvzAssetsRoot);
        rebuildGardenBoard();
        if (layers != null) layers.addActorAt(1, gardenBoard);
        refreshGardenResources();
    }

    private void refreshGardenResources() {
        User user = Data.getCurrentUser();
        if (user == null) return;
        if (coinsLabel != null) coinsLabel.setText(String.valueOf(user.getCoins()));
        if (gemsLabel != null) gemsLabel.setText(String.valueOf(user.getDiamonds()));
    }

    @Override public void hide() {
        if (plantLayer != null) { plantLayer.dispose(); plantLayer = null; }
        super.hide();
    }

    @Override public void dispose() {
        if (plantLayer != null) { plantLayer.dispose(); plantLayer = null; }
        super.dispose();
    }

    private FileHandle findPvzAssetsRoot() {
        String configured = System.getProperty("pvz.assets");
        List<FileHandle> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) candidates.add(new FileHandle(new File(configured)));
        for (String path : new String[]{"Assets", "../Assets", "../../Assets", "pvz-assets", "../pvz-assets", "../../pvz-assets"}) {
            candidates.add(new FileHandle(new File(path)));
        }
        candidates.add(Gdx.files.internal("pvz-assets"));
        for (FileHandle candidate : candidates) {
            FileHandle root = resolveAssetRoot(candidate);
            if (root != null) return root;
        }
        return null;
    }

    private FileHandle resolveAssetRoot(FileHandle root) {
        if (isAssetRoot(root)) return root;
        if (root == null || !root.exists()) return null;
        for (String child : new String[]{"Base Assets", "base assets", "BaseAssets", "pvz-assets", "assets"}) {
            if (isAssetRoot(root.child(child))) return root.child(child);
        }
        try {
            for (FileHandle child : root.list()) if (child.isDirectory() && isAssetRoot(child)) return child;
        } catch (RuntimeException ignored) { }
        return null;
    }

    private boolean isAssetRoot(FileHandle root) {
        if (root == null || !root.exists()) return false;
        return (root.child("RESOURCES.json").exists() || root.child("resources.json").exists())
            && (root.child("ATLASES").exists() || root.child("atlases").exists());
    }
}

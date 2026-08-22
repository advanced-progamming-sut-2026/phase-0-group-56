package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import controllers.datacontroller.PlantData;
import controllers.menus.secondarymenus.Collection;
import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import view.components.CollectionAssetCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Final graphical Almanac/Collection screen. */
public class CollectionView extends View {
    private static final float LIST_WIDTH = 720f;
    private static final float DETAIL_WIDTH = 390f;
    private static final float LIST_HEIGHT = 470f;

    private boolean plantTab = true;
    private String statusFilter = "ALL";
    private String familyFilter = "ANY";
    private PlantType selectedPlant;
    private ZombieRegistry.ZombieType selectedZombie;
    private CollectionAssetCatalog assetCatalog;

    public CollectionView() {
        menu = new Collection();
    }

    @Override
    public void show() {
        assetCatalog = new CollectionAssetCatalog();
        super.show();
    }

    @Override
    public void render(float delta) {
        if (assetCatalog != null) {
            assetCatalog.update();
        }
        super.render(delta);
    }

    @Override
    public void hide() {
        closeAssets();
        super.hide();
    }

    @Override
    public void dispose() {
        closeAssets();
        super.dispose();
    }

    @Override
    protected String getScreenTitle() {
        return "Collection";
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        Collection controller = (Collection) menu;
        buildTabs(table);
        buildSummary(table, controller);

        if (plantTab) {
            buildPlants(table, controller);
        } else {
            buildZombies(table, controller);
        }
    }

    private void buildTabs(Table table) {
        Table tabs = pvzInnerPanel();
        TextButton plants = plantTab
            ? greenButton("PLANTS", null)
            : brownButton("PLANTS", () -> switchTab(true));
        TextButton zombies = plantTab
            ? brownButton("ZOMBIES", () -> switchTab(false))
            : purpleButton("ZOMBIES", null);

        tabs.add(plants).width(220f).height(52f).padRight(12f);
        tabs.add(zombies).width(220f).height(52f);
        table.add(tabs).width(520f).padBottom(10f).row();
    }

    private void buildSummary(Table table, Collection controller) {
        int unlockedPlants = 0;
        for (PlantType type : controller.getAllPlants()) {
            if (controller.isPlantUnlocked(type)) {
                unlockedPlants++;
            }
        }

        int unlockedZombies = 0;
        for (ZombieRegistry.ZombieType type : controller.getAllZombies()) {
            if (controller.isZombieUnlocked(type)) {
                unlockedZombies++;
            }
        }

        String summary = "PLANTS  " + unlockedPlants + " / " + controller.getAllPlants().size()
            + "       ZOMBIES  " + unlockedZombies + " / " + controller.getAllZombies().size();
        Label label = new Label(summary, skin, "medium_outline");
        label.setAlignment(Align.center);
        table.add(label).padBottom(10f).row();

        if (assetCatalog == null || !assetCatalog.isAvailable()) {
            Label fallback = secondaryLabel(
                "Official asset pack not found; Collection is using its safe text fallback."
            );
            fallback.setAlignment(Align.center);
            table.add(fallback).padBottom(10f).row();
        }
    }

    private void buildPlants(Table table, Collection controller) {
        buildPlantFilters(table, controller);

        List<PlantType> visiblePlants = new ArrayList<>();
        for (PlantType type : controller.getAllPlants()) {
            if (plantMatchesFilters(controller, type)) {
                visiblePlants.add(type);
            }
        }

        Table body = new Table();
        ScrollPane list = scrollable(buildPlantGrid(controller, visiblePlants));
        body.add(list).width(LIST_WIDTH).height(LIST_HEIGHT).top().padRight(16f);
        body.add(buildPlantDetail(controller)).width(DETAIL_WIDTH).top();
        table.add(body).growX();
    }

    private void buildPlantFilters(Table table, Collection controller) {
        Table filters = pvzInnerPanel();
        SelectBox<String> statusBox = new SelectBox<>(skin);
        statusBox.setItems("ALL", "UNLOCKED", "LOCKED", "UPGRADEABLE");
        statusBox.setSelected(statusFilter);
        statusBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                statusFilter = statusBox.getSelected();
                rebuild();
            }
        });

        SelectBox<String> familyBox = new SelectBox<>(skin);
        Array<String> families = new Array<>();
        for (String family : controller.getPlantFamilies()) {
            families.add(family);
        }
        familyBox.setItems(families);
        familyBox.setSelected(familyFilter);
        familyBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                familyFilter = familyBox.getSelected();
                rebuild();
            }
        });

        filters.add(new Label("STATUS", skin, "medium_outline")).padRight(10f);
        filters.add(statusBox).width(220f).height(42f).padRight(28f);
        filters.add(new Label("FAMILY", skin, "medium_outline")).padRight(10f);
        filters.add(familyBox).width(240f).height(42f);
        table.add(filters).width(850f).padBottom(12f).row();
    }

    private Table buildPlantGrid(Collection controller, List<PlantType> plants) {
        Table panel = pvzPanel();
        panel.top();
        Label title = mediumTitle("PLANT COLLECTION");
        title.setAlignment(Align.center);
        panel.add(title).colspan(3).padBottom(12f).row();

        if (plants.isEmpty()) {
            Label empty = wrappedLabel("No plants match the selected filters.", 590f);
            empty.setAlignment(Align.center);
            panel.add(empty).colspan(3).width(590f).pad(30f);
            return panel;
        }

        int column = 0;
        for (PlantType type : plants) {
            panel.add(buildPlantCard(controller, type)).width(205f).height(158f).pad(5f);
            column++;
            if (column == 3) {
                panel.row();
                column = 0;
            }
        }
        return panel;
    }

    private Actor buildPlantCard(Collection controller, PlantType type) {
        boolean unlocked = controller.isPlantUnlocked(type);
        boolean selected = type == selectedPlant;
        TextButton background = selected
            ? purpleButton("", null)
            : unlocked ? greenButton("", null) : brownButton("", null);
        background.setTouchable(Touchable.disabled);

        Table contentTable = new Table();
        contentTable.setTouchable(Touchable.disabled);
        contentTable.pad(5f);
        TextureRegion portrait = assetCatalog == null ? null : assetCatalog.plantPortrait(type);
        addPortraitOrFallback(contentTable, portrait, displayName(type.name()), unlocked, 82f);

        Label name = new Label(displayName(type.name()), skin, "medium_outline");
        name.setAlignment(Align.center);
        name.setTouchable(Touchable.disabled);
        contentTable.add(name).width(185f).row();

        String status = unlocked
            ? "LVL " + controller.getPlantLevel(type)
            : "LOCKED";
        if (controller.canUpgrade(type)) {
            status += "  •  READY";
        }
        Label detail = new Label(status, skin);
        detail.setAlignment(Align.center);
        detail.setTouchable(Touchable.disabled);
        contentTable.add(detail).width(185f);

        Stack stack = new Stack(background, contentTable);
        stack.setTouchable(Touchable.enabled);
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedPlant = type;
                rebuild();
            }
        });
        return stack;
    }

    private boolean plantMatchesFilters(Collection controller, PlantType type) {
        boolean unlocked = controller.isPlantUnlocked(type);
        boolean statusMatches = switch (statusFilter) {
            case "UNLOCKED" -> unlocked;
            case "LOCKED" -> !unlocked;
            case "UPGRADEABLE" -> controller.canUpgrade(type);
            default -> true;
        };
        return statusMatches
            && ("ANY".equals(familyFilter)
            || familyFilter.equals(controller.getPlantFamily(type)));
    }

    private Table buildPlantDetail(Collection controller) {
        Table detail = pvzPanel();
        detail.top();
        Label heading = mediumTitle("PLANT DETAILS");
        heading.setAlignment(Align.center);
        detail.add(heading).colspan(2).padBottom(12f).row();

        if (selectedPlant == null) {
            Label message = wrappedLabel("Select a plant card to view its details.", 320f);
            message.setAlignment(Align.center);
            detail.add(message).colspan(2).width(320f).pad(24f);
            return detail;
        }

        PlantType type = selectedPlant;
        boolean unlocked = controller.isPlantUnlocked(type);
        TextureRegion portrait = assetCatalog == null ? null : assetCatalog.plantPortrait(type);
        Table portraitBox = new Table();
        addPortraitOrFallback(portraitBox, portrait, displayName(type.name()), unlocked, 132f);
        detail.add(portraitBox).colspan(2).height(138f).padBottom(8f).row();

        Label name = mediumTitle(displayName(type.name()));
        name.setAlignment(Align.center);
        detail.add(name).colspan(2).padBottom(10f).row();
        addDetail(detail, "STATUS", unlocked ? "UNLOCKED" : "LOCKED");
        addDetail(detail, "FAMILY", controller.getPlantFamily(type));
        addDetail(detail, "LEVEL", String.valueOf(controller.getPlantLevel(type)));
        addDetail(
            detail,
            "SEEDS",
            controller.isAtMaximumLevel(type)
                ? controller.getSeedCount(type) + " (MAX LEVEL)"
                : controller.getSeedCount(type) + " / " + controller.getRequiredSeeds(type)
        );

        PlantData data = controller.getPlantData(type);
        if (data == null) {
            addDetail(detail, "STATS", "Plant data has not loaded yet.");
        } else {
            addDetail(detail, "HEALTH", number(data.getHp()));
            addDetail(detail, "SUN COST", number(data.getCost()));
            addDetail(detail, "RECHARGE", number(data.getRecharge()));
            addDetail(detail, "DAMAGE", number(data.getDamage()));
            addDetail(detail, "TAGS", data.getTags().isEmpty() ? "NONE" : data.getTags().toString());
        }

        if (!unlocked) {
            detail.add(greenButton(
                "BUY - " + Collection.PURCHASE_COST + " COINS",
                () -> confirmPurchase(controller, type)
            )).colspan(2).width(310f).height(52f).padTop(14f);
        } else if (controller.isAtMaximumLevel(type)) {
            detail.add(brownButton("MAXIMUM LEVEL", () ->
                showMessage("This plant is already at maximum level.")
            )).colspan(2).width(310f).height(52f).padTop(14f);
        } else {
            String cost = controller.getRequiredCoins(type) + " COINS + "
                + controller.getRequiredSeeds(type) + " SEEDS";
            detail.add(purpleButton("UPGRADE - " + cost, () ->
                confirmUpgrade(controller, type)
            )).colspan(2).width(340f).height(52f).padTop(14f);
        }
        return detail;
    }

    private void buildZombies(Table table, Collection controller) {
        Table body = new Table();
        ScrollPane list = scrollable(buildZombieGrid(controller));
        body.add(list).width(LIST_WIDTH).height(LIST_HEIGHT).top().padRight(16f);
        body.add(buildZombieDetail(controller)).width(DETAIL_WIDTH).top();
        table.add(body).growX();
    }

    private Table buildZombieGrid(Collection controller) {
        Table panel = pvzPanel();
        panel.top();
        Label title = mediumTitle("ZOMBIE COLLECTION");
        title.setAlignment(Align.center);
        panel.add(title).colspan(3).padBottom(12f).row();

        int column = 0;
        for (ZombieRegistry.ZombieType type : controller.getAllZombies()) {
            panel.add(buildZombieCard(controller, type)).width(205f).height(148f).pad(5f);
            column++;
            if (column == 3) {
                panel.row();
                column = 0;
            }
        }
        return panel;
    }

    private Actor buildZombieCard(Collection controller, ZombieRegistry.ZombieType type) {
        boolean unlocked = controller.isZombieUnlocked(type);
        boolean selected = type == selectedZombie;
        TextButton background = selected
            ? purpleButton("", null)
            : unlocked ? greenButton("", null) : brownButton("", null);
        background.setTouchable(Touchable.disabled);

        Table contentTable = new Table();
        contentTable.setTouchable(Touchable.disabled);
        contentTable.pad(5f);
        TextureRegion portrait = unlocked && assetCatalog != null
            ? assetCatalog.zombiePortrait(type)
            : null;
        addPortraitOrFallback(
            contentTable,
            portrait,
            unlocked ? displayName(type.name()) : "?",
            unlocked,
            82f
        );

        Label name = new Label(
            unlocked ? displayName(type.name()) : "UNDISCOVERED",
            skin,
            "medium_outline"
        );
        name.setAlignment(Align.center);
        name.setTouchable(Touchable.disabled);
        contentTable.add(name).width(185f).row();

        Label status = new Label(unlocked ? "DISCOVERED" : "LOCKED", skin);
        status.setAlignment(Align.center);
        status.setTouchable(Touchable.disabled);
        contentTable.add(status).width(185f);

        Stack stack = new Stack(background, contentTable);
        stack.setTouchable(Touchable.enabled);
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedZombie = type;
                rebuild();
            }
        });
        return stack;
    }

    private Table buildZombieDetail(Collection controller) {
        Table detail = pvzPanel();
        detail.top();
        Label heading = mediumTitle("ZOMBIE DETAILS");
        heading.setAlignment(Align.center);
        detail.add(heading).colspan(2).padBottom(12f).row();

        if (selectedZombie == null) {
            Label message = wrappedLabel("Select a zombie card to view its details.", 320f);
            message.setAlignment(Align.center);
            detail.add(message).colspan(2).width(320f).pad(24f);
            return detail;
        }
        if (!controller.isZombieUnlocked(selectedZombie)) {
            Label hidden = wrappedLabel(
                "This zombie has not been discovered yet. Its identity and stats remain hidden.",
                320f
            );
            hidden.setAlignment(Align.center);
            detail.add(hidden).colspan(2).width(320f).pad(24f);
            return detail;
        }

        TextureRegion portrait = assetCatalog == null
            ? null
            : assetCatalog.zombiePortrait(selectedZombie);
        Table portraitBox = new Table();
        addPortraitOrFallback(portraitBox, portrait, displayName(selectedZombie.name()), true, 140f);
        detail.add(portraitBox).colspan(2).height(146f).padBottom(8f).row();

        Label name = mediumTitle(displayName(selectedZombie.name()));
        name.setAlignment(Align.center);
        detail.add(name).colspan(2).padBottom(10f).row();
        addDetail(detail, "DISCOVERED", "YES");
        addDetail(detail, "TYPE", selectedZombie.name());

        Collection.ZombiePreview preview = controller.getZombiePreview(selectedZombie);
        if (preview == null) {
            addDetail(detail, "STATS", "Preview data is unavailable.");
        } else {
            addDetail(detail, "HEALTH", number(preview.getHealth()));
            addDetail(detail, "DAMAGE", number(preview.getDamage()));
            addDetail(detail, "SPEED", number(preview.getSpeed()));
            addDetail(detail, "WAVE COST", String.valueOf(preview.getWaveCost()));
            addDetail(detail, "ARMOR", preview.getArmor());
            addDetail(detail, "ABILITIES", preview.getAbilities());
        }
        return detail;
    }

    private void addPortraitOrFallback(
        Table target,
        TextureRegion portrait,
        String fallbackText,
        boolean active,
        float size
    ) {
        if (portrait != null) {
            Image image = new Image(portrait);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            if (!active) {
                image.setColor(Color.DARK_GRAY);
            }
            target.add(image).size(size).row();
        } else {
            Label fallback = new Label(fallbackText, skin, "medium_outline");
            fallback.setAlignment(Align.center);
            fallback.setTouchable(Touchable.disabled);
            if (!active) {
                fallback.setColor(Color.GRAY);
            }
            target.add(fallback).width(size + 45f).height(size).center().row();
        }
    }

    private void addDetail(Table table, String key, String value) {
        Label keyLabel = new Label(key, skin, "medium_outline");
        Label valueLabel = wrappedLabel(value == null ? "" : value, 215f);
        table.add(keyLabel).width(118f).left().pad(4f);
        table.add(valueLabel).width(215f).left().pad(4f).row();
    }

    private ScrollPane scrollable(Table contentTable) {
        ScrollPane pane = new ScrollPane(contentTable, skin);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        return pane;
    }

    private void confirmPurchase(Collection controller, PlantType type) {
        showConfirmation(
            "Purchase Plant",
            "Buy " + displayName(type.name()) + " for " + Collection.PURCHASE_COST + " coins?",
            () -> finishAction(controller.buyPlant(type))
        );
    }

    private void confirmUpgrade(Collection controller, PlantType type) {
        showConfirmation(
            "Upgrade Plant",
            "Upgrade " + displayName(type.name()) + " to level "
                + (controller.getPlantLevel(type) + 1) + "?",
            () -> finishAction(controller.upgradePlant(type))
        );
    }

    private void finishAction(String result) {
        showMessage(result);
        refreshResourceLabels();
        if (result != null && !result.startsWith("Error:")) {
            rebuild();
        }
    }

    private void switchTab(boolean plants) {
        plantTab = plants;
        rebuild();
    }

    private void rebuild() {
        if (content == null) {
            return;
        }
        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }

    private void closeAssets() {
        if (assetCatalog != null) {
            assetCatalog.close();
            assetCatalog = null;
        }
    }

    private static String displayName(String enumName) {
        if (enumName == null) {
            return "";
        }
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static String number(float value) {
        return value == Math.round(value)
            ? String.valueOf(Math.round(value))
            : String.format(Locale.ROOT, "%.2f", value);
    }
}

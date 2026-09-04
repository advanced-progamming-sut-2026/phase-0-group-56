package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Scaling;

import controllers.datacontroller.PlantData;
import controllers.menus.secondarymenus.Collection;
import models.entity.Zombie;
import models.entity.ZombieRegistry;
import models.entity.ZombieState;
import models.entity.Armor;
import models.entity.ability.Ability;
import models.entity.ability.BulletAbility;
import models.entity.ability.ExplodeAbility;
import models.entity.ability.MoveAbility;
import models.entity.ability.RandomChooserAbility;
import models.entity.ability.SpawnAbility;
import models.entity.ability.SpeedChangeAbility;
import models.entity.ability.SunRobbingAbility;
import models.factory.builder.PlantType;
import models.gamepanes.Tile;
import view.components.CollectionAssetCatalog;

import java.util.List;
import java.util.StringJoiner;

public class CollectionView extends View {

    private boolean plantTab = true;

    private String filter = "ALL";
    private String familyFilter = "ANY";

    private PlantType selectedPlant;
    private ZombieRegistry.ZombieType selectedZombie;

    private CollectionAssetCatalog assetCatalog;

    public CollectionView() {
        menu = new Collection();
    }

    @Override
    public void show() {
        assetCatalog = CollectionAssetCatalog.create();
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
        closeAssetCatalog();
        super.hide();
    }

    @Override
    public void dispose() {
        closeAssetCatalog();
        super.dispose();
    }

    private void closeAssetCatalog() {
        if (assetCatalog != null) {
            assetCatalog.close();
            assetCatalog = null;
        }
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

        Collection controller =
            (Collection) menu;

        table.add(
                menuSectionHeader(
                    "almanac",
                    "COLLECTION",
                    "Browse unlocked plants and zombies, inspect details, and manage upgrades."
                )
            )
            .width(820f)
            .padBottom(14f)
            .row();

        buildTabs(table);

        if (plantTab) {
            buildPlants(
                table,
                controller
            );
        } else {
            buildZombies(
                table,
                controller
            );
        }
    }

    private void buildTabs(
        Table table
    ) {

        Table tabPanel =
            pvzInnerPanel();

        TextButton plants =
            plantTab
                ? greenButton(
                "PLANTS",
                () -> {
                }
            )
                : brownButton(
                "PLANTS",
                () -> {
                    plantTab = true;
                    rebuild();
                }
            );

        TextButton zombies =
            !plantTab
                ? purpleButton(
                "ZOMBIES",
                () -> {
                }
            )
                : brownButton(
                "ZOMBIES",
                () -> {
                    plantTab = false;
                    rebuild();
                }
            );

        tabPanel.add(plants)
            .width(220f)
            .height(54f)
            .padRight(12f);

        tabPanel.add(zombies)
            .width(220f)
            .height(54f);

        table.add(tabPanel)
            .width(520f)
            .padBottom(16f)
            .row();
    }

    /*
     * ============================================================
     * PLANTS
     * ============================================================
     */

    private void buildPlants(
        Table table,
        Collection controller
    ) {

        buildPlantFilters(table, controller);

        Table mainPanel =
            new Table();

        Table plantList =
            buildPlantList(
                controller
            );

        Table detail =
            buildPlantDetail(
                controller
            );

        mainPanel.add(plantList)
            .width(700f)
            .top()
            .padRight(18f);

        mainPanel.add(detail)
            .width(410f)
            .top();

        table.add(mainPanel)
            .growX();
    }

    private void buildPlantFilters(
        Table table,
        Collection controller
    ) {

        Table filters =
            pvzInnerPanel();

        SelectBox<String> filterBox =
            new SelectBox<>(skin);

        filterBox.setItems(
            new Array<>(
                new String[]{
                    "ALL",
                    "UNLOCKED",
                    "LOCKED",
                    "UPGRADEABLE"
                }
            )
        );

        filterBox.setSelected(
            filter
        );

        filterBox.addListener(
            new ChangeListener() {
                @Override
                public void changed(
                    ChangeEvent event,
                    Actor actor
                ) {

                    filter =
                        filterBox.getSelected();

                    rebuild();
                }
            }
        );

        SelectBox<String> familyBox =
            new SelectBox<>(skin);

        Array<String> familyItems =
            new Array<>();

        familyItems.add("ANY");

        for (String category : controller.getPlantCategories()) {
            familyItems.add(category);
        }

        // A save created by an older build may contain a category value that
        // is no longer present in plants.json. Never leave the SelectBox in a
        // visually selected-but-empty state in that case.
        boolean validFamily = false;
        for (String category : familyItems) {
            if (category.equalsIgnoreCase(familyFilter)) {
                validFamily = true;
                break;
            }
        }
        if (!"ANY".equalsIgnoreCase(familyFilter) && !validFamily) {
            familyFilter = "ANY";
        }

        familyBox.setItems(
            familyItems
        );

        familyBox.setSelected(
            familyFilter
        );

        familyBox.addListener(
            new ChangeListener() {
                @Override
                public void changed(
                    ChangeEvent event,
                    Actor actor
                ) {

                    familyFilter =
                        familyBox.getSelected();

                    rebuild();
                }
            }
        );

        Label statusLabel =
            new Label(
                "STATUS",
                skin,
                "medium_outline"
            );

        Label familyLabel =
            new Label(
                "TYPE",
                skin,
                "medium_outline"
            );

        filters.add(statusLabel)
            .padRight(10f);

        filters.add(filterBox)
            .width(220f)
            .height(42f)
            .padRight(25f);

        filters.add(familyLabel)
            .padRight(10f);

        filters.add(familyBox)
            .width(220f)
            .height(42f);

        table.add(filters)
            .width(800f)
            .padBottom(14f)
            .row();
    }

    private Table buildPlantList(
        Collection controller
    ) {

        Table panel =
            pvzPanel();

        Label title =
            mediumTitle(
                "PLANT COLLECTION"
            );

        title.setAlignment(
            Align.center
        );

        panel.add(title)
            .colspan(3)
            .padBottom(14f)
            .row();

        int shown = 0;

        for (
            PlantType type :
            controller.getAllPlants()
        ) {

            if (
                !plantMatchesFilter(
                    controller,
                    type
                )
            ) {
                continue;
            }

            boolean unlocked =
                controller
                    .isPlantUnlocked(
                        type
                    );

            int level =
                controller
                    .getPlantLevel(
                        type
                    );

            int seeds =
                controller
                    .getSeedCount(
                        type
                    );

            int required =
                controller
                    .getRequiredSeeds(
                        type
                    );

            String text =
                type.name()
                    .replace(
                        '_',
                        ' '
                    )
                    + "\nLVL "
                    + level
                    + "\n"
                    + seeds
                    + " / "
                    + required
                    + " SEEDS"
                    + (
                    unlocked
                        ? ""
                        : "\nLOCKED"
                );

            TextButton actionButton;

            if (!unlocked) {

                actionButton =
                    brownButton(
                        text,
                        () -> {

                            selectedPlant =
                                type;

                            rebuild();
                        }
                    );

            } else if (
                controller.canUpgrade(
                    type
                )
            ) {

                actionButton =
                    purpleButton(
                        text,
                        () -> {

                            selectedPlant =
                                type;

                            rebuild();
                        }
                    );

            } else {

                actionButton =
                    greenButton(
                        text,
                        () -> {

                            selectedPlant =
                                type;

                            rebuild();
                        }
                    );
            }

            panel.add(
                    collectionCard(
                        actionButton,
                        assetCatalog == null
                            ? null
                            : assetCatalog.plantPortrait(type)
                    )
                )
                .width(205f)
                .height(180f)
                .pad(6f);

            shown++;

            if (
                shown % 3 == 0
            ) {
                panel.row();
            }
        }

        if (shown == 0) {

            Label empty =
                wrappedLabel(
                    "No plants match the selected filters.",
                    560f
                );

            empty.setAlignment(
                Align.center
            );

            panel.add(empty)
                .colspan(3)
                .width(560f)
                .pad(30f);
        }

        return panel;
    }

    private boolean plantMatchesFilter(
        Collection controller,
        PlantType type
    ) {

        boolean unlocked =
            controller
                .isPlantUnlocked(
                    type
                );

        boolean statusMatches =
            switch (filter) {

                case "UNLOCKED" ->
                    unlocked;

                case "LOCKED" ->
                    !unlocked;

                case "UPGRADEABLE" ->
                    controller.canUpgrade(
                        type
                    );

                default ->
                    true;
            };

        if (!statusMatches) {
            return false;
        }

        if (
            "ANY".equals(
                familyFilter
            )
        ) {
            return true;
        }

        return controller.getPlantCategory(type)
            .trim()
            .equalsIgnoreCase(familyFilter.trim());
    }

    private Table buildPlantDetail(
        Collection controller
    ) {

        Table detail =
            pvzPanel();

        if (selectedPlant == null) {

            Label select =
                wrappedLabel(
                    "Select a plant card to view its details.",
                    330f
                );

            select.setAlignment(
                Align.center
            );

            detail.add(
                    mediumTitle(
                        "PLANT DETAILS"
                    )
                )
                .padBottom(16f)
                .row();

            detail.add(select)
                .width(330f)
                .pad(20f);

            return detail;
        }

        PlantType type =
            selectedPlant;

        boolean unlocked =
            controller
                .isPlantUnlocked(
                    type
                );

        PlantData data =
            controller
                .getPlantData(
                    type
                );

        Label title =
            mediumTitle(
                type.name()
                    .replace(
                        '_',
                        ' '
                    )
            );

        title.setAlignment(
            Align.center
        );

        detail.add(title)
            .colspan(2)
            .padBottom(14f)
            .row();

        addPortrait(
            detail,
            assetCatalog == null
                ? null
                : assetCatalog.plantPortrait(type)
        );

        addDetail(
            detail,
            "STATUS",
            unlocked
                ? "UNLOCKED"
                : "LOCKED"
        );

        addDetail(
            detail,
            "LEVEL",
            String.valueOf(
                controller
                    .getPlantLevel(
                        type
                    )
            )
        );

        addDetail(
            detail,
            "SEEDS",
            controller
                .getSeedCount(type)
                + " / "
                + controller
                .getRequiredSeeds(
                    type
                )
        );

        if (data != null) {

            addDetail(
                detail,
                "HEALTH",
                String.valueOf(
                    data.getHp()
                )
            );

            addDetail(
                detail,
                "SUN COST",
                String.valueOf(
                    data.getCost()
                )
            );

            addDetail(
                detail,
                "RECHARGE",
                String.valueOf(
                    data.getRecharge()
                )
            );

            addDetail(
                detail,
                "DAMAGE",
                String.valueOf(
                    data.getDamage()
                )
            );

            addDetail(
                detail,
                "TAGS",
                data.getTags() == null
                    ? "-"
                    : data.getTags()
                    .toString()
            );

        } else {

            addDetail(
                detail,
                "STATS",
                "Plant data is not loaded yet."
            );
        }

        addDetail(
            detail,
            "TYPE",
            controller.getPlantCategory(type)
        );

        if (!unlocked) {

            detail.add(
                    greenButton(
                        "BUY - 2000 COINS",
                        () ->
                            showConfirmation(
                                "Purchase Plant",
                                "Buy "
                                    + type.name()
                                    + " for 2000 coins?",
                                () -> {

                                    String result =
                                        controller
                                            .buyPlant(
                                                type.name()
                                            );

                                    showMessage(
                                        result
                                    );

                                    refreshResourceLabels();

                                    if (
                                        !result.startsWith(
                                            "Error:"
                                        )
                                    ) {
                                        rebuild();
                                    }
                                }
                            )
                    )
                )
                .colspan(2)
                .width(290f)
                .height(54f)
                .padTop(18f)
                .row();

        } else {

            String upgradeText =
                "UPGRADE - "
                    + controller
                    .getRequiredCoins(
                        type
                    )
                    + " COINS + "
                    + controller
                    .getRequiredSeeds(
                        type
                    )
                    + " SEEDS";

            detail.add(
                    purpleButton(
                        upgradeText,
                        () ->
                            showConfirmation(
                                "Upgrade Plant",
                                "Upgrade "
                                    + type.name()
                                    + "?",
                                () -> {

                                    String result =
                                        controller
                                            .upgradePlant(
                                                type
                                            );

                                    showMessage(
                                        result
                                    );

                                    refreshResourceLabels();

                                    if (
                                        !result.startsWith(
                                            "Error:"
                                        )
                                    ) {
                                        rebuild();
                                    }
                                }
                            )
                    )
                )
                .colspan(2)
                .width(340f)
                .height(54f)
                .padTop(18f)
                .row();
        }

        return detail;
    }

    /*
     * ============================================================
     * ZOMBIES
     * ============================================================
     */

    private void buildZombies(
        Table table,
        Collection controller
    ) {

        Table body =
            new Table();

        Table list =
            pvzPanel();

        Label listTitle =
            mediumTitle(
                "ZOMBIE COLLECTION"
            );

        listTitle.setAlignment(
            Align.center
        );

        list.add(listTitle)
            .colspan(3)
            .padBottom(14f)
            .row();

        int index = 0;

        for (
            ZombieRegistry.ZombieType type :
            controller.getUnlockedZombies()
        ) {

            String text = type.name().replace('_', ' ');

            TextButton actionButton = purpleButton(
                text,
                () -> {
                    selectedZombie = type;
                    rebuild();
                }
            );

            list.add(
                    collectionCard(
                        actionButton,
                        assetCatalog == null
                            ? null
                            : assetCatalog.zombiePortrait(type)
                    )
                )
                .width(205f)
                .height(160f)
                .pad(6f);

            index++;

            if (
                index % 3 == 0
            ) {
                list.row();
            }
        }

        if (index == 0) {
            Label empty = wrappedLabel(
                "No zombies have been discovered yet. Encounter a zombie in a level to unlock it here.",
                560f
            );
            empty.setAlignment(Align.center);
            list.add(empty)
                .colspan(3)
                .width(560f)
                .pad(30f);
        }

        Table detail =
            buildZombieDetail(
                controller
            );

        body.add(list)
            .width(700f)
            .top()
            .padRight(18f);

        body.add(detail)
            .width(410f)
            .top();

        table.add(body)
            .growX();
    }

    private Table buildZombieDetail(
        Collection controller
    ) {

        Table detail =
            pvzPanel();

        Label title =
            mediumTitle(
                "ZOMBIE DETAILS"
            );

        title.setAlignment(
            Align.center
        );

        detail.add(title)
            .colspan(2)
            .padBottom(16f)
            .row();

        if (
            selectedZombie
                == null
        ) {

            Label message =
                wrappedLabel(
                    "Select a zombie card to view its details.",
                    330f
                );

            message.setAlignment(
                Align.center
            );

            detail.add(message)
                .colspan(2)
                .width(330f)
                .pad(20f);

            return detail;
        }

        if (
            !controller
                .isZombieUnlocked(
                    selectedZombie
                )
        ) {

            Label hidden =
                wrappedLabel(
                    "This zombie has not been discovered yet. "
                        + "Its details remain hidden until encountered.",
                    330f
                );

            hidden.setAlignment(
                Align.center
            );

            detail.add(hidden)
                .colspan(2)
                .width(330f)
                .pad(20f);

            return detail;
        }

        Label name =
            mediumTitle(
                selectedZombie
                    .name()
                    .replace(
                        '_',
                        ' '
                    )
            );

        name.setAlignment(
            Align.center
        );

        detail.add(name)
            .colspan(2)
            .padBottom(12f)
            .row();

        Zombie preview =
            controller
                .createZombiePreview(
                    selectedZombie
                );

        if (preview != null && assetCatalog != null) {
            detail.add(new CollectionZombiePreviewActor(assetCatalog, preview))
                .colspan(2)
                .width(300f)
                .height(190f)
                .center()
                .padBottom(12f)
                .row();
        } else {
            addPortrait(
                detail,
                assetCatalog == null
                    ? null
                    : assetCatalog.zombiePortrait(selectedZombie)
            );
        }

        addDetail(
            detail,
            "DISCOVERED",
            "YES"
        );

        addDetail(
            detail,
            "TYPE",
            selectedZombie
                .name()
        );

        addDetail(
            detail,
            "ABOUT",
            controller.getZombieDescription(selectedZombie)
        );

        if (preview != null) {

            addDetail(
                detail,
                "HEALTH",
                String.valueOf(
                    preview.getMaxHp()
                )
            );

            addDetail(
                detail,
                "DAMAGE",
                String.valueOf(
                    preview.getDamage()
                )
            );

            addDetail(
                detail,
                "SPEED",
                String.valueOf(
                    Math.abs(
                        preview.getSpeed()
                    )
                )
            );

            addDetail(
                detail,
                "WAVE COST",
                String.valueOf(
                    preview.getCost()
                )
            );

            addDetail(
                detail,
                "ARMOR",
                formatArmors(preview.getArmors())
            );

            addDetail(
                detail,
                "ABILITIES",
                formatAbilities(preview.getAbilities())
            );
        }

        return detail;
    }

    /**
     * Converts model abilities to stable, human-readable text. Calling
     * List#toString() here used Object.toString() and exposed JVM
     * implementation names/addresses in the Collection screen.
     */
    private static String formatAbilities(List<Ability> abilities) {
        if (abilities == null || abilities.isEmpty()) {
            return "NONE";
        }

        StringJoiner result = new StringJoiner(", ");
        for (Ability ability : abilities) {
            if (ability != null) {
                result.add(formatAbility(ability));
            }
        }
        String text = result.toString();
        return text.isEmpty() ? "NONE" : text;
    }

    private static String formatArmors(List<Armor> armors) {
        if (armors == null || armors.isEmpty()) {
            return "NONE";
        }

        StringJoiner result = new StringJoiner(", ");
        for (Armor armor : armors) {
            if (armor == null) {
                continue;
            }
            String type = armor.getType();
            if (type == null || type.isBlank()) {
                type = "Unknown armor";
            }
            result.add(type.replace('_', ' '));
        }
        String text = result.toString();
        return text.isEmpty() ? "NONE" : text;
    }

    private static String formatAbility(Ability ability) {
        if (ability instanceof BulletAbility) {
            return "Ranged attack";
        }
        if (ability instanceof ExplodeAbility) {
            return "Explosion";
        }
        if (ability instanceof SunRobbingAbility) {
            return "Steals sunlight";
        }
        if (ability instanceof SpawnAbility) {
            return "Summons reinforcements";
        }
        if (ability instanceof SpeedChangeAbility) {
            return "Speed boost";
        }
        if (ability instanceof RandomChooserAbility) {
            return "Special support";
        }
        if (ability instanceof MoveAbility move) {
            if (move.getType() == null) {
                return "Special movement";
            }
            return switch (move.getType()) {
                case PUSH_ARCADE -> "Pushes plants";
                case PUSH_ICE -> "Pushes ice";
                case PUSH_BARREL -> "Rolls a barrel";
                case PULL_PLANT -> "Pulls plants";
                case SWAP_ZOMBIE -> "Moves other zombies";
                case PIANO -> "Changes lanes";
            };
        }

        String name = ability.getClass().getSimpleName()
            .replaceAll("([a-z])([A-Z])", "$1 $2")
            .trim();
        return name.isEmpty() ? "Special ability" : name;
    }

    /**
     * A presentation-only Scene2D actor for the selected zombie. The model is
     * a factory preview with zero speed, so ZombieRenderer selects its idle
     * clip while the actor advances only the animation clock. No gameplay
     * collection or combat state is mutated.
     */
    private static final class CollectionZombiePreviewActor extends Actor {
        private final CollectionAssetCatalog catalog;
        private final Zombie zombie;
        private final Rectangle bounds = new Rectangle();

        private CollectionZombiePreviewActor(
            CollectionAssetCatalog catalog,
            Zombie zombie
        ) {
            this.catalog = catalog;
            this.zombie = zombie;
            setTouchable(Touchable.disabled);

            zombie.setSpeed(0f);
            zombie.setLine(2);
            zombie.setX(4f * Tile.getWidth());
            zombie.setState(ZombieState.IDLE);
            catalog.preloadZombie(zombie);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            zombie.setSpeed(0f);
            zombie.setState(ZombieState.IDLE);
            zombie.updateStateTime(Math.min(Math.max(delta, 0f), 1f / 20f));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (catalog == null || zombie == null || batch == null) {
                return;
            }
            bounds.set(getX(), getY(), getWidth(), getHeight());
            catalog.renderZombie(batch, zombie, bounds);
        }
    }

    private void addPortrait(
        Table detail,
        TextureRegion region
    ) {
        if (region == null) {
            return;
        }

        Image portrait = new Image(region);
        portrait.setScaling(Scaling.fit);

        detail.add(portrait)
            .colspan(2)
            .size(128f)
            .center()
            .padBottom(12f)
            .row();
    }

    private Table collectionCard(
        TextButton actionButton,
        TextureRegion region
    ) {
        Table card = pvzInnerPanel();

        if (region != null) {
            Image portrait = new Image(region);
            portrait.setScaling(Scaling.fit);

            card.add(portrait)
                .size(58f)
                .center()
                .padBottom(4f)
                .row();
        }

        card.add(actionButton)
            .width(185f)
            .height(region == null ? 86f : 78f)
            .center();

        return card;
    }

    private void addDetail(
        Table table,
        String key,
        String value
    ) {

        Label keyLabel =
            new Label(
                key,
                skin,
                "medium_outline"
            );

        Label valueLabel =
            wrappedLabel(
                value == null
                    ? ""
                    : value,
                235f
            );

        table.add(keyLabel)
            .width(125f)
            .left()
            .pad(5f);

        table.add(valueLabel)
            .width(235f)
            .left()
            .pad(5f)
            .row();
    }

    private void rebuild() {

        content.clearChildren();

        buildContent(
            content
        );

        refreshResourceLabels();
    }
}

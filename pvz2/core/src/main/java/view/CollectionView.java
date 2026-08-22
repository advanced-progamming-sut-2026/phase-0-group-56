package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

import controllers.datacontroller.PlantData;
import controllers.menus.secondarymenus.Collection;
import models.entity.PlantCategory;
import models.entity.Zombie;
import models.entity.ZombieRegistry;
import models.factory.builder.PlantType;
import view.components.CollectionAssetCatalog;

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

        buildPlantFilters(table);

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
        Table table
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

        for (
            PlantCategory category :
            PlantCategory.values()
        ) {

            familyItems.add(
                category.name()
            );
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
                "FAMILY",
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

        return type.getCategory()
            != null
            &&
            type.getCategory()
                .name()
                .equals(
                    familyFilter
                );
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

        String category =
            type.getCategory()
                == null
                ? "UNKNOWN"
                : type.getCategory()
                .name();

        addDetail(
            detail,
            "FAMILY",
            category
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
            controller.getAllZombies()
        ) {

            boolean unlocked =
                controller
                    .isZombieUnlocked(
                        type
                    );

            String text =
                unlocked
                    ? type.name()
                    .replace(
                        '_',
                        ' '
                    )
                    : "???\nUNDISCOVERED";

            TextButton actionButton =
                unlocked
                    ? purpleButton(
                    text,
                    () -> {

                        selectedZombie =
                            type;

                        rebuild();
                    }
                )
                    : brownButton(
                    text,
                    () -> {

                        selectedZombie =
                            type;

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

        addPortrait(
            detail,
            assetCatalog == null
                ? null
                : assetCatalog.zombiePortrait(selectedZombie)
        );

        Zombie preview =
            controller
                .createZombiePreview(
                    selectedZombie
                );

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
                preview.hasArmor()
                    ? preview
                    .getArmors()
                    .toString()
                    : "NONE"
            );

            addDetail(
                detail,
                "ABILITIES",
                preview
                    .getAbilities()
                    .isEmpty()
                    ? "NONE"
                    : preview
                    .getAbilities()
                    .toString()
            );
        }

        return detail;
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

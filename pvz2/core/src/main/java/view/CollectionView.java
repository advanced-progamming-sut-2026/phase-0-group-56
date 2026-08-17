package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;

import controllers.datacontroller.PlantData;
import controllers.menus.secondarymenus.Collection;
import models.entity.ZombieRegistry;
import models.entity.Zombie;
import models.entity.PlantCategory;
import models.factory.builder.PlantType;


public class CollectionView extends View {
    private boolean plantTab = true;
    private String filter = "ALL";
    private String familyFilter = "ANY";
    private PlantType selectedPlant;
    private ZombieRegistry.ZombieType selectedZombie;

    public CollectionView() {
        menu = new Collection();
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

        Table tabs = new Table();
        tabs.add(button(plantTab ? "[ Plants ]" : "Plants", () -> {
            plantTab = true;
            rebuild();
        })).width(180f).height(44f).pad(4f);
        tabs.add(button(!plantTab ? "[ Zombies ]" : "Zombies", () -> {
            plantTab = false;
            rebuild();
        })).width(180f).height(44f).pad(4f);
        table.add(tabs).padBottom(10f).row();

        if (plantTab) {
            buildPlants(table, controller);
        } else {
            buildZombies(table, controller);
        }
    }

    private void buildPlants(Table table, Collection controller) {
        SelectBox<String> filterBox = new SelectBox<>(skin);
        filterBox.setItems(new Array<>(new String[]{"ALL", "UNLOCKED", "LOCKED", "UPGRADEABLE"}));
        filterBox.setSelected(filter);
        filterBox.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                filter = filterBox.getSelected();
                rebuild();
            }
        });

        SelectBox<String> familyBox = new SelectBox<>(skin);
        Array<String> familyItems = new Array<>();
        familyItems.add("ANY");
        for (PlantCategory category : PlantCategory.values()) {
            familyItems.add(category.name());
        }
        familyBox.setItems(familyItems);
        familyBox.setSelected(familyFilter);
        familyBox.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                familyFilter = familyBox.getSelected();
                rebuild();
            }
        });

        Table filterRow = new Table();
        filterRow.add(new Label("Status filter", skin)).padRight(10f);
        filterRow.add(filterBox).width(200f).padRight(20f);
        filterRow.add(new Label("Family", skin)).padRight(10f);
        filterRow.add(familyBox).width(200f);
        table.add(filterRow).padBottom(10f).row();

        Table body = new Table();
        Table list = new Table();
        list.top();
        int shown = 0;
        for (PlantType type : controller.getAllPlants()) {
            if (!plantMatchesFilter(controller, type)) {
                continue;
            }
            boolean unlocked = controller.isPlantUnlocked(type);
            int level = controller.getPlantLevel(type);
            int seeds = controller.getSeedCount(type);
            int required = controller.getRequiredSeeds(type);
            String cardText = type.name().replace('_', ' ')
                + "\nLevel: " + level
                + "\nSeeds: " + seeds + "/" + required
                + (unlocked ? "" : "\nLOCKED");
            TextButton card = button(cardText, () -> {
                selectedPlant = type;
                rebuild();
            });
            list.add(card).width(200f).height(112f).pad(5f);
            shown++;
            if (shown % 3 == 0) {
                list.row();
            }
        }
        if (shown == 0) {
            list.add(new Label("No plants match this filter.", skin)).pad(20f);
        }

        Table detail = buildPlantDetail(controller);
        body.add(list).width(660f).top().padRight(16f);
        body.add(detail).width(420f).top();
        table.add(body).growX();
    }

    private boolean plantMatchesFilter(Collection controller, PlantType type) {
        boolean unlocked = controller.isPlantUnlocked(type);
        boolean statusMatches = switch (filter) {
            case "UNLOCKED" -> unlocked;
            case "LOCKED" -> !unlocked;
            case "UPGRADEABLE" -> controller.canUpgrade(type);
            default -> true;
        };
        if (!statusMatches) {
            return false;
        }
        if ("ANY".equals(familyFilter)) {
            return true;
        }
        return type.getCategory() != null && type.getCategory().name().equals(familyFilter);
    }

    private Table buildPlantDetail(Collection controller) {
        Table detail = new Table();
        if (selectedPlant == null) {
            detail.add(wrappedLabel("Select a plant to view details.", 380f)).width(380f).pad(14f);
            return detail;
        }

        PlantType type = selectedPlant;
        boolean unlocked = controller.isPlantUnlocked(type);
        PlantData data = controller.getPlantData(type);

        detail.add(new Label(type.name().replace('_', ' '), skin)).pad(10f).row();
        addDetail(detail, "Status", unlocked ? "Unlocked" : "Locked");
        addDetail(detail, "Level", String.valueOf(controller.getPlantLevel(type)));
        addDetail(detail, "Seed packets", controller.getSeedCount(type)
            + " / " + controller.getRequiredSeeds(type));

        if (data != null) {
            addDetail(detail, "Health", String.valueOf(data.getHp()));
            addDetail(detail, "Sun cost", String.valueOf(data.getCost()));
            addDetail(detail, "Recharge", String.valueOf(data.getRecharge()));
            addDetail(detail, "Damage", String.valueOf(data.getDamage()));
            addDetail(detail, "Tags", data.getTags() == null ? "-" : data.getTags().toString());
        } else {
            addDetail(detail, "Stats", "PlantData is not loaded in the current project asset setup.");
        }

        String category = type.getCategory() == null ? "Unknown" : type.getCategory().name();
        addDetail(detail, "Family", category);

        if (!unlocked) {
            detail.add(button("Buy - 2000 coins", () -> showConfirmation(
                "Purchase plant",
                "Buy " + type.name() + " for 2000 coins?",
                () -> {
                    String result = controller.buyPlant(type.name());
                    showMessage(result);
                    refreshResourceLabels();
                    if (!result.startsWith("Error:")) {
                        rebuild();
                    }
                }))).width(260f).height(45f).pad(10f).row();
        } else {
            String upgradeText = "Upgrade - " + controller.getRequiredCoins(type)
                + " coins + " + controller.getRequiredSeeds(type) + " seeds";
            detail.add(button(upgradeText, () -> showConfirmation(
                "Upgrade plant",
                "Upgrade " + type.name() + "?",
                () -> {
                    String result = controller.upgradePlant(type);
                    showMessage(result);
                    refreshResourceLabels();
                    if (!result.startsWith("Error:")) {
                        rebuild();
                    }
                }))).width(330f).height(48f).pad(10f).row();
        }
        return detail;
    }

    private void buildZombies(Table table, Collection controller) {
        Table body = new Table();
        Table list = new Table();
        int index = 0;
        for (ZombieRegistry.ZombieType type : controller.getAllZombies()) {
            boolean unlocked = controller.isZombieUnlocked(type);
            String text = unlocked ? type.name().replace('_', ' ') : "???\nUNDISCOVERED";
            TextButton card = button(text, () -> {
                selectedZombie = type;
                rebuild();
            });
            list.add(card).width(205f).height(88f).pad(5f);
            index++;
            if (index % 3 == 0) {
                list.row();
            }
        }

        Table detail = new Table();
        if (selectedZombie == null) {
            detail.add(wrappedLabel("Select a zombie to view details.", 360f)).width(360f).pad(14f);
        } else if (!controller.isZombieUnlocked(selectedZombie)) {
            detail.add(wrappedLabel(
                    "This zombie has not been seen by the current user yet, so its details remain hidden.", 360f))
                .width(360f).pad(14f);
        } else {
            detail.add(new Label(selectedZombie.name().replace('_', ' '), skin)).pad(10f).row();
            Zombie preview = controller.createZombiePreview(selectedZombie);
            addDetail(detail, "Discovered", "Yes");
            addDetail(detail, "Type", selectedZombie.name());
            if (preview != null) {
                addDetail(detail, "Health", String.valueOf(preview.getMaxHp()));
                addDetail(detail, "Damage", String.valueOf(preview.getDamage()));
                addDetail(detail, "Speed", String.valueOf(Math.abs(preview.getSpeed())));
                addDetail(detail, "Wave cost", String.valueOf(preview.getCost()));
                addDetail(detail, "Armor", preview.hasArmor() ? preview.getArmors().toString() : "None");
                addDetail(detail, "Abilities", preview.getAbilities().isEmpty()
                    ? "None" : preview.getAbilities().toString());
            }
        }

        body.add(list).width(690f).top().padRight(16f);
        body.add(detail).width(400f).top();
        table.add(body).growX();
    }

    private void addDetail(Table table, String key, String value) {
        table.add(new Label(key + ":", skin)).width(115f).left().pad(5f);
        table.add(wrappedLabel(value == null ? "" : value, 250f)).width(250f).left().pad(5f).row();
    }

    private void rebuild() {
        content.clearChildren();
        buildContent(content);
        refreshResourceLabels();
    }
}

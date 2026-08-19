package controllers.datacontroller;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import models.entity.PlantTags;

import java.io.Serializable;
import java.util.ArrayList;

public class PlantData implements Serializable, Json.Serializable {
    private int id;
    private String name;
    private float actionInterval;
    private float hp;
    private float cost;
    private ArrayList<PlantTags> tags = new ArrayList<>();
    private float recharge;
    private ArrayList<Upgrade> upgrades = new ArrayList<>();
    private float damage;

    /**
     * Custom LibGDX JSON reader.
     *
     * Why this exists:
     * plants.json currently stores tags as strings and sometimes packs several tags into one
     * string, e.g. ["Shroom, Wramp_wp, Night"].  It also uses "-" for no tags.
     * LibGDX cannot deserialize those values directly into ArrayList<PlantTags>.
     *
     * This reader keeps the public model strongly typed while accepting the existing data.
     */
    @Override
    public void read(Json json, JsonValue jsonData) {
        id = jsonData.getInt("id", 0);
        name = jsonData.getString("name", null);
        actionInterval = jsonData.getFloat("actionInterval", 0f);
        hp = jsonData.getFloat("hp", 0f);
        cost = jsonData.getFloat("cost", 0f);
        recharge = jsonData.getFloat("recharge", 0f);
        damage = jsonData.getFloat("damage", 0f);

        tags = new ArrayList<>();
        JsonValue tagsNode = jsonData.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonValue item = tagsNode.child; item != null; item = item.next) {
                String raw = item.asString();
                if (raw == null) {
                    continue;
                }

                // Some entries contain several tags inside one JSON string.
                String[] pieces = raw.split(",");
                for (String piece : pieces) {
                    PlantTags tag = PlantTags.fromJsonToken(piece);
                    if (tag != null && !tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
        }

        upgrades = new ArrayList<>();
        JsonValue upgradesNode = jsonData.get("upgrades");
        if (upgradesNode != null && upgradesNode.isArray()) {
            for (JsonValue node = upgradesNode.child; node != null; node = node.next) {
                Upgrade upgrade = new Upgrade();
                upgrade.setLevel(node.getInt("level", 0));

                // Repository JSON uses "description" while the game code uses "effect".
                String effect = node.getString("effect", null);
                if (effect == null) {
                    effect = node.getString("description", "");
                }
                upgrade.setEffect(effect);
                upgrade.setSpecialFlag(node.getBoolean("specialFlag", false));
                upgrades.add(upgrade);
            }
        }
    }

    /**
     * Writes a canonical form: one tag per JSON item and "description" for upgrade text.
     * Reading does not depend on this method, but it makes future serialized output clean.
     */
    @Override
    public void write(Json json) {
        json.writeValue("id", id);
        json.writeValue("name", name);
        json.writeValue("cost", cost);
        json.writeValue("hp", hp);
        json.writeValue("damage", damage);
        json.writeValue("actionInterval", actionInterval);
        json.writeValue("recharge", recharge);

        json.writeArrayStart("tags");
        for (PlantTags tag : tags) {
            json.writeValue(tag.name());
        }
        json.writeArrayEnd();

        json.writeArrayStart("upgrades");
        for (Upgrade upgrade : upgrades) {
            json.writeObjectStart();
            json.writeValue("level", upgrade.getLevel());
            json.writeValue("description", upgrade.getEffect());
            json.writeValue("specialFlag", upgrade.isSpecialFlag());
            json.writeObjectEnd();
        }
        json.writeArrayEnd();
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public ArrayList<Upgrade> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(ArrayList<Upgrade> upgrades) {
        this.upgrades = upgrades == null ? new ArrayList<>() : upgrades;
    }

    public float getRecharge() {
        return recharge;
    }

    public void setRecharge(float recharge) {
        this.recharge = recharge;
    }

    public ArrayList<PlantTags> getTags() {
        return tags;
    }

    public void setTags(ArrayList<PlantTags> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public float getActionInterval() {
        return actionInterval;
    }

    public void setActionInterval(float actionInterval) {
        this.actionInterval = actionInterval;
    }

    public float getHp() {
        return hp;
    }

    public void setHp(float hp) {
        this.hp = hp;
    }
}

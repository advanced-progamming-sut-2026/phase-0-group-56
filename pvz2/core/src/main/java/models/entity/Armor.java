package models.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Armor {

    private final String type;
    private int health;
    private final int maxHealth;
    private final boolean magnetic;

    private boolean broken;
    private Zombie owner;

    private final List<String> visibilityKeys = new ArrayList<>();
    private final List<String> alwaysActiveKeys = new ArrayList<>();

    private ArmorState currentState;


    public Armor(String type, int health) {
        this(type, health, false);
    }


    public Armor(String type, int health, boolean magnetic) {
        this.type = type;
        this.health = health;
        this.maxHealth = health;
        this.magnetic = magnetic;
        this.broken = false;
        this.owner = null;
        this.currentState = health > 0 ? ArmorState.STRONG : ArmorState.DESTROYED;
    }


    public Armor withVisibilityKeys(String... keys) {
        visibilityKeys.addAll(Arrays.asList(keys));
        return this;
    }


    public Armor withAlwaysActiveKeys(String... keys) {
        alwaysActiveKeys.addAll(Arrays.asList(keys));
        return this;
    }


    public Armor withVisibilityKey(String key) {
        visibilityKeys.add(key);
        return this;
    }


    public void attachTo(Zombie zombie) {
        this.owner = zombie;
        updateVisibility();
    }


    public void updateVisibility() {

        if (owner == null) {
            return;
        }


        // خاموش کردن همه حالت ها
        for (String key : visibilityKeys) {
            owner.setVisibility(key, false);
        }


        if (broken || health <= 0) {
            currentState = ArmorState.DESTROYED;

            for (String key : alwaysActiveKeys) {
                owner.setVisibility(key, false);
            }

            return;
        }


        float percentage = (float) health / maxHealth;


        ArmorState newState;

        if (percentage > 0.66f) {
            newState = ArmorState.STRONG;
        }
        else if (percentage > 0.33f) {
            newState = ArmorState.DAMAGED;
        }
        else {
            newState = ArmorState.DESTROYED;
        }


        currentState = newState;


        int index = getStateIndex(newState);


        if (index < visibilityKeys.size()) {
            owner.setVisibility(
                visibilityKeys.get(index),
                true
            );
        }


        // همیشه فعال ها
        for (String key : alwaysActiveKeys) {
            owner.setVisibility(key, true);
        }
    }



    private int getStateIndex(ArmorState state) {

        return switch (state) {

            case STRONG -> 0;

            case DAMAGED -> 1;

            case DESTROYED -> 2;
        };
    }



    public void takeDamage(int damage) {

        if (broken || damage <= 0) {
            return;
        }


        health -= damage;


        if (health <= 0) {

            health = 0;
            broken = true;
        }


        updateVisibility();
    }



    public boolean isActive() {
        return !broken && health > 0;
    }



    public String getType() {
        return type;
    }


    public int getHealth() {
        return health;
    }


    public int getMaxHealth() {
        return maxHealth;
    }


    public boolean isMagnetic() {
        return magnetic;
    }


    public boolean isBroken() {
        return broken;
    }


    public ArmorState getCurrentState() {
        return currentState;
    }
}

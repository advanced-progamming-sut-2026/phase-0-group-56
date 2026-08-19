package controllers.datacontroller;

import java.io.Serializable;

public class Upgrade implements Serializable {
    private int level;
    private String effect;
    private boolean specialFlag;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Existing game code uses getEffect().
     * plants.json currently calls the same value "description"; PlantData maps it here.
     */
    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    // Convenience aliases matching the wording used by plants.json.
    public String getDescription() {
        return effect;
    }

    public void setDescription(String description) {
        this.effect = description;
    }

    public boolean isSpecialFlag() {
        return specialFlag;
    }

    public void setSpecialFlag(boolean specialFlag) {
        this.specialFlag = specialFlag;
    }
}

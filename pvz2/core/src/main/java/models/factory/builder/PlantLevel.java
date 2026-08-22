package models.factory.builder;

import models.App;
import models.User;

/** Null-safe access to a plant's persistent upgrade level. */
public final class PlantLevel {
    private PlantLevel() {
    }

    public static int current(PlantType type) {
        User user = App.getCurrentuser();
        if (user == null || type == null || user.getLevels() == null) {
            return 1;
        }
        Integer level = user.getLevels().get(type);
        return level == null ? 1 : Math.max(1, level);
    }
}

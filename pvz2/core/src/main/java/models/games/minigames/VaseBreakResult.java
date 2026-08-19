package models.games.minigames;

import models.factory.builder.PlantType;

/** Result of one attempt to break a vase. */
public final class VaseBreakResult {
    public enum DropKind {
        NONE,
        PLANT,
        ZOMBIE
    }

    private final boolean broken;
    private final Vase vase;
    private final DropKind dropKind;
    private final PlantType plantType;
    private final String message;

    private VaseBreakResult(
        boolean broken,
        Vase vase,
        DropKind dropKind,
        PlantType plantType,
        String message
    ) {
        this.broken = broken;
        this.vase = vase;
        this.dropKind = dropKind == null ? DropKind.NONE : dropKind;
        this.plantType = plantType;
        this.message = message == null ? "" : message;
    }

    public static VaseBreakResult miss() {
        return new VaseBreakResult(
            false,
            null,
            DropKind.NONE,
            null,
            "There is no vase here."
        );
    }

    public static VaseBreakResult plant(Vase vase, PlantType type) {
        return new VaseBreakResult(
            true,
            vase,
            DropKind.PLANT,
            type,
            "A " + pretty(type) + " seed packet dropped."
        );
    }

    public static VaseBreakResult zombie(Vase vase, String zombieName) {
        String name = zombieName == null || zombieName.isBlank()
            ? "zombie"
            : zombieName.replace('_', ' ');
        return new VaseBreakResult(
            true,
            vase,
            DropKind.ZOMBIE,
            null,
            "A " + name + " came out of the vase!"
        );
    }

    public boolean isBroken() {
        return broken;
    }

    public Vase getVase() {
        return vase;
    }

    public DropKind getDropKind() {
        return dropKind;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public String getMessage() {
        return message;
    }

    private static String pretty(PlantType type) {
        return type == null
            ? "plant"
            : type.name().toLowerCase().replace('_', ' ');
    }
}

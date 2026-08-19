package models.entity;

import java.util.Locale;

public enum PlantTags {
    Insta_kill,
    MOVE_ZOMBIES,
    ONCE_USAGE,
    MAGICAL,
    Fire,
    Ice,
    POISON,
    Night,
    Day,
    Shroom,
    Wramp_up,
    Trap,
    EXPLOSIVE,
    AoE,
    WATER,
    STACK,
    SUN,

    // These two tags exist in plants.json but were missing from the enum.
    Pea,
    Charge;

    /**
     * Converts the textual tags used by plants.json to the enum used by the game.
     *
     * The current data contains a few legacy spellings/casing variants, for example:
     *   "Wramp_wp", "stack", "Water", "MoveZombies", "Magic", "Poison".
     * This method intentionally accepts those forms so the data file can be loaded safely.
     *
     * @return the matching tag, or null for an empty/"-" token.
     * @throws IllegalArgumentException if the token is non-empty but unknown.
     */
    public static PlantTags fromJsonToken(String token) {
        if (token == null) {
            return null;
        }

        String normalized = token
            .trim()
            .replace('-', '_')
            .replace(' ', '_')
            .toLowerCase(Locale.ROOT);

        if (normalized.isEmpty() || normalized.equals("_")) {
            return null;
        }

        return switch (normalized) {
            case "insta_kill", "instakill" -> Insta_kill;
            case "movezombies", "move_zombies" -> MOVE_ZOMBIES;
            case "once_usage", "onceusage" -> ONCE_USAGE;
            case "magic", "magical" -> MAGICAL;
            case "fire" -> Fire;
            case "ice" -> Ice;
            case "poison" -> POISON;
            case "night" -> Night;
            case "day" -> Day;
            case "shroom" -> Shroom;
            case "wramp_wp", "wramp_up", "warm_up", "warmup" -> Wramp_up;
            case "trap" -> Trap;
            case "explosive" -> EXPLOSIVE;
            case "aoe" -> AoE;
            case "water" -> WATER;
            case "stack" -> STACK;
            case "sun" -> SUN;
            case "pea" -> Pea;
            case "charge" -> Charge;
            default -> throw new IllegalArgumentException("Unknown plant tag: " + token);
        };
    }
}

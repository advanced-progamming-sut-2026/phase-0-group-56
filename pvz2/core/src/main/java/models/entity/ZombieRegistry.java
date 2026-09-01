package models.entity;

import java.io.Serializable;
import java.util.*;

public class ZombieRegistry implements Serializable {

    // ====== ALL ZOMBIE TYPES IN THE GAME ======
    public enum ZombieType {
        NORMAL,
        CONEHEAD,
        BUCKETHEAD,
        BRICKHEAD,
        KNIGHT,
        IMP,
        GARGANTUAR,
        ALLSTAR,
        ARCADe,
        PARASOL,
        TURQUOISE,
        PROSPECTOR,
        PIANIST,
        NEWSPAPER,
        BARREL_ROLLER,
        RA,
        EXPLORER,
        TOMB_RAISER,
        DODO_RIDER,
        HUNTER,
        TROGLOBITE,
        FISHERMAN,
        SNORKEL,
        OCTOPUS,
        JUGGLER,
        WIZARD,
        KING,
        IMP_DRAGON
    }

    // ====== PER-USER REGISTRY ======
    private final Map<ZombieType, Boolean> registry = new HashMap<>();

    public ZombieRegistry() {
        reset();
    }

    public void unlock(ZombieType type) {
        if (type != null) {
            registry.put(type, true);
        }
    }

    public void unlock(String typeName) {
        unlock(fromFactoryName(typeName));
    }

    public boolean isUnlocked(ZombieType type) {
        return registry.getOrDefault(type, false);
    }

    public boolean isUnlocked(String typeName) {
        return isUnlocked(fromFactoryName(typeName));
    }

    /**
     * Marks a zombie as discovered from the names used by ZombieFactory and
     * the level JSON. The return value is true only for a newly discovered
     * entry, allowing renderers to persist the change once rather than every
     * frame.
     */
    public boolean discover(String typeName) {
        ZombieType type = fromFactoryName(typeName);
        if (type == null || isUnlocked(type)) {
            return false;
        }
        unlock(type);
        return true;
    }

    /** Converts factory/PAM aliases into the stable Collection enum. */
    public static ZombieType fromFactoryName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }

        String key = typeName
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]", "");

        return switch (key) {
            case "NORMAL", "ZOMBIEDEFAULT", "ZOMBIETUTORIAL",
                 "ZOMBIEDARKBASIC" -> ZombieType.NORMAL;
            case "CONE", "CONEHEAD", "ZOMBIEARMOR1",
                 "ZOMBIETUTORIALARMOR1" -> ZombieType.CONEHEAD;
            case "BUCKET", "BUCKETHEAD", "ZOMBIEARMOR2",
                 "ZOMBIETUTORIALARMOR2" -> ZombieType.BUCKETHEAD;
            case "BRICK", "BRICKHEAD", "ZOMBIEARMOR4",
                 "ZOMBIEFUTUREBASICBRICK" -> ZombieType.BRICKHEAD;
            case "KNIGHT", "ZOMBIEDARKARMOR3" -> ZombieType.KNIGHT;
            case "IMP", "ZOMBIEIMP", "ZOMBIETUTORIALIMP" -> ZombieType.IMP;
            case "GARGANTUAR", "ZOMBIEGARGANTUAR", "TUTORIALGARGANTUAR" -> ZombieType.GARGANTUAR;
            case "ALLSTAR", "ZOMBIEMODERNALLSTAR" -> ZombieType.ALLSTAR;
            case "ARCADE", "ZOMBIEARCADE", "ZOMBIE80SARCADE" -> ZombieType.ARCADe;
            case "PARASOL", "ZOMBIELOSTCITYJANE" -> ZombieType.PARASOL;
            case "TURQUOISE", "ZOMBIECAMELDEFAULT", "ZOMBIELOSTCITYCRYSTALSKULL" -> ZombieType.TURQUOISE;
            case "PROSPECTOR", "ZOMBIEPROSPECTOR" -> ZombieType.PROSPECTOR;
            case "PIANO", "PIANIST", "ZOMBIEPIANO" -> ZombieType.PIANIST;
            case "NEWSPAPER", "ZOMBIENEWSPAPER", "ZOMBIEMODERNNEWSPAPER" -> ZombieType.NEWSPAPER;
            case "BARREL", "BARRELROLLER", "BARRELROLL", "ZOMBIEBARREL",
                 "ZOMBIEPIRATEBARRELPUSHER" -> ZombieType.BARREL_ROLLER;
            case "RA", "ZOMBIERA", "ZOMBIEEGYPTRA" -> ZombieType.RA;
            case "EXPLORER", "ZOMBIEEXPLORER" -> ZombieType.EXPLORER;
            case "TOMBRAISER", "ZOMBIETOMBRAISER",
                 "ZOMBIEEGYPTTOMBRAISER" -> ZombieType.TOMB_RAISER;
            case "DODO", "DODORIDER", "ZOMBIEICEAGEDODO", "ZOMBIEICEAGEDODORIDER" -> ZombieType.DODO_RIDER;
            case "HUNTER", "ZOMBIEICEAGEHUNTER" -> ZombieType.HUNTER;
            case "TROGLOBITE", "ZOMBIEICEAGETROGLOBITE" -> ZombieType.TROGLOBITE;
            case "FISHERMAN", "ZOMBIEBEACHFISHERMAN" -> ZombieType.FISHERMAN;
            case "SNORKEL", "ZOMBIEBEACHSNORKEL", "ZOMBIEBEACHSNORKELER" -> ZombieType.SNORKEL;
            case "OCTOPUS", "ZOMBIEBEACHOCTOPUS" -> ZombieType.OCTOPUS;
            case "JUGGLER", "ZOMBIEDARKJUGGLER", "ZOMBIEDARKJESTER" -> ZombieType.JUGGLER;
            case "WIZARD", "ZOMBIEWIZARD", "ZOMBIEDARKWIZARD" -> ZombieType.WIZARD;
            case "KING", "ZOMBIEDARKKING" -> ZombieType.KING;
            case "DRAGONIMP", "IMPDRAGON", "ZOMBIEDARKIMPDRAGON" -> ZombieType.IMP_DRAGON;
            default -> fromEnumName(key);
        };
    }

    private static ZombieType fromEnumName(String key) {
        for (ZombieType type : ZombieType.values()) {
            String enumKey = type.name().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
            if (enumKey.equals(key)) {
                return type;
            }
        }
        return null;
    }

    public List<ZombieType> getUnlockedZombies() {
        List<ZombieType> result = new ArrayList<>();
        for (Map.Entry<ZombieType, Boolean> entry : registry.entrySet()) {
            if (entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public List<ZombieType> getAllZombieTypes() {
        return new ArrayList<>(registry.keySet());
    }

    public int getUnlockedCount() {
        return (int) registry.values().stream().filter(v -> v).count();
    }

    public int getTotalCount() {
        return registry.size();
    }

    public void reset() {
        for (ZombieType type : ZombieType.values()) {
            registry.put(type, false);
        }
    }
}

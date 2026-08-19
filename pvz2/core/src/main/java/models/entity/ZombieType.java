package models.entity;

public enum ZombieType {

    // ====== CORE ======
    NORMAL("normal", "ZombieDefault"),
    CONEHEAD("cone", "ZombieArmor1"),
    BUCKETHEAD("bucket", "ZombieArmor2"),
    BRICKHEAD("brick", "ZombieArmor4"),
    KNIGHT("knight", "ZombieDarkArmor3"),
    IMP("imp", "ZombieImp"),
    GARGANTUAR("gargantuar", "ZombieGargantuar"),
    ALLSTAR("allstar", "ZombieModernAllStar"),
    ARCADE("arcade", "ZombieArcade"),
    PARASOL("parasol", "parasol"),
    TURQUOISE("turquoise", "ZombieCamelDefault"),
    PROSPECTOR("prospector", "ZombieProspector"),
    PIANIST("piano", "ZombiePiano"),
    NEWSPAPER("newspaper", "ZombieNewspaper"),
    BARREL_ROLLER("barrel", "ZombieBarrel"),
    RA("ra", "ZombieRa"),
    EXPLORER("explorer", "ZombieExplorer"),
    TOMB_RAISER("tombraiser", "ZombieTombRaiser"),
    DODO_RIDER("dodo", "ZombieIceAgeDodo"),
    HUNTER("hunter", "ZombieIceAgeHunter"),
    TROGLOBITE("troglobite", "ZombieIceAgeTroglobite"),
    FISHERMAN("fisherman", "ZombieBeachFisherman"),
    SNORKEL("snorkel", "ZombieBeachSnorkel"),
    OCTOPUS("octopus", "ZombieBeachOctopus"),
    JUGGLER("juggler", "ZombieDarkJuggler"),
    WIZARD("wizard", "ZombieWizard"),
    KING("king", "ZombieDarkKing"),
    IMP_DRAGON("dragon_imp", "ZombieDarkImpDragon");

    private final String type;
    private final String jsonAlias;

    ZombieType(String type, String jsonAlias) {
        this.type = type;
        this.jsonAlias = jsonAlias;
    }

    public String getType() {
        return type;
    }

    public String getJsonAlias() {
        return jsonAlias;
    }

    public static ZombieType fromType(String type) {
        for (ZombieType zt : values()) {
            if (zt.type.equalsIgnoreCase(type) || zt.jsonAlias.equalsIgnoreCase(type)) {
                return zt;
            }
        }
        return null;
    }

    public static boolean isValid(String type) {
        return fromType(type) != null;
    }
}
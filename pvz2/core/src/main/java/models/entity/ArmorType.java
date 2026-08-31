package models.entity;


public enum ArmorType {


    CONE(
        "cone",
        370,
        false,
        new String[]{
            "zombie_armor_cone_norm",
            "zombie_armor_cone_damage_01",
            "zombie_armor_cone_damage_02"
        },
        new String[]{}
    ),


    BUCKET(
        "bucket",
        1100,
        true,
        new String[]{
            "zombie_armor_bucket_norm",
            "zombie_armor_bucket_damage_01",
            "zombie_armor_bucket_damage_02"
        },
        new String[]{}
    ),


    BRICK(
        "brick",
        2200,
        false,
        new String[]{
            "zombie_armor_brick_norm",
            "zombie_armor_brick_damage_01",
            "zombie_armor_brick_damage_02"
        },
        new String[]{}
    ),


    CROWN(
        "crown",
        1600,
        true,
        new String[]{
            "zombie_armor_crown_norm",
            "zombie_armor_crown_damage_01",
            "zombie_armor_crown_damage_02"
        },
        new String[]{
            "_zombie_armor_crown_states"
        }
    ),


    SHOULDER(
        "shoulder",
        1600,
        false,
        new String[]{
            "zombie_shoulder_armor_norm",
            "zombie_shoulder_armor_damage_01",
            "zombie_shoulder_armor_damage_02"
        },
        new String[]{
            "zombie_shoulder_armor"
        }
    ),


    NEWSPAPER(
        "newspaper",
        800,
        false,
        new String[]{
            "_zombie_newspaper",
            "_zombie_newspaper_dmg1",
            "_zombie_newspaper_dmg2"
        },
        new String[]{}
    );


    private final String id;
    private final int health;
    private final boolean magnetic;

    private final String[] visibilityKeys;
    private final String[] alwaysActiveKeys;



    ArmorType(
        String id,
        int health,
        boolean magnetic,
        String[] visibilityKeys,
        String[] alwaysActiveKeys
    ) {

        this.id = id;
        this.health = health;
        this.magnetic = magnetic;
        this.visibilityKeys = visibilityKeys;
        this.alwaysActiveKeys = alwaysActiveKeys;
    }



    public String getId() {
        return id;
    }


    public int getHealth() {
        return health;
    }


    public boolean isMagnetic() {
        return magnetic;
    }


    public String[] getVisibilityKeys() {
        return visibilityKeys.clone();
    }


    public String[] getAlwaysActiveKeys() {
        return alwaysActiveKeys.clone();
    }



    public Armor create() {

        Armor armor = new Armor(
            id,
            health,
            magnetic
        );


        armor.withVisibilityKeys(visibilityKeys);
        armor.withAlwaysActiveKeys(alwaysActiveKeys);


        return armor;
    }
}

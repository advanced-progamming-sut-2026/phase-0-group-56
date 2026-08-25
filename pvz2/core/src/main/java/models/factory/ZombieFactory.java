package models.factory;

import models.entity.*;
import models.entity.ability.*;
import controllers.observer.*;

public class ZombieFactory {

    public static Zombie createZombie(String type ) {

        int row = 0;

        // ====== 1. NORMAL ======
        if (type.equals("normal") || type.equals("ZombieDefault")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -150f, 50, 32, 95);
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM");
            return z;
        }

        // ====== 2. CONEHEAD ======
        if (type.equals("cone") || type.equals("ZombieArmor1")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -150f, 50, 32, 95);
            z.addArmor(ArmorType.CONE.create());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM");
            return z;
        }

        // ====== 3. BUCKETHEAD ======
        if (type.equals("bucket") || type.equals("ZombieArmor2")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -92.5f, 60, 32, 95);
            z.addArmor(ArmorType.BUCKET.create());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM");
            return z;
        }

        // ====== 4. KNIGHT ======
        if (type.equals("knight") || type.equals("ZombieDarkArmor3")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -92.5f, 80, 32, 95);
            z.addArmor(ArmorType.CROWN.create());
            z.addArmor(ArmorType.SHOULDER.create());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM");
            return z;
        }

        // ====== 5. BRICKHEAD ======
        if (type.equals("brick") || type.equals("ZombieArmor4")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -92.5f, 700, 32, 95);
            z.addArmor(ArmorType.BRICK.create());
            z.setPamPath("768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM");
            return z;
        }

        // ====== 6. GARGANTUAR ======
        if (type.equals("gargantuar") || type.equals("ZombieGargantuar")) {
            Zombie z = new Zombie(type, type, row, 3600, 1500, -120f, 1500, 62, 95);
            z.addAbility(new SpawnAbility("imp", 1, 0.5f));
            z.setPamPath("768/INITIAL/ZOMBIE/TUTORIAL_GARGANTUAR/TUTORIAL_GARGANTUAR.PAM");
            z.setFire("fire");
            return z;
        }

        // ====== 7. IMP ======
        if (type.equals("imp") || type.equals("ZombieImp")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -110f, 100, 20, 80);
            z.setPamPath("768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL_IMP/ZOMBIE_TUTORIAL_IMP.PAM");
            return z;
        }

        // ====== 8. ALL STAR ======
        if (type.equals("allstar") || type.equals("ZombieModernAllStar")) {
            Zombie z = new Zombie(type, type, row, 1100, 1500, -110f, 1000, 42, 95);
            z.addAbility(new SpeedChangeAbility(0.5f, SpeedChangeAbility.TriggerType.ON_KILL));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_MODERN_ALLSTAR/ZOMBIE_MODERN_ALLSTAR.PAM");
            z.setFire("kick");
            return z;
        }

        // ====== 9. ARCADE ======
        if (type.equals("arcade") || type.equals("ZombieArcade")) {
            Zombie z = new Zombie(type, type, row, 490, 100, -95f, 600, 32, 95);
            z.addAbility(new MoveAbility(MoveAbility.MoveType.PUSH_ARCADE, 1.5f));
            z.addAbility(new ExplodeAbility(1, 1500, 0.5f, ExplodeAbility.Condition.IS_CARRYING));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM");
            z.setFire("push");
            return z;
        }

        // ====== 10. PARASOL ======
        if (type.equals("parasol")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -92.5f, 200, 32, 95);
            z.addBulletObserver(new ParasolObserver());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_JANE/ZOMBIE_LOSTCITY_JANE.PAM");
            return z;
        }

        // ====== 11. TURQUOISE ======
        if (type.equals("turquoise") || type.equals("ZombieCamelDefault")) {
            Zombie z = new Zombie(type, type, row, 380, 100, -92.5f, 300, 32, 95);
            z.addAbility(new SunRobbingAbility(25000, 50));
            z.addAbility(new ExplodeAbility(4, 1500, 8.0f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_LOSTCITY_CRYSTALSKULL/ZOMBIE_LOSTCITY_CRYSTALSKULL.PAM");
            z.setFire("power"); // ra ability
            z.setExtra("attack"); // exploding laser ability
            return z;
        }

        // ====== 12. PROSPECTOR ======
        if (type.equals("prospector") || type.equals("ZombieProspector")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -80f, 200, 32, 95);
            z.addAbility(new ExplodeAbility(0, 1500, 10.0f, ExplodeAbility.Condition.NOT_FROZEN));
            z.addAbility(new SpawnAbility("zombie_reverse", 1, true, false));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM");
            z.setFire("fly");
            return z;
        }

        // ====== 13. PIANIST ======
        if (type.equals("piano") || type.equals("ZombiePiano")) {
            Zombie z = new Zombie(type, type, row, 840, 4000, -60f, 450, 75, 110);
            z.addAbility(new ExplodeAbility(1, 2500, 5.0f));
            z.addAbility(new MoveAbility(MoveAbility.MoveType.PIANO , 2.0f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_PIANO/ZOMBIE_PIANO.PAM");
            z.setWalk(null);
            z.setFire("play");
            z.setEat(null);
            return z;
        }

        // ====== 14. NEWSPAPER ======
        if (type.equals("newspaper") || type.equals("ZombieNewspaper")) {
            Zombie z = new Zombie(type, type, row, 460, 200, -110f, 700, 32, 95);
            z.addArmor(ArmorType.NEWSPAPER.create());
            z.addAbility(new SpeedChangeAbility(4.0f, SpeedChangeAbility.TriggerType.ON_ARMOR_BROKEN));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_MODERN_NEWSPAPER/ZOMBIE_MODERN_NEWSPAPER.PAM");
            z.setIdle("idle_newspaper");
            z.setWalk("walk_newspaper");
            z.setEat("eat_newspaper");
            return z;
        }

        // ====== 15. BARREL ROLLER ======
        if (type.equals("barrel") || type.equals("ZombieBarrel")) {
            Zombie z = new Zombie(type, type, row, 100, 0, -92.5f, 150, 32, 95);
            z.addAbility(new MoveAbility(MoveAbility.MoveType.PUSH_BARREL, 1.5f));
            z.addAbility(new SpawnAbility("imp", 2, true));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER/ZOMBIE_PIRATE_BARREL_PUSHER.PAM");
            return z;
        }

        // ====== 16. RA ======
        if (type.equals("ra") || type.equals("ZombieRa")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -100f, 100, 32, 95);
            z.addAbility(new SunRobbingAbility(25000, 50));
            z.setPamPath("768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM");
            z.setFire("power");
            return z;
        }

        // ====== 17. EXPLORER ======
        if (type.equals("explorer") || type.equals("ZombieExplorer")) {
            Zombie z = new Zombie(type, type, row, 250, 100, -125f, 250, 32, 95);
            z.addAbility(new ExplodeAbility(1, 1500, 0.5f, ExplodeAbility.Condition.TORCH_ON));
            z.addBulletObserver(new TorchObserver());
            z.setPamPath("768/INITIAL/ZOMBIE/ZOMBIE_EXPLORER/ZOMBIE_EXPLORER.PAM");
            return z;
        }

        // ====== 18. TOMB RAISER ======
        if (type.equals("tombraiser") || type.equals("ZombieTombRaiser")) {
            Zombie z = new Zombie(type, type, row, 380, 100, -92.5f, 300, 32, 95);
            z.addAbility(new SpawnAbility("grave", 2, 6.0f));
            z.setPamPath("768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_TOMBRAISER/ZOMBIE_EGYPT_TOMBRAISER.PAM");
            z.setFire("power");
            return z;
        }

        // ====== 19. DODO RIDER ======
        if (type.equals("dodo") || type.equals("ZombieIceAgeDodo")) {
            Zombie z = new Zombie(type, type, row, 490, 100, -150f, 600, 46, 95);
            z.addBulletObserver(new PassThroughObserver());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM");
            z.setIdle("idle3");
            z.setFire("fly_start");
            return z;
        }

        // ====== 20. HUNTER ======
        if (type.equals("hunter") || type.equals("ZombieIceAgeHunter")) {
            Zombie z = new Zombie(type, type, row, 700, 100, -60f, 500, 32, 95);
            z.addAbility(new BulletAbility(ProjectileType.ICE, 2.0f, 4.0f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM");
            z.setFire("throw");
            return z;
        }

        // ====== 21. TROGLOBITE ======
        if (type.equals("troglobite") || type.equals("ZombieIceAgeTroglobite")) {
            Zombie z = new Zombie(type, type, row, 470, 100, -92.5f, 600, 32, 95);
            z.addAbility(new MoveAbility(MoveAbility.MoveType.PUSH_ICE, 1.5f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_ICEAGE_TROGLOBITE/ZOMBIE_ICEAGE_TROGLOBITE.PAM");
            z.setFire("push");
            return z;
        }

        // ====== 22. FISHERMAN ======
        if (type.equals("fisherman") || type.equals("ZombieBeachFisherman")) {
            Zombie z = new Zombie(type, type, row, 1000, 100, -1.0f, 700, 32, 95);
            z.addAbility(new MoveAbility(MoveAbility.MoveType.PULL_PLANT, 2.5f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM");
            z.setExtra("intro");
            z.setFire("toss");
            z.setDie("die2");
            return z;
        }

        // ====== 23. SNORKEL =====
        if (type.equals("snorkel") || type.equals("ZombieBeachSnorkel")) {
            Zombie z = new Zombie(type, type, row, 350, 100, -92.5f, 200, 32, 105);
            z.addBulletObserver(new SnorkelObserver());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_BEACH_SNORKELER/ZOMBIE_BEACH_SNORKELER.PAM");
            return z;
        }

        // ====== 24. OCTOPUS ======
        if (type.equals("octopus") || type.equals("ZombieBeachOctopus")) {
            Zombie z = new Zombie(type, type, row, 910, 100, -60f, 900, 32, 95);
            z.addAbility(new BulletAbility(ProjectileType.ICE, 2.0f, 4.0f));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM");
            z.setFire("toss");
            return z;
        }

        // ====== 25. JUGGLER ======
        if (type.equals("juggler") || type.equals("ZombieDarkJuggler")) {
            Zombie z = new Zombie(type, type, row, 420, 100, -50f, 450, 32, 95);
            z.addBulletObserver(new JugglerObserver());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM");
            z.setFire("spin");
            return z;
        }

        // ====== 26. WIZARD ======
        if (type.equals("wizard") || type.equals("ZombieWizard")) {
            Zombie z = new Zombie(type, type, row, 490, 100, -60f, 800, 32, 95);
            z.addAbility(new RandomChooserAbility(
                2.0f, 3.0f,
                RandomChooserAbility.TargetType.PLANT,
                RandomChooserAbility.ActionType.TURN_TO_CAT
            ));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM");
            z.setFire("sheep");
            return z;
        }

        // ====== 27. KING ======
        if (type.equals("king") || type.equals("ZombieDarkKing")) {
            Zombie z = new Zombie(type, type, row, 1000, 100, -1f, 750, 32, 95);
            z.addAbility(new RandomChooserAbility(
                4.0f, 10.0f,
                RandomChooserAbility.TargetType.ZOMBIE,
                RandomChooserAbility.ActionType.GIVE_ARMOR
            ));
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM");
            z.setExtra("intro");
            z.setFire("special");
            z.setWalk("idle2");
            return z;
        }

        // ====== 28. IMP DRAGON ======
        if (type.equals("dragon_imp") || type.equals("ZombieDarkImpDragon")) {
            Zombie z = new Zombie(type, type, row, 190, 100, -92.5f, 150, 20, 80);
            z.addBulletObserver(new DragonObserver());
            z.setPamPath("768/FULL/ZOMBIE/ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM");
            return z;
        }

        // ====== FALLBACK ======
        return new Zombie(type, type, row, 190, 100, -92.5f, 100, 32, 95);
    }
}

package models.factory.builder;

import controllers.datacontroller.Data;
import models.App;
import models.entity.PlantCategory;
import models.entity.PlantTags;
import models.factory.plantSkills.*;
import models.factory.plantSkills.skillDatas.*;
import models.entity.ProjectileType;
import models.entity.Plant;

public enum PlantType {

    /// ---------Sun Producers------
    SUNFLOWER{
        @Override
        public Plant allocateSkill(Plant plant) {
            SunProduceData data = new SunProduceData(25);
            plant.getPlantfoodSkill().add(new SunProduce(data));
            return super.allocateSkill(plant);
        }
    },
    TWIN_SUNFLOWER{
        @Override
        public Plant allocateSkill(Plant plant) {
            SunProduceData data = new SunProduceData(50);
            plant.getPlantfoodSkill().add(new SunProduce(data));
            return super.allocateSkill(plant);
        }
    },
    SUN_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            SunProduceData data = new SunProduceData(15);
            plant.getPlantfoodSkill().add(new SunProduce(data));
            return super.allocateSkill(plant);
        }
    },
    PRIMAL_SUNFLOWER{
        @Override
        public Plant allocateSkill(Plant plant) {
            SunProduceData data = new SunProduceData(75);
            plant.getPlantfoodSkill().add(new SunProduce(data));
            return super.allocateSkill(plant);
        }
    },
    GOLD_BLOOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            SunProduceData data = new SunProduceData(375 , true);
            plant.getPlantfoodSkill().add(new SunProduce(data));
            return super.allocateSkill(plant);
        }
    },
    /// ---------SHOOTERS----------
    PEASHOOTER{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 30);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    REPEATER{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 2);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 52);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    THREEPEATER{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.ThreeLine , 3);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.AllLines , 150);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    SNOW_PEA{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            plant.getPlantfoodSkill().add(new Freeze(Freeze.Type.LINE));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 40);
            plant.getPlantfoodSkill().add(new Freeze(Freeze.Type.LINE));
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    ROTOBAGA{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.Diagonal , 4);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.Diagonal , 120);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    PEA_POD{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            return super.allocateSkill(plant);
        }
    },
    SPLIT_PEA{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.Front_Back , 3);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.Front_Back , 120);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    CITRON{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.HEAVY_BULLET , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PLASMA , ShootingMood.OneLine , 1);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    BOWLING_BULB{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.ONION_1 , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.ONION_1 , ShootingMood.OneLine , 3);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    STARFRUIT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.STAR , ShootingMood.Star , 5);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.STAR , ShootingMood.Star , 150);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    FIRE_PEASHOOTER{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 40);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return  super.allocateSkill(plant);
        }
    },
    GOO_PEASHOOTER{

        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 40);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    MEGA_GATLING_PEA{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 4);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf1 = new ShootingData(ProjectileType.PEA , ShootingMood.OneLine , 120);
            ShootingData pf2 = new ShootingData(ProjectileType.GIANT_PEA , ShootingMood.OneLine , 4);
            plant.getPlantfoodSkill().add(new Shoot(pf1));
            plant.getPlantfoodSkill().add(new Shoot(pf2));
            return  super.allocateSkill(plant);
        }
    },
    SEA_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.BUBBLE ,
                ShootingMood.MID_RANGE , 1);
            data.range = PlantLevel.current(plant.getType())
                == 4 ? 4 : 3;
            ShootingData pf = new ShootingData(ProjectileType.BUBBLE , ShootingMood.MID_RANGE , 30);
            ExtraHP hp = new ExtraHP(ExtraHP.Type.LIFE_RESET);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            plant.getPlantfoodSkill().add(hp);
            return  super.allocateSkill(plant);
        }
    },
    PUFF_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.BUBBLE ,
                ShootingMood.MID_RANGE , 1);
            data.range = PlantLevel.current(plant.getType())
                >= 2 ? 4 : 3;
            ShootingData pf = new ShootingData(ProjectileType.BUBBLE , ShootingMood.MID_RANGE , 30);
            ExtraHP hp = new ExtraHP(ExtraHP.Type.LIFE_RESET);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            plant.getPlantfoodSkill().add(hp);
            return   super.allocateSkill(plant);
        }
    },
    /// -----------EXPLOSIVES--------------
    POTATO_MINE{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.TOUCH);
            plant.getBaseSkill().add(new Explosive(data));
            ExtraHP clone = new ExtraHP(ExtraHP.Type.CLONE);
            clone.cloneNumber = 2;
            plant.getPlantfoodSkill().add(clone);
            return  super.allocateSkill(plant);
        }
    },
    PRIMAL_POTATO_MINE{
        @Override
        public Plant allocateSkill(Plant plant) {
            // Trap plants trigger this area explosion when a zombie gets close.
            ExplosionData data = new ExplosionData(3, 3);
            plant.getBaseSkill().add(new Explosive(data));
            ExtraHP clone = new ExtraHP(ExtraHP.Type.CLONE);
            clone.cloneNumber = 2;
            plant.getPlantfoodSkill().add(clone);
            return  super.allocateSkill(plant);
        }
    },
    CHERRY_BOMB{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(3,3);
            plant.getBaseSkill().add(new Explosive(data));
            return  super.allocateSkill(plant);
        }
    },
    SQUASH{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.NEXT_TO);
            plant.getBaseSkill().add(new Explosive(data));
            ExplosionData pf = new ExplosionData(2);
            Explosive e = new Explosive(pf);
            e.setRandom(true);
            plant.getPlantfoodSkill().add(e);
            return  super.allocateSkill(plant);
        }
    },
    GRAPESHOT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(3,3);
            // Clone behaviour is represented by ExtraHP until dedicated clone entities exist.
            plant.getBaseSkill().add(new Explosive(data));
            return  super.allocateSkill(plant);

        }
    },
    JALAPENO{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.LINE);
            plant.getBaseSkill().add(new Explosive(data));
            return  super.allocateSkill(plant);
        }
    },
    DOOM_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.ALL);
            plant.getBaseSkill().add(new Explosive(data));
            return  super.allocateSkill(plant);
        }
    },
    TANGLE_KELP{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.NEXT_TO);
            data.randomCount = PlantLevel.current(plant.getType()) >= 3 ? 2 : 1;
            plant.getBaseSkill().add(new Explosive(data));
            ExplosionData pf = new ExplosionData(2);
            Explosive e = new Explosive(pf);
            e.setRandom(true);
            plant.getPlantfoodSkill().add(e);
            return super.allocateSkill(plant);
        }
    },
    ICEBERG_LETTUCE{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Freeze(Freeze.Type.TOUCH));
            plant.getPlantfoodSkill().add(new Freeze(Freeze.Type.ALL));
            return  super.allocateSkill(plant);
        }
    },

    ICE_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Freeze(Freeze.Type.ALL));
            return   super.allocateSkill(plant);
        }
    },
    HOT_POTATO{

    },
    GRAVE_BUSTER{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Modify(Modify.Type.GRAVE_EATER));
            return  super.allocateSkill(plant);
        }
    },
    /// ---------LOBBERS------------------
    CABBAGE_PULT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.CABBAGE , ShootingMood.LOBBER , 1);
            plant.getBaseSkill().add(new Shoot(data));

            return super.allocateSkill(plant);
        }
    },
    KERNEL_PULT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.CORN , ShootingMood.LOBBER , 1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.BUTTER , ShootingMood.LOBBER , 10);
            Shoot all = new Shoot(pf);
            all.setAll(true);
            plant.getPlantfoodSkill().add(all);
            return super.allocateSkill(plant);
        }
    },
    MELON_PULT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.MELON , ShootingMood.LOBBER , 1);
            plant.getBaseSkill().add(new Shoot(data));
            return  super.allocateSkill(plant);
        }
    },
    WINTER_MELON{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.MELON , ShootingMood.LOBBER , 1);
            plant.getBaseSkill().add(new Shoot(data));
            return  super.allocateSkill(plant);
        }
    },
    PEPPER_PULT{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.PEPPER, ShootingMood.LOBBER , 1);
            plant.getBaseSkill().add(new Shoot(data));
            return  super.allocateSkill(plant);
        }
    },
    /// -----------STRIKE_THROUGH--------
    CACTUS{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.CACTUS , ShootingMood.OneLine,
                1);
            plant.getBaseSkill().add(new Shoot(data));
            ShootingData pf = new ShootingData(ProjectileType.ELECTRICAL_CACTUS , ShootingMood.OneLine,
                1);
            plant.getPlantfoodSkill().add(new Shoot(pf));
            return super.allocateSkill(plant);
        }
    },
    FUM_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            ShootingData data = new ShootingData(ProjectileType.BUBBLE , ShootingMood.MID_RANGE , 1);
            int level = PlantLevel.current(plant.getType());
            data.range = level >= 2 ? 5 : 4;
            plant.getBaseSkill().add(new Shoot(data));
            plant.getPlantfoodSkill().add(new Modify());
            return super.allocateSkill(plant);
        }
    },
    /// -----------MELEE---------------
    BONK_CHOY{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Melee(Melee.MeleeAttack.PUNCH));
            plant.getPlantfoodSkill().add(new Melee(Melee.MeleeAttack.AoE , 1 , 1));
            return super.allocateSkill(plant);
        }
    },
    PHAT_BEET{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(3 , 3);
            plant.getBaseSkill().add(new Explosive(data));
            ExplosionData pf = new ExplosionData(ExplosionData.ExplosionType.ALL);
            plant.getPlantfoodSkill().add(new Explosive(pf));
            return super.allocateSkill(plant);
        }
    },
    CHOMPER{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(ExplosionData.ExplosionType.NEXT_TO);
            plant.getBaseSkill().add(new Explosive(data));
            ExplosionData pf = new ExplosionData(ExplosionData.ExplosionType.RANDOM);
            pf.randomCount = 3;
            plant.getPlantfoodSkill().add(new Explosive(pf));
            return super.allocateSkill(plant);
        }
    },
    WASABI_WHIP{
        @Override
        public Plant allocateSkill(Plant plant) {
            int level = PlantLevel.current(plant.getType());
            int range = level >= 3 ? 2 : 1;
            plant.getBaseSkill().add(new Melee(Melee.MeleeAttack.PUNCH).setRange(range));
            ExplosionData pf = new ExplosionData(ExplosionData.ExplosionType.RANDOM);
            pf.randomCount = 3;
            plant.getPlantfoodSkill().add(new Explosive(pf));
            /// TO DO: fire effecth
            return super.allocateSkill(plant);
        }
    },
    KIWIBEAST{
        @Override
        public Plant allocateSkill(Plant plant) {
            ExplosionData data = new ExplosionData(3 , 3);
            plant.getBaseSkill().add(new Explosive(data));
            ExplosionData data1 = new ExplosionData(9 , 9);
            plant.getPlantfoodSkill().add(new Explosive(data1));
            return super.allocateSkill(plant);
        }
    },
    /// ----------WALL_NUTS------------
    WALL_NUT{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getPlantfoodSkill().add(new ExtraHP(ExtraHP.Type.ARMOR , 4000));
            return super.allocateSkill(plant);
        }
    },
    TALL_NUT{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block();
            plant.getBaseSkill().add(skill);
            plant.getPlantfoodSkill().add(new  ExtraHP(ExtraHP.Type.ARMOR , 8000));
            return super.allocateSkill(plant);
        }
    },
    ENDURIAN{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block();
            //skill.damage = true;
            plant.getBaseSkill().add(skill);
            return   super.allocateSkill(plant);
        }
    },
    GARLIC{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block();
            plant.getBaseSkill().add(skill);
            return   super.allocateSkill(plant);
        }
    },
    SWEET_POTATO{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block(true);
            plant.getBaseSkill().add(skill);
            return   super.allocateSkill(plant);
        }
    },
    EXPLODE_O_NUT{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block();
            plant.getBaseSkill().add(skill);
            ExtraHP pf = new ExtraHP(ExtraHP.Type.ARMOR , 6000);
            pf.explosive = true;
            plant.getPlantfoodSkill().add(pf);
            return   super.allocateSkill(plant);
        }
    },
    PUMPKIN{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new  ExtraHP(ExtraHP.Type.ARMOR , plant.getHp()));
            return super.allocateSkill(plant);
        }
    },
    SUN_BEAN{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill = new Block();
            plant.getBaseSkill().add(skill);
            plant.getPlantfoodSkill().add(new ExtraHP(ExtraHP.Type.ARMOR , 10000));
            return super.allocateSkill(plant);
        }
    },
    /// --------MODIFIERS-----------
    TORCHWOOD{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Modify());
            return super.allocateSkill(plant);
        }
    },
    HYPNO_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {

            return super.allocateSkill(plant);
        }
    },
    IMITATER,
    LILY_PAD{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new  Modify());
            return super.allocateSkill(plant);
        }
    },
    /// --------HOMING--------------
    CAULIPOWER{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill data = new Homing(ProjectileType.MAGIC , Homing.Type.RANDOM);
            plant.getBaseSkill().add(data);

            return super.allocateSkill(plant);
        }
    },
    ELECTRIC_BLUEBERRY{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new Homing(ProjectileType.LIGHTNING , Homing.Type.RANDOM));
            Homing pf = new Homing(ProjectileType.MAGIC , Homing.Type.RANDOM);
            pf.targetCount = 3;
            plant.getPlantfoodSkill().add(pf);
            return super.allocateSkill(plant);
        }
    },
    CAT_TAIL{
        @Override
        public Plant allocateSkill(Plant plant) {
            Skill skill  = new Homing(ProjectileType.MAGIC , Homing.Type.CLOSEST);
            plant.getBaseSkill().add(skill);
            Homing pf =  new Homing(ProjectileType.MAGIC , Homing.Type.RANDOM);
            pf.targetCount = 30;
            plant.getPlantfoodSkill().add(pf);
            return super.allocateSkill(plant);
        }
    },
    MAGNET_SHROOM{
        @Override
        public Plant allocateSkill(Plant plant) {
            plant.getBaseSkill().add(new  Modify());
            plant.getPlantfoodSkill().add(new Modify());
            return super.allocateSkill(plant);
        }
    },


    ///MINTS
    ENLIGHTEN_MINT,
    APPEASE_MINT,
    ARMA_MINT,
    BOMBARD_MINT,
    ENFORCE_MINT,
    REINFORCE_MINT,
    ENCHANT_MINT,
    PIERCE_MINT,
    CATTAIL_MINT,
    MARIGOLD;


    public Plant allocateSkill(Plant plant){
        for (int i = 2; i <= PlantLevel.current(plant.getType()); i++) {
            if(Data.getPlants().get(plant.getType())
                .getUpgrades().get(i - 1).getEffect().equals("AoE on Death")) plant
                .getTags().add(PlantTags.EXPLOSIVE);
        }
        return plant;
    }
    public PlantCategory getCategory() {

        switch (this) {

            // -------- Sun Producers --------
            case SUNFLOWER:
            case TWIN_SUNFLOWER:
            case SUN_SHROOM:
            case PRIMAL_SUNFLOWER:
            case GOLD_BLOOM:
                return PlantCategory.SunProducer;


            // -------- Shooters --------
            case PEASHOOTER:
            case REPEATER:
            case THREEPEATER:
            case SNOW_PEA:
            case ROTOBAGA:
            case PEA_POD:
            case SPLIT_PEA:
            case CITRON:
            case BOWLING_BULB:
            case STARFRUIT:
            case FIRE_PEASHOOTER:
            case GOO_PEASHOOTER:
            case MEGA_GATLING_PEA:
            case SEA_SHROOM:
            case PUFF_SHROOM:
                return PlantCategory.SHOOTER;


            // -------- Explosives --------
            case POTATO_MINE:
            case PRIMAL_POTATO_MINE:
            case CHERRY_BOMB:
            case SQUASH:
            case GRAPESHOT:
            case JALAPENO:
            case DOOM_SHROOM:
            case TANGLE_KELP:
            case ICEBERG_LETTUCE:
            case ICE_SHROOM:
            case HOT_POTATO:
            case GRAVE_BUSTER:
            case PHAT_BEET:
            case CHOMPER:
            case WASABI_WHIP:
            case KIWIBEAST:
                return PlantCategory.Explosive;


            // -------- Lobber / Strike Through --------
            case CACTUS:
            case FUM_SHROOM:
            case CABBAGE_PULT:
            case KERNEL_PULT:
            case MELON_PULT:
            case WINTER_MELON:
            case PEPPER_PULT:
                return PlantCategory.StrikeThrough;


            // -------- Melee (فعلا نزدیک‌ترین دسته موجود) --------
            case BONK_CHOY:
                return PlantCategory.StrikeThrough;


            // -------- Modifier --------
            case TORCHWOOD:
            case HYPNO_SHROOM:
            case IMITATER:
            case LILY_PAD:
            case MAGNET_SHROOM:
                return PlantCategory.Mint;


            // -------- Homing --------
            case CAULIPOWER:
            case ELECTRIC_BLUEBERRY:
            case CAT_TAIL:
                return PlantCategory.SHOOTER;


            // -------- Wall plants --------
            case WALL_NUT:
            case TALL_NUT:
            case ENDURIAN:
            case GARLIC:
            case SWEET_POTATO:
            case EXPLODE_O_NUT:
            case PUMPKIN:
            case SUN_BEAN:
                return PlantCategory.StrikeThrough;


            // -------- Mints --------
            case ENLIGHTEN_MINT:
            case APPEASE_MINT:
            case ARMA_MINT:
            case BOMBARD_MINT:
            case ENFORCE_MINT:
            case REINFORCE_MINT:
            case ENCHANT_MINT:
            case PIERCE_MINT:
            case CATTAIL_MINT:
            case MARIGOLD:
                return PlantCategory.Mint;


            default:
                return null;
        }
    }
}

package models.factory.plantSkills;

import models.App;
import models.Constants;
import models.entity.*;
import models.factory.builder.PlantType;
import models.factory.plantSkills.skillDatas.ShootingData;
import models.factory.plantSkills.skillDatas.ShootingMood;
import models.gamepanes.Tile;
import models.games.BaseGame;

import java.util.ArrayList;
import java.util.Random;


public class Shoot implements Skill {
    ShootingData data;
    boolean random = false;
    boolean all = false;

    public Shoot(ShootingData data){
        this.data = data;
    }
    @Override
    public void do_skill(Plant shooter , BaseGame game) {

        try {
            shoot(shooter , data, game);
        }catch (Exception e){
            System.out.println("man kharam");
        }
    }



    private float MOUTH_EXITPOINT = 0.65f;

    public void shoot(Plant shooter, ShootingData data, BaseGame game)
        throws CloneNotSupportedException {

        System.out.println("bangg bangg bangg .. " + shooter.getType() + " is shooting ..");

        int firstNewProjectile = game.getBullets().size();

        switch (data.getMood()) {
            case OneLine -> oneLineShoot(shooter, data, game);
            case ThreeLine -> threeLineShoot(shooter, data, game);
            case Front_Back -> frontBackShoot(shooter, data, game);
            case Star -> starShoot(shooter, data, game);
            case Diagonal -> diagonal(shooter, data, game);
            case LOBBER -> lobber(shooter, game);
            case Random -> random(shooter, game, data.getRandomCount());
            case AllLines -> all(shooter, game);
            case MID_RANGE -> { /* handled by data.range below */ }
        }

        if (data.range > 0) {
            midRange(shooter, game);
        } else if (random) {
            random(shooter, game, data.getRandomCount());
        } else if (all) {
            all(shooter, game);
        }

        // Every projectile created by this shot inherits its shooter's real gameplay/visual metadata.
        for (int i = firstNewProjectile; i < game.getBullets().size(); i++) {
            Projectile projectile = game.getBullets().get(i);
            projectile.setDamage(shooter.getDamage());
            projectile.setTags(shooter.getTags());
            projectile.setSourcePlantType(shooter.getType());
            // Snow Pea must remain an ice attack even when a hand-built plant
            // or an older save omitted the Ice data tag.
            if (shooter.getType() == PlantType.SNOW_PEA
                && !projectile.getTags().contains(Projectile.Tag.ICE)) {
                projectile.getTags().add(Projectile.Tag.ICE);
            }
        }
    }


    float onionChange;
    public void oneLineShoot(Plant shooter, ShootingData data , BaseGame game) throws CloneNotSupportedException {
        onionChange += 2;
        int level = App.getCurrentuser().getLevels().get(shooter.getType());
        if(data.getBullet() == ProjectileType.ONION_1 || data.getBullet() == ProjectileType.ONION_3){
            if(onionChange >= (level >= 2 ? 9 : 10)) {
                data.setBullet(ProjectileType.ONION_2);
                onionChange = 0;
            }
        }
        else if(data.getBullet() == ProjectileType.ONION_2){
            if (onionChange >= (level >= 2 ? 4 : 5)){
                Random rand = new Random();
                boolean one =  rand.nextBoolean();
                if(one){
                    data.setBullet(ProjectileType.ONION_1);
                }
                else{
                    data.setBullet(ProjectileType.ONION_3);
                }
                onionChange = 0;
            }
        }
        float x = shooter.getX() + shooter.getWidth();
        float y =  (shooter.getY() + shooter.getHeight() * MOUTH_EXITPOINT);
        Projectile projectile = new Projectile(x , y , data.getBullet(),shooter.getLine());
        projectile.setVelocityX(Constants.BULLET_VELOCITY_X);
        for (int i = 0; i < data.getBulletNumber(); i++) {
            Projectile projectile1 = (Projectile) projectile.clone();
            projectile1.setX(projectile1.getX() + i * 10);
            game.getBullets().add(projectile1);
        }


        if(data.getBulletNumber() >= 50){
            Projectile projectile1 = new Projectile(x , y , Constants.BULLET_VELOCITY_X, ProjectileType.GIANT_PEA
                , shooter.getDamage(),shooter.getLine());
            game.getBullets().add(projectile1);
        }
    }

    public void threeLineShoot(Plant shooter, ShootingData data, BaseGame game)
        throws CloneNotSupportedException {

        Projectile center = new Projectile(
            shooter.getX() + shooter.getWidth(),
            shooter.getY() + shooter.getHeight() * MOUTH_EXITPOINT,
            Constants.BULLET_VELOCITY_X,
            data.getBullet(),
            shooter.getDamage(),
            shooter.getLine()
        );

        game.getBullets().add(center);

        if (shooter.getLine() < 4) {
            Projectile up = (Projectile) center.clone();
            up.setY(up.getY() + Tile.getHeight());
            up.setLine(shooter.getLine() + 1);
            game.getBullets().add(up);
        }

        if (shooter.getLine() > 0) {
            Projectile down = (Projectile) center.clone();
            down.setY(down.getY() - Tile.getHeight());
            down.setLine(shooter.getLine() - 1);
            game.getBullets().add(down);
        }
    }

    private void frontBackShoot(Plant shooter , ShootingData data , BaseGame game) throws CloneNotSupportedException {
        ShootingData front = new ShootingData(data.getBullet() , data.getMood() ,
            data.getBulletNumber() / 2);
        oneLineShoot(shooter , front , game);
        Projectile projectileBack = new Projectile(shooter.getX() , shooter.getY() + shooter.getHeight() * MOUTH_EXITPOINT
            ,Constants.BULLET_VELOCITY_X * -1
            , data.getBullet(), shooter.getDamage() ,  shooter.getLine());
        for (int i = 0; i < data.getBulletNumber() / 2; i++) {
            Projectile b = (Projectile) projectileBack.clone();
            b.setX(b.getX() - i * 4);
            game.getBullets().add(b);
        }
    }

    private void starShoot(Plant shooter , ShootingData data , BaseGame game) throws CloneNotSupportedException {
        oneLineShoot(shooter, new ShootingData(data.getBullet() , data.getMood()
            , data.getBulletNumber() / 5), game);///right
        Projectile projectileStar2 = new Projectile(shooter.getX() , shooter.getY() + shooter.getHeight() * MOUTH_EXITPOINT ,
            Constants.BULLET_VELOCITY_X * -1 , data.getBullet() , shooter.getDamage()
            ,  shooter.getLine());
        Projectile projectileStar3 = (Projectile) projectileStar2.clone();
        projectileStar3.setVelocityY(projectileStar3.getVelocityX());
        projectileStar3.setY(shooter.getY());
        Projectile projectileStar4 = (Projectile) projectileStar3.clone();
        projectileStar4.setVelocityX(projectileStar4.getVelocityX() * -1);
        projectileStar4.setX(shooter.getX() +  shooter.getWidth());
        Projectile projectileStar5 = (Projectile) projectileStar4.clone();
        projectileStar5.setVelocityY(projectileStar5.getVelocityY() * -1);
        projectileStar5.setVelocityX(0);
        projectileStar5.setPosition(shooter.getX() + shooter.getWidth() / 2 , shooter.getY() + shooter.getHeight());
        for (int i = 0; i < data.getBulletNumber() / 5; i++) {
            game.getBullets().add((Projectile) projectileStar5.clone());/// up
            game.getBullets().add((Projectile) projectileStar2.clone());/// left
            game.getBullets().add((Projectile) projectileStar3.clone());/// bottom left
            game.getBullets().add((Projectile) projectileStar4.clone());/// bottom right
        }

    }


    private void diagonal(Plant shooter , ShootingData  data , BaseGame game) throws CloneNotSupportedException {
        Projectile projectile1 = new Projectile(shooter.getX() + shooter.getWidth() , shooter.getY() + shooter.getHeight() * 0.9f ,
            Constants.BULLET_VELOCITY_X, data.getBullet() , shooter.getDamage()
            ,   shooter.getLine());
        projectile1.setVelocityY(projectile1.getVelocityX());
        Projectile projectile2 = (Projectile) projectile1.clone();
        projectile2.setVelocityX(projectile2.getVelocityX() * -1);
        projectile2.setX(shooter.getX());
        Projectile projectile3 = (Projectile) projectile2.clone();
        projectile3.setVelocityY(projectile3.getVelocityY() * -1);
        projectile3.setY(shooter.getY());
        Projectile projectile4 = (Projectile) projectile3.clone();
        projectile4.setX(shooter.getX() + shooter.getWidth());
        projectile4.setVelocityX(Constants.BULLET_VELOCITY_X);
        for (int i = 0; i < data.getBulletNumber() / 4; i++) {
            game.getBullets().add((Projectile) projectile4.clone());
            game.getBullets().add((Projectile) projectile3.clone());
            game.getBullets().add((Projectile) projectile2.clone());
            game.getBullets().add((Projectile) projectile1.clone());
        }
    }


    private void midRange(Plant shooter, BaseGame game) {
        if (shooter.getCategory() == PlantCategory.StrikeThrough) {
            for (Zombie zombie : game.getZombies()) {
                int tileDistance = zombie.getTileIndex() - shooter.getTileIndex();
                if (zombie.getLine() == shooter.getLine()
                    && tileDistance >= 0
                    && tileDistance <= data.range) {
                    zombie.setHp(zombie.getHp() - shooter.getDamage());
                }
            }
            return;
        }

        for (Zombie zombie : game.getZombies()) {
            int tileDistance = zombie.getTileIndex() - shooter.getTileIndex();
            if (zombie.getLine() == shooter.getLine()
                && tileDistance >= 0
                && tileDistance <= data.range) {

                Projectile projectile = new Projectile(
                    shooter.getX() + shooter.getWidth(),
                    shooter.getY() + shooter.getHeight() * MOUTH_EXITPOINT,
                    Constants.BULLET_VELOCITY_X,
                    data.getBullet(),
                    shooter.getDamage(),
                    shooter.getLine()
                );

                game.getBullets().add(projectile);
                return;
            }
        }
    }

    private void lobber(Plant shooter , BaseGame game){
        Zombie target = null;
        for (Zombie z : game.getZombies()) {
            if(z.getLine() == shooter.getLine()){
                if(target == null){
                    target = z;
                }
                else if(target.getTileIndex() > z.getTileIndex()){
                    target = z;
                }
            }
        }

        if(target != null) game.getBullets().add(lobberShoot(shooter , target));
    }
    private Projectile lobberShoot(Plant shooter , Zombie target){
        // Lobbers launch from the plant's mouth and follow an actual arc.  The
        // old calculation divided by (bullet speed + zombie speed); zombie
        // speeds are negative in the normal game, so that could produce a
        // negative flight time and immediately drive the projectile into the
        // ground.
        float startX = shooter.getX() + shooter.getWidth() * 0.75f;
        float startY = shooter.getY() + shooter.getHeight() * 0.70f;
        Projectile projectile = new Projectile(startX, startY
            , data.getBullet(),shooter.getLine());
        if(data.getBullet() == ProjectileType.CORN){
            Random rand = new Random();
            boolean changeIncrease = App.getCurrentuser().getLevels().get(shooter.getType()) >= 2;
            int a = rand.nextInt(100); // probability = 20%
            if((a >= 1 && a <= 40) || (changeIncrease && a >= 41 && a <= 45)) projectile.setType(ProjectileType.BUTTER);
        }
        projectile.setVelocityX(Constants.LOBBER_BULLET_VELOCITY_X);
        float targetVelocityX = target.getVelocityX();
        if (Math.abs(targetVelocityX) < 0.001f) {
            targetVelocityX = target.getSpeed();
        }

        float horizontalDistance = target.getX() - startX;
        if (horizontalDistance < 1f) {
            horizontalDistance = Tile.getWidth();
        }

        // Intercept the zombie's current horizontal path.  This remains
        // positive for the game's right-to-left zombie speeds.
        float relativeSpeed = projectile.getVelocityX() - targetVelocityX;
        float t = horizontalDistance / Math.max(1f, relativeSpeed);
        t = Math.max(0.25f, t);
        float dy = target.getY() - startY;
        float vy = dy / t + Constants.GRAVITY * t / 2f;
        projectile.setVelocityY(vy);
        projectile.setGrounded(false);
        projectile.setIgnoresObstacles(true);
        return projectile;
    }



    @Override
    public ArrayList<Zombie> random(Plant plant, BaseGame game, int numbers) {
        ArrayList<Zombie> targets = Skill.super.random(plant, game, data.getRandomCount());
        if(data.getMood() == ShootingMood.LOBBER){
            for (Zombie z : targets) {
                game.getBullets().add(lobberShoot(plant, z));
            }
        }
        return targets;
    }

    @Override
    public void all(Plant plant, BaseGame game) {
        if (data.getMood() == ShootingMood.LOBBER) {
            for (Zombie zombie : game.getZombies()) {
                game.getBullets().add(lobberShoot(plant, zombie));
            }
            return;
        }

        if (data.getMood() == ShootingMood.AllLines) {
            int shotsPerLine = Math.max(1, data.getBulletNumber() / 5);

            for (int shot = 0; shot < shotsPerLine; shot++) {
                for (int line = 0; line < 5; line++) {
                    Projectile projectile = new Projectile(
                        plant.getX() + plant.getWidth() + shot * 10f,
                        line * Tile.getHeight() + plant.getHeight() * MOUTH_EXITPOINT,
                        Constants.BULLET_VELOCITY_X,
                        data.getBullet(),
                        plant.getDamage(),
                        line
                    );
                    game.getBullets().add(projectile);
                }
            }
        }
    }

    @Override
    public void setRandom(boolean random) {
        this.random = random;
    }

    @Override
    public void setAll(boolean all) {
        this.all = all;
    }
    }

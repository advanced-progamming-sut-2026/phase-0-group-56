package models.factory.plantSkills.skillDatas;

import models.entity.ProjectileType;

public class ShootingData {
    private ProjectileType bullet; /// type of your bullet
    private ShootingMood mood; /// how you wanna shoot
    private int bulletNumber; /// how many projectiles you wanna shoot each time
    private int randomCount; /// random zombies to shoot
    public int range; /// range for mid - ranged plants

    public ShootingData(ProjectileType type , ShootingMood mood , int bulletNumber) {
        this.bulletNumber = bulletNumber;
        this.mood = mood;
        bullet = type;
    }

    public ShootingData(){

    }
    public ProjectileType getBullet() {
        return bullet;
    }

    public void setBullet(ProjectileType bullet) {
        this.bullet = bullet;
    }

    public ShootingMood getMood() {
        return mood;
    }

    public void setMood(ShootingMood mood) {
        this.mood = mood;
    }

    public int getBulletNumber() {
        return bulletNumber;
    }

    public void setBulletNumber(int bulletNumber) {
        this.bulletNumber = bulletNumber;
    }

    public int getRandomCount() {
        return randomCount;
    }

    public void setRandomCount(int randomCount) {
        this.randomCount = randomCount;
    }
}

package models.gamepanes;

import models.entity.Zombie;
import models.factory.ZombieFactory;

import java.util.ArrayList;
import java.util.Random;

public class Wave {
    private ArrayList<Zombie> zombies = new ArrayList<>();
    private int zombieCount = 0;
    private float zombiesHP;
    private int id;
    private int hardness;
    private float cost;
    boolean finalWave;
    public Wave(){}
    public Wave(int id, int hardness, float cost){
        this.id = id;
        this.hardness = hardness;
    }

    Random rand = new Random();
    public void initWave(ArrayList<String> available){
        if(cost <= 0) return;
        int index = rand.nextInt(available.size());
        zombies.add(ZombieFactory.createZombie(available.get(index)));
        zombieCount++;
        zombiesHP += zombies.getLast().getHp();
        cost -= zombies.getLast().getCost();
        initWave(available);
    }

    public boolean isFinished(){
        float totalHp = 0;
        for (Zombie z : zombies) {
            if (z != null && !z.isDead()) {
                totalHp += Math.max(0f, z.getHp());
            }
        }
        /* A wave is finished only after every zombie in it has died.  The
         * previous 25% threshold advanced normal waves while live zombies
         * were still on the lawn and could consequently announce a win early.
         */
        return totalHp <= 0.1f;
    }


    public void setFinalWave(boolean finalWave) {
        this.finalWave = finalWave;
    }

    public void setZombies(ArrayList<Zombie> zombies) {
        this.zombies = zombies;
    }


    public int getZombieCount() {
        return zombieCount;
    }

    public void setZombieCount(int zombieCount) {
        this.zombieCount = zombieCount;
    }

    public float getZombiesHP() {
        return zombiesHP;
    }

    public void setZombiesHP(float zombiesHP) {
        this.zombiesHP = zombiesHP;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHardness() {
        return hardness;
    }

    public void setHardness(int hardness) {
        this.hardness = hardness;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }

    public Random getRand() {
        return rand;
    }

    public void setRand(Random rand) {
        this.rand = rand;
    }

    public ArrayList<Zombie> getZombies() {
        return zombies;
    }
}

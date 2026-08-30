package network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable, wire-friendly snapshot of a network I, Zombie match. */
public record IZombieNetworkState(
    String matchId,
    long revision,
    Phase phase,
    Role winner,
    long remainingMillis,
    int plantSun,
    int zombieSun,
    int brainsEaten,
    int plantScore,
    int zombieScore,
    boolean[] eatenBrains,
    List<Unit> plants,
    List<Unit> zombies,
    List<Projectile> projectiles
) {
    public IZombieNetworkState {
        matchId = matchId == null ? "" : matchId;
        phase = phase == null ? Phase.WAITING : phase;
        eatenBrains = normalizeBrains(eatenBrains);
        plants = immutableCopy(plants);
        zombies = immutableCopy(zombies);
        projectiles = projectiles == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(projectiles));
    }

    @Override
    public boolean[] eatenBrains() {
        return Arrays.copyOf(eatenBrains, eatenBrains.length);
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null
            ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static boolean[] normalizeBrains(boolean[] values) {
        return values == null ? new boolean[5] : Arrays.copyOf(values, 5);
    }

    public enum Phase {
        WAITING,
        PLAYING,
        PLANTS_WON,
        ZOMBIES_WON,
        ABORTED
    }

    public enum Role {
        PLANTS,
        ZOMBIES
    }

    public record Unit(
        long id,
        String type,
        int row,
        float x,
        float y,
        float hp,
        float maxHp
    ) {
        public Unit {
            type = type == null ? "" : type;
        }
    }

    public record Projectile(
        long id,
        int row,
        float x,
        float y,
        float damage
    ) {
    }
}

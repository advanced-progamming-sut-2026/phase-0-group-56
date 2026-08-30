package network;

import java.util.ArrayList;
import java.util.List;

/** Compact field codec used by the line-oriented network protocol. */
public final class IZombieNetworkStateCodec {
    private IZombieNetworkStateCodec() {
    }

    public static String[] encode(IZombieNetworkState state) {
        if (state == null) {
            return new String[0];
        }
        List<String> fields = new ArrayList<>();
        fields.add(state.matchId());
        fields.add(Long.toString(state.revision()));
        fields.add(state.phase().name());
        fields.add(state.winner() == null ? "" : state.winner().name());
        fields.add(Long.toString(state.remainingMillis()));
        fields.add(Integer.toString(state.plantSun()));
        fields.add(Integer.toString(state.zombieSun()));
        fields.add(Integer.toString(state.brainsEaten()));
        fields.add(Integer.toString(state.plantScore()));
        fields.add(Integer.toString(state.zombieScore()));
        StringBuilder brains = new StringBuilder();
        for (boolean eaten : state.eatenBrains()) {
            brains.append(eaten ? '1' : '0');
        }
        fields.add(brains.toString());
        addUnits(fields, state.plants());
        addUnits(fields, state.zombies());
        fields.add(Integer.toString(state.projectiles().size()));
        for (IZombieNetworkState.Projectile projectile : state.projectiles()) {
            fields.add(Long.toString(projectile.id()));
            fields.add(Integer.toString(projectile.row()));
            fields.add(Float.toString(projectile.x()));
            fields.add(Float.toString(projectile.y()));
            fields.add(Float.toString(projectile.damage()));
        }
        return fields.toArray(String[]::new);
    }

    public static IZombieNetworkState decode(String[] data) {
        if (data == null || data.length < 14) {
            return null;
        }
        try {
            int index = 0;
            String matchId = data[index++];
            long revision = Long.parseLong(data[index++]);
            IZombieNetworkState.Phase phase = IZombieNetworkState.Phase.valueOf(data[index++]);
            String winnerValue = data[index++];
            IZombieNetworkState.Role winner = winnerValue.isBlank()
                ? null : IZombieNetworkState.Role.valueOf(winnerValue);
            long remaining = Long.parseLong(data[index++]);
            int plantSun = Integer.parseInt(data[index++]);
            int zombieSun = Integer.parseInt(data[index++]);
            int brains = Integer.parseInt(data[index++]);
            int plantScore = Integer.parseInt(data[index++]);
            int zombieScore = Integer.parseInt(data[index++]);
            boolean[] eaten = decodeBrains(data[index++]);
            UnitResult plants = readUnits(data, index);
            index = plants.nextIndex;
            UnitResult zombies = readUnits(data, index);
            index = zombies.nextIndex;
            int projectileCount = readInt(data, index++);
            if (projectileCount < 0 || projectileCount > 1000) {
                return null;
            }
            List<IZombieNetworkState.Projectile> projectiles = new ArrayList<>();
            for (int i = 0; i < projectileCount && index + 4 < data.length; i++) {
                projectiles.add(new IZombieNetworkState.Projectile(
                    Long.parseLong(data[index++]), Integer.parseInt(data[index++]),
                    Float.parseFloat(data[index++]), Float.parseFloat(data[index++]),
                    Float.parseFloat(data[index++])));
            }
            return new IZombieNetworkState(matchId, revision, phase, winner, remaining,
                plantSun, zombieSun, brains, plantScore, zombieScore, eaten,
                plants.units, zombies.units, projectiles);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void addUnits(List<String> fields, List<IZombieNetworkState.Unit> units) {
        fields.add(Integer.toString(units.size()));
        for (IZombieNetworkState.Unit unit : units) {
            fields.add(Long.toString(unit.id()));
            fields.add(unit.type());
            fields.add(Integer.toString(unit.row()));
            fields.add(Float.toString(unit.x()));
            fields.add(Float.toString(unit.y()));
            fields.add(Float.toString(unit.hp()));
            fields.add(Float.toString(unit.maxHp()));
        }
    }

    private static UnitResult readUnits(String[] data, int start) {
        int index = start;
        int count = readInt(data, index++);
        if (count < 0 || count > 1000) {
            throw new IllegalArgumentException("Invalid entity count.");
        }
        List<IZombieNetworkState.Unit> units = new ArrayList<>();
        for (int i = 0; i < count && index + 6 < data.length; i++) {
            units.add(new IZombieNetworkState.Unit(
                Long.parseLong(data[index++]), data[index++], Integer.parseInt(data[index++]),
                Float.parseFloat(data[index++]), Float.parseFloat(data[index++]),
                Float.parseFloat(data[index++]), Float.parseFloat(data[index++])));
        }
        return new UnitResult(units, index);
    }

    private static boolean[] decodeBrains(String value) {
        boolean[] result = new boolean[IZombieNetworkMatch.ROW_COUNT];
        if (value == null) {
            return result;
        }
        for (int index = 0; index < result.length && index < value.length(); index++) {
            result[index] = value.charAt(index) == '1';
        }
        return result;
    }

    private static int readInt(String[] data, int index) {
        if (index < 0 || index >= data.length) {
            throw new IllegalArgumentException("Missing snapshot field.");
        }
        return Integer.parseInt(data[index]);
    }

    private record UnitResult(List<IZombieNetworkState.Unit> units, int nextIndex) {
    }
}

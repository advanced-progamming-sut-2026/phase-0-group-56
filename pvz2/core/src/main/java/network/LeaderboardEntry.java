package network;

/** Public leaderboard row returned by the network server. */
public record LeaderboardEntry(String username, String nickname, int score) {
}

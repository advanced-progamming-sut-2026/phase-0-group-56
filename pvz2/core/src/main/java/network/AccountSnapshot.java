package network;

/** Public account data sent to a client after registration or login. */
public record AccountSnapshot(
    String username,
    String nickname,
    String email,
    String gender,
    int highestScore
) {
}

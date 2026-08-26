package network;

/** Asynchronous notification delivered by the server. */
public record NetworkEvent(String type, String[] data) {
}

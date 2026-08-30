package network;

/** Optional standalone entry point for hosting a server outside the game client. */
public final class NetworkServerMain {
    private NetworkServerMain() {
    }

    public static void main(String[] arguments) throws Exception {
        int port = NetworkServer.DEFAULT_PORT;
        if (arguments.length > 0) {
            port = Integer.parseInt(arguments[0]);
        }
        NetworkServer server = new NetworkServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        Thread.currentThread().join();
    }
}

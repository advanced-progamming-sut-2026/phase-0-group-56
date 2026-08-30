package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controllers.menus.secondarymenus.Network;
import network.NetworkEvent;
import network.NetworkService;
import view.gameview.IZombieNetworkView;
import models.App;

/** Graphical view for the pre-game network features. */
public class NetworkView extends View {
    private final Network controller;
    private Label statusLabel;
    private Label requestLabel;
    private TextButton acceptButton;
    private TextButton rejectButton;
    private String requestId;
    private boolean lastConnectionState;

    /** MVC constructor: the Network menu/controller is supplied by the caller. */
    public NetworkView(Network controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Network controller cannot be null.");
        }
        this.controller = controller;
        menu = controller;
    }

    /** Convenience overload for existing callers that create screens directly. */
    public NetworkView() {
        this(new Network());
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        table.add(menuSectionHeader(
                "almanac",
                "NETWORK PLAY",
                "Find a random opponent or challenge a specific player."
            ))
            .width(820f)
            .padBottom(15f)
            .row();

        lastConnectionState = NetworkService.isConnected();
        statusLabel = secondaryLabel(connectionText(lastConnectionState));
        table.add(statusLabel).padBottom(10f).row();

        Table connection = new Table();
        TextField address = field("Server address (127.0.0.1)");
        TextField port = field("Port (47856)");
        connection.add(address).width(265f).height(44f).padRight(6f);
        connection.add(port).width(135f).height(44f).padRight(6f);
        connection.add(greenSmallButton("CONNECT", () -> setStatus(
                controller.connectToServer(address.getText().trim(), parsePort(port.getText(), 47856)))))
            .width(140f).height(44f).padRight(6f);
        connection.add(brownButton("HOST", () -> setStatus(
                controller.hostServer(parsePort(port.getText(), 47856)))))
            .width(120f).height(44f);
        table.add(connection).padBottom(14f).row();

        table.add(greenButton(
                "FIND RANDOM OPPONENT",
                () -> setStatus(controller.findRandomOpponent())
            ))
            .width(360f)
            .height(54f)
            .padBottom(12f)
            .row();

        Table challenge = new Table();
        TextField username = field("Opponent username");
        challenge.add(username)
            .width(330f)
            .height(44f)
            .padRight(8f);
        challenge.add(purpleButton(
                "CHALLENGE",
                () -> setStatus(controller.challenge(username.getText().trim()))
            ))
            .width(180f)
            .height(44f);
        table.add(challenge).padBottom(16f).row();

        requestLabel = secondaryLabel("No incoming game requests.");
        table.add(requestLabel).padBottom(8f).row();

        Table decisions = new Table();
        acceptButton = greenSmallButton("ACCEPT", () -> respond(true));
        rejectButton = brownButton("REJECT", () -> respond(false));
        decisions.add(acceptButton).width(150f).height(44f).padRight(8f);
        decisions.add(rejectButton).width(150f).height(44f);
        table.add(decisions).row();
        setRequestVisible(false);
    }

    private String connectionText(boolean connected) {
        return connected ? "CONNECTED TO SERVER" : "NOT CONNECTED TO SERVER";
    }

    private void respond(boolean accepted) {
        if (requestId == null || requestId.isBlank()) {
            setRequestVisible(false);
            return;
        }
        setStatus(controller.respondToChallenge(requestId, accepted));
        requestId = null;
        setRequestVisible(false);
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message == null || message.isBlank()
                ? connectionText(NetworkService.isConnected())
                : message);
        }
    }

    private void setRequestVisible(boolean visible) {
        if (requestLabel != null) {
            requestLabel.setVisible(visible);
        }
        if (acceptButton != null) {
            acceptButton.setVisible(visible);
        }
        if (rejectButton != null) {
            rejectButton.setVisible(visible);
        }
    }

    private int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value == null || value.isBlank() ? Integer.toString(fallback) : value.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void processEvents() {
        boolean connected = NetworkService.isConnected();
        if (connected != lastConnectionState) {
            lastConnectionState = connected;
            if (!connected) {
                setStatus(connectionText(false));
            }
        }

        NetworkEvent event;
        while ((event = controller.pollEvent()) != null) {
            String type = event.type();
            String[] data = event.data();
            if ("GAME_REQUEST".equals(type) && data.length >= 2
                && !data[1].isBlank()) {
                requestId = data[1];
                requestLabel.setText("Incoming request from " + data[0]);
                setRequestVisible(true);
            } else if ("MATCH_FOUND".equals(type) && data.length >= 3) {
                requestId = null;
                setRequestVisible(false);
                setStatus("Match found: " + data[0] + " (" + data[2] + ")");
                // MATCH_FOUND carries opponent, match id, and the server-assigned
                // role. Enter the synchronized game immediately so both clients
                // cannot accidentally start different local simulations.
                try {
                    App.setScreen(new IZombieNetworkView(data[1], data[2], data[0]));
                } catch (RuntimeException exception) {
                    setStatus("Match found, but the game could not be opened.");
                }
            } else if ("GAME_REQUEST_RESULT".equals(type) && data.length >= 2) {
                setStatus(data[1].equalsIgnoreCase("true")
                    ? "Challenge accepted by " + data[0]
                    : "Challenge rejected by " + data[0]);
            }
        }
    }

    @Override
    public void render(float delta) {
        processEvents();
        super.render(delta);
    }

    @Override
    public void hide() {
        controller.close();
        super.hide();
    }


}

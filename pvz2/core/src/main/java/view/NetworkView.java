package view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import controllers.menus.secondarymenus.Network;
import models.utils.RegexHelper;
import network.NetworkEvent;
import network.NetworkService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkView extends View {
    /*private Label statusLabel;
    private Label requestLabel;
    private TextButton acceptButton;
    private TextButton rejectButton;
    private String requestId;
    private boolean lastConnectionState;

    public NetworkView() {
        menu = new Network();
    }

    @Override
    protected Screen getBackScreen() {
        return new HomeView();
    }

    @Override
    protected void buildContent(Table table) {
        Network controller = (Network) menu;
        table.add(menuSectionHeader("almanac", "NETWORK PLAY",
                "Find a random opponent or challenge a specific player."))
            .width(820f).padBottom(15f).row();
        lastConnectionState = NetworkService.isConnected();
        statusLabel = secondaryLabel(lastConnectionState
            ? "CONNECTED TO SERVER" : "NOT CONNECTED TO SERVER");
        table.add(statusLabel).padBottom(10f).row();
        table.add(greenButton("FIND RANDOM OPPONENT",
                () -> setStatus(controller.findRandomOpponent())))
            .width(360f).height(54f).padBottom(12f).row();

        Table challenge = new Table();
        TextField username = field("Opponent username");
        challenge.add(username).width(330f).height(44f).padRight(8f);
        challenge.add(purpleButton("CHALLENGE",
                () -> setStatus(controller.challenge(username.getText().trim()))))
            .width(180f).height(44f);
        table.add(challenge).padBottom(16f).row();

        requestLabel = secondaryLabel("No incoming game requests.");
        table.add(requestLabel).padBottom(8f).row();
        Table decisions = new Table();
        acceptButton = greenSmallButton("ACCEPT", () -> respond(controller, true));
        rejectButton = brownButton("REJECT", () -> respond(controller, false));
        decisions.add(acceptButton).width(150f).height(44f).padRight(8f);
        decisions.add(rejectButton).width(150f).height(44f);
        table.add(decisions).row();
        setRequestVisible(false);
    }

    private void respond(Network controller, boolean accepted) {
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
            statusLabel.setText(message == null ? "" : message);
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

    private void processEvents() {
        boolean connected = NetworkService.isConnected();
        if (connected != lastConnectionState) {
            lastConnectionState = connected;
            // Do not overwrite an in-progress response while the connection is
            // still alive; only report a newly lost connection here.
            if (!connected) {
                setStatus("NOT CONNECTED TO SERVER");
            }
        }

        NetworkEvent event;
        while ((event = ((Network) menu).pollEvent()) != null) {
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
    public void input() {
        System.out.println(menu.ShowCurrentMenu());
        super.input();
        // The graphical screen normally has no terminal input.  Keep the
        // legacy command path safe when stdin is closed or unavailable.
        if (input == null || input.isBlank()) {
            return;
        }
        if (handleGlobalCommands(input)) {
            return;
        }
        Matcher connectMatcher = Pattern.compile(RegexHelper.NETWORK_CONNECT).matcher(input);
        Matcher hostMatcher = Pattern.compile(RegexHelper.NETWORK_HOST).matcher(input);
        if (connectMatcher.matches()) {
            System.out.println(((Network) menu).connectToServer(connectMatcher.group("ip"),
                Integer.parseInt(connectMatcher.group("port"))));
        } else if (hostMatcher.matches()) {
            System.out.println(((Network) menu).hostServer(Integer.parseInt(hostMatcher.group("port"))));
        } else if (input.matches(RegexHelper.NETWORK_RANDOM_MATCH)) {
            System.out.println(((Network) menu).findRandomOpponent());
        } else if (input.matches(RegexHelper.NETWORK_CHALLENGE)) {
            System.out.println(((Network) menu).challenge(input.replaceFirst("(?i)^challenge\\s+", "")));
        } else if (input.matches(RegexHelper.NETWORK_RESPOND)) {
            Matcher matcher = Pattern.compile(RegexHelper.NETWORK_RESPOND).matcher(input);
            matcher.matches();
            System.out.println(((Network) menu).respondToChallenge(matcher.group("requestId"),
                "accept".equalsIgnoreCase(matcher.group("decision"))));
        } else {
            System.out.println("Invalid command!");
        }
    }*/
}

package view.gameview;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import models.GameMessage;
import network.NetworkClient;
import utils.MessageAssetManager;

public class MessageSelector {
    private final Table root;
    private final Table optionsTable;
    private final ButtonGroup<TextButton> typeGroup;
    private final TextButton textBtn, emojiBtn, stickerBtn;

    private NetworkClient client;
    private String matchId;
    private String myName;
    private String opponentName;

    private static final String[] TEXTS = {"Nice move!", "Hurry up!", "Good game!"};
    private static final String[] EMOJIS = {"🔥", "😊", "😡"};
    private static final String[] STICKERS = {"banana", "browncoat", "breakdance"};
    private static final String[] SOUNDS = {"notification", "notification", "notification"};

    public MessageSelector(Skin skin) {
        root = new Table(skin);
        root.setBackground(skin.getDrawable("image_ui_quests_panel_edge_to_edge_ten"));
        root.pad(6);

        textBtn = new TextButton("TEXT", skin, "green_small");
        emojiBtn = new TextButton("EMOJI", skin, "green_small");
        stickerBtn = new TextButton("STICKER", skin, "green_small");

        typeGroup = new ButtonGroup<>(textBtn, emojiBtn, stickerBtn);
        typeGroup.setMaxCheckCount(1);
        textBtn.setChecked(true);

        optionsTable = new Table(skin);

        root.add(textBtn).pad(2).size(70, 30);
        root.add(emojiBtn).pad(2).size(70, 30);
        root.add(stickerBtn).pad(2).size(70, 30);
        root.row();
        root.add(optionsTable).colspan(3).pad(4);

        textBtn.addListener(e -> { showTextOptions(skin); return true; });
        emojiBtn.addListener(e -> { showEmojiOptions(skin); return true; });
        stickerBtn.addListener(e -> { showStickerOptions(skin); return true; });

        showTextOptions(skin);
        root.setSize(280, 130);
    }

    public void setContext(NetworkClient c, String mid, String me, String opp) {
        this.client = c; this.matchId = mid; this.myName = me; this.opponentName = opp;
    }

    private void showTextOptions(Skin skin) {
        optionsTable.clear();
        for (int i = 0; i < TEXTS.length; i++) {
            final String text = TEXTS[i];
            final String sound = SOUNDS[i];
            TextButton btn = new TextButton(text, skin, "brown_small");
            btn.addListener(e -> { send(GameMessage.MessageType.TEXT, text, sound); return true; });
            optionsTable.add(btn).pad(2).fillX().row();
        }
    }

    private void showEmojiOptions(Skin skin) {
        optionsTable.clear();
        for (int i = 0; i < EMOJIS.length; i++) {
            final String emoji = EMOJIS[i];
            final String sound = SOUNDS[i];
            TextButton btn = new TextButton(emoji, skin, "brown_small");
            btn.addListener(e -> { send(GameMessage.MessageType.EMOJI, emoji, sound); return true; });
            optionsTable.add(btn).pad(2).size(60, 40);
        }
    }

    private void showStickerOptions(Skin skin) {
        optionsTable.clear();
        for (int i = 0; i < STICKERS.length; i++) {
            final String key = STICKERS[i];
            final String sound = MessageAssetManager.getInstance().getStickerSound(key);
            TextButton btn = new TextButton(key, skin, "brown_small");
            btn.addListener(e -> { send(GameMessage.MessageType.STICKER, key, sound); return true; });
            optionsTable.add(btn).pad(2).fillX().row();
        }
    }

    private void send(GameMessage.MessageType type, String content, String sound) {
        if (client == null || matchId == null || opponentName == null) return;
        client.sendMessage(matchId, opponentName, type.name(), content, sound);
    }

    public Table getRoot() { return root; }
}

package view.gameview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import models.GameMessage;
import pvz.libpvz.pam.PamPlayer;
import utils.AudioManager;
import utils.MessageAssetManager;

public class MessageWidget {
    private final Table root;
    private final Label senderLabel;
    private final Label receiverLabel;
    private final Label textLabel;
    private final Image emojiImage;
    private final Table stickerContainer;

    private String stickerPamPath;
    private String stickerClipName;
    private float stickerTime;
    private boolean hasSticker;
    private PamPlayer pamPlayer;
    private String pendingSound;

    public MessageWidget(Skin skin, PamPlayer player) {
        this.pamPlayer = player;
        root = new Table(skin);
        root.setBackground(skin.getDrawable("image_ui_quests_panel_edge_to_edge_ten"));
        root.pad(8);

        senderLabel = new Label("", skin, "medium_outline");
        receiverLabel = new Label("", skin, "medium_outline");
        textLabel = new Label("", skin, "medium_outline");
        emojiImage = new Image();
        stickerContainer = new Table();
        stickerContainer.setSize(80, 80);

        root.add(senderLabel).left().row();
        root.add(receiverLabel).left().row();
        root.add(textLabel).center().row();
        root.add(emojiImage).center().row();
        root.add(stickerContainer).center().row();
        root.setSize(260, 160);
    }

    public void display(GameMessage msg) {
        senderLabel.setText(msg.getSender());
        receiverLabel.setText("-> " + msg.getReceiver());
        textLabel.setVisible(false);
        emojiImage.setVisible(false);
        stickerContainer.setVisible(false);
        hasSticker = false;
        pendingSound = msg.getSoundId();

        MessageAssetManager assets = MessageAssetManager.getInstance();

        switch (msg.getType()) {
            case TEXT:
                textLabel.setText(msg.getContentId());
                textLabel.setVisible(true);
                break;

            case EMOJI:
                String emoji = msg.getContentId();
                textLabel.setText(emoji);
                textLabel.setVisible(true);
                break;

            case STICKER:
                String pam = assets.getStickerPam(msg.getContentId());
                String clip = assets.getStickerClip(msg.getContentId());
                if (pam != null && clip != null) {
                    stickerPamPath = pam;
                    stickerClipName = clip;
                    stickerTime = 0f;
                    hasSticker = true;
                    stickerContainer.setVisible(true);
                }
                break;
        }

        if (pendingSound != null && !pendingSound.isEmpty()) {
            AudioManager.getInstance().playSound(pendingSound);
        }
    }

    public void update(float delta) {
        if (hasSticker) stickerTime += delta;
    }

    public void renderSticker(SpriteBatch batch) {
        if (!hasSticker || pamPlayer == null || stickerPamPath == null) return;

        float x = stickerContainer.getX() + 10;
        float y = stickerContainer.getY() + 10;
        float scale = 0.6f;

        pamPlayer.draw(batch, stickerPamPath, stickerClipName, stickerTime,
            x, y, scale, scale, true, null);
    }

    public Table getRoot() { return root; }
}

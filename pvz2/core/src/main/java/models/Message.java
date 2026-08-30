package models;

public class GameMessage {
    private String sender;
    private String receiver;
    private MessageType type;
    private String contentId;
    private String soundId;
    private long timestamp;

    public enum MessageType {
        TEXT,
        EMOJI,
        STICKER
    }

    public GameMessage() {}

    public GameMessage(String sender, String receiver, MessageType type, String contentId, String soundId) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
        this.contentId = contentId;
        this.soundId = soundId;
        this.timestamp = System.currentTimeMillis();
    }

    // ====== GETTERS ======
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public MessageType getType() { return type; }
    public String getContentId() { return contentId; }
    public String getSoundId() { return soundId; }
    public long getTimestamp() { return timestamp; }

    // ====== SETTERS ======
    public void setSender(String sender) { this.sender = sender; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public void setType(MessageType type) { this.type = type; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public void setSoundId(String soundId) { this.soundId = soundId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

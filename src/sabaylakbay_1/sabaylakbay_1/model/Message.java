package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A chat message. groupId > 0 = group chat, recipientId > 0 = direct message. */
public class Message {

    private int id;
    private int groupId;
    private int senderId;
    private String senderName;
    private int recipientId;
    private String content;
    private boolean announcement;
    private LocalDateTime sentAt;

    public Message() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isAnnouncement() { return announcement; }
    public void setAnnouncement(boolean announcement) { this.announcement = announcement; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

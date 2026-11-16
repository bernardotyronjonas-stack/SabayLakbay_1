package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A notification shown to a user. */
public class Notification {

    private int id;
    private int userId;
    private String message;
    private int relatedGroupId;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getRelatedGroupId() { return relatedGroupId; }
    public void setRelatedGroupId(int relatedGroupId) { this.relatedGroupId = relatedGroupId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

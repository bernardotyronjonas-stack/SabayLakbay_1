package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** One line of the admin activity log: who did what, to whom, and when. */
public class AdminLog {
    private int id;
    private String adminName;
    private String action;
    private String target;
    private String details;
    private LocalDateTime createdAt;

    public AdminLog() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

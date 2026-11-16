package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A report of a suspicious user or group. */
public class Report {

    private int id;
    private int reporterId;
    private ReportTarget targetType;
    private int targetId;
    private String reason;
    private String details;
    private LocalDateTime createdAt;
    private String reporterName;
    private String targetName;
    private ReportStatus status;
    private String actionTaken;
    private String adminNotes;
    private LocalDateTime reviewedAt;

    public Report() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }
    public ReportTarget getTargetType() { return targetType; }
    public void setTargetType(ReportTarget targetType) { this.targetType = targetType; }
    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** Full name of the reporter */
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    /** Name of the reported user / group title / "Message from ..." (for display) */
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    /** PENDING, UNDER_REVIEW, RESOLVED or DISMISSED */
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    /** What the admin did, e.g. "Warning sent" */
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    /** Admin notes */
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    /** When the admin last updated the report */
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}

package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** One uploaded ID waiting for (or checked by) an admin. */
public class IdVerification {

    private int id;
    private int userId;
    private String userName;
    private String userEmail;
    private String idType;          // National ID (PhilSys), Driver's License, Passport...
    private String filePath;
    private String selfiePath;      // face photo from the camera (null for older uploads)
    private java.time.LocalDate userBirthdate;   // shown to the admin to compare with the ID
    private VerificationStatus status;
    private LocalDateTime userCreatedAt;
    private LocalDateTime reviewedAt;

    public IdVerification() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    /** Where the ID photo is saved on this computer */
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getSelfiePath() { return selfiePath; }
    public void setSelfiePath(String selfiePath) { this.selfiePath = selfiePath; }
    public java.time.LocalDate getUserBirthdate() { return userBirthdate; }
    public void setUserBirthdate(java.time.LocalDate d) { this.userBirthdate = d; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }
    /** When the traveler registered (used as the "submitted" date) */
    public LocalDateTime getUserCreatedAt() { return userCreatedAt; }
    public void setUserCreatedAt(LocalDateTime t) { this.userCreatedAt = t; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}

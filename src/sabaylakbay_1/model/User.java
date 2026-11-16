package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A registered user. */
public class User {

    private int id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String bio;
    private String avatarPath;
    private UserRole role;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public User() { }

    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] p = fullName.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    @Override public boolean equals(Object o) { return o instanceof User && ((User) o).id == id; }
    @Override public int hashCode() { return id; }
    @Override public String toString() { return fullName; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    /** TRAVELER or ADMIN */
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    /** ACTIVE, SUSPENDED or REMOVED */
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    /** When the account was registered */
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package sabaylakbay_1.model;

import java.time.LocalDateTime;

/** A registered user. */
public class User {

    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarPath;
    private java.time.LocalDate birthdate;
    private String gender;          // Female, Male, Prefer not to say (optional)
    private String emergencyName;   // who to call during a trip
    private String emergencyPhone;
    private String street;        // house no., street, building (optional)
    private String barangay;
    private String city;          // city / municipality
    private String province;      // or "Metro Manila"
    private String zipCode;
    private Double latitude;      // home pin (null = not set)
    private Double longitude;
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    /** TRAVELER or ADMIN */
    public java.time.LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(java.time.LocalDate birthdate) { this.birthdate = birthdate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmergencyName() { return emergencyName; }
    public void setEmergencyName(String emergencyName) { this.emergencyName = emergencyName; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    /** Short place name, e.g. "Quezon City, Metro Manila" (null when no address yet). */
    public String getLocation() {
        if (city == null && province == null) return null;
        if (city == null) return province;
        if (province == null || city.contains(province)) return city;
        return city + ", " + province;
    }

    /** Full address, e.g. "12 Mapagmahal St., Brgy. Pinyahan, Quezon City, Metro Manila 1100". */
    public String getFullAddress() {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (street != null && !street.isBlank()) parts.add(street);
        if (barangay != null && !barangay.isBlank()) parts.add("Brgy. " + barangay);
        if (getLocation() != null) parts.add(getLocation());
        String a = String.join(", ", parts);
        return zipCode != null && !zipCode.isBlank() ? a + " " + zipCode : a;
    }

    public boolean hasAddress() { return city != null && !city.isBlank(); }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean hasLocation() { return latitude != null && longitude != null; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    /** ACTIVE, SUSPENDED or REMOVED */
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    /** When the account was registered */
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

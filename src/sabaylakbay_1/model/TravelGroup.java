package sabaylakbay_1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** A travel group / trip. */
public class TravelGroup {

    private int id;
    private Difficulty difficulty;
    private String category;
    private String title;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private double budgetPerPerson;
    private int maxMembers;
    private int currentMembers;
    private String transportMode;
    private String meetingPlace;
    private LocalTime meetingTime;
    private String description;
    private String creatorNotes;
    private String imagePath;
    private Double latitude;     // map pin, null = no pin
    private boolean openJoin;    // true = verified travelers join right away; false = the organizer approves each request
    private Double longitude;
    private int creatorId;
    private String creatorName;
    private GroupStatus status;
    private LocalDateTime createdAt;

    public TravelGroup() { }

    public int getSlotsAvailable() { return Math.max(0, maxMembers - currentMembers); }
    public boolean isFull() { return currentMembers >= maxMembers; }
    public boolean isCancelled() { return status == GroupStatus.CANCELLED; }
    /** A trip is completed once its end date has passed. */
    public boolean isCompleted() { return endDate != null && endDate.isBefore(LocalDate.now()); }

    public TravelGroup copy() {
        TravelGroup g = new TravelGroup();
        g.id = id; g.title = title; g.destination = destination; g.startDate = startDate; g.endDate = endDate;
        g.budgetPerPerson = budgetPerPerson; g.maxMembers = maxMembers; g.currentMembers = currentMembers;
        g.transportMode = transportMode; g.meetingPlace = meetingPlace; g.meetingTime = meetingTime;
        g.description = description; g.creatorNotes = creatorNotes; g.imagePath = imagePath; g.latitude = latitude; g.longitude = longitude;
        g.creatorId = creatorId; g.creatorName = creatorName; g.category = category; g.difficulty = difficulty; g.status = status; g.createdAt = createdAt;
        g.meetingLatitude = meetingLatitude; g.meetingLongitude = meetingLongitude;
        return g;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public double getBudgetPerPerson() { return budgetPerPerson; }
    public void setBudgetPerPerson(double budgetPerPerson) { this.budgetPerPerson = budgetPerPerson; }
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }
    public int getCurrentMembers() { return currentMembers; }
    public void setCurrentMembers(int currentMembers) { this.currentMembers = currentMembers; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public String getMeetingPlace() { return meetingPlace; }
    public void setMeetingPlace(String meetingPlace) { this.meetingPlace = meetingPlace; }
    public LocalTime getMeetingTime() { return meetingTime; }
    public void setMeetingTime(LocalTime meetingTime) { this.meetingTime = meetingTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatorNotes() { return creatorNotes; }
    public void setCreatorNotes(String creatorNotes) { this.creatorNotes = creatorNotes; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean hasPin() { return latitude != null && longitude != null; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public GroupStatus getStatus() { return status; }
    public void setStatus(GroupStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /** Hiking, Beach, Food trip ... */
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    /** EASY, MODERATE or HARD */
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    // map pin of the MEETING PLACE (used for "Groups near you")
    private Double meetingLatitude, meetingLongitude;
    public Double getMeetingLatitude() { return meetingLatitude; }
    public void setMeetingLatitude(Double v) { this.meetingLatitude = v; }
    public Double getMeetingLongitude() { return meetingLongitude; }
    public void setMeetingLongitude(Double v) { this.meetingLongitude = v; }
    public boolean hasMeetingPin() { return meetingLatitude != null && meetingLongitude != null; }

    /** OPEN JOIN: verified travelers join right away. Otherwise (REQUEST) the organizer approves each one. */
    public boolean isOpenJoin() { return openJoin; }
    public void setOpenJoin(boolean openJoin) { this.openJoin = openJoin; }
}

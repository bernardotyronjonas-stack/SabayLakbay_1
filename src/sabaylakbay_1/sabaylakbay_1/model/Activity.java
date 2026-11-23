package sabaylakbay_1.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One itinerary item (a "stop") of a trip: when, what, where, an optional photo and an optional map pin. */
public class Activity {

    private int id;
    private String details;
    private int groupId;
    private LocalDate date;
    private LocalTime time;
    private String title;
    private String location;
    private String photoPath;          // saved photo of the place (uploads/itinerary/...), or null
    private Double latitude, longitude; // map pin of the place, or null

    public Activity() { }

    /** A copy, so a form can change it and still cancel. */
    public Activity copy() {
        Activity a = new Activity();
        a.id = id; a.details = details; a.groupId = groupId; a.date = date; a.time = time;
        a.title = title; a.location = location; a.photoPath = photoPath; a.latitude = latitude; a.longitude = longitude;
        return a;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    /** Extra details, e.g. "Bring a change of clothes" */
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public boolean hasPin() { return latitude != null && longitude != null; }
    public boolean hasPhoto() { return photoPath != null && !photoPath.isBlank(); }
}

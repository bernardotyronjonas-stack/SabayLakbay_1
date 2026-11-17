package sabaylakbay_1.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One itinerary item of a trip. */
public class Activity {

    private int id;
    private String details;
    private int groupId;
    private LocalDate date;
    private LocalTime time;
    private String title;
    private String location;

    public Activity() { }

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
}

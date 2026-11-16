package sabaylakbay_1.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One itinerary item of a trip. */
public class Activity {

    private int id;
    private int groupId;
    private LocalDate date;
    private LocalTime time;
    private String title;
    private String location;
    private double estimatedCost;

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
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
}

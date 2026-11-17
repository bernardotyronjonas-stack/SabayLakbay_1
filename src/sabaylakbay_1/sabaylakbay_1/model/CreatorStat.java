package sabaylakbay_1.model;

/** One row of "Top performing creators" on the admin dashboard. */
public class CreatorStat {

    private User user;
    private int groupsCreated;
    private int completedTrips;
    private int travelersHosted;
    private double rating;

    public CreatorStat() { }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getGroupsCreated() { return groupsCreated; }
    public void setGroupsCreated(int v) { groupsCreated = v; }
    public int getCompletedTrips() { return completedTrips; }
    public void setCompletedTrips(int v) { completedTrips = v; }
    /** Total members across all their groups */
    public int getTravelersHosted() { return travelersHosted; }
    public void setTravelersHosted(int v) { travelersHosted = v; }
    /** Average rating from reviews */
    public double getRating() { return rating; }
    public void setRating(double v) { rating = v; }
}

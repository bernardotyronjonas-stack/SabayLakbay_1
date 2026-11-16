package sabaylakbay_1.model;

/** Numbers for the admin Dashboard Overview cards. */
public class AdminStats {

    private int totalUsers;
    private int totalGroups;
    private int activeTrips;
    private int completedTrips;
    private int pendingReports;

    public AdminStats() { }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int v) { totalUsers = v; }
    public int getTotalGroups() { return totalGroups; }
    public void setTotalGroups(int v) { totalGroups = v; }
    /** Open groups whose trip has not ended yet */
    public int getActiveTrips() { return activeTrips; }
    public void setActiveTrips(int v) { activeTrips = v; }
    /** Groups whose end date has passed */
    public int getCompletedTrips() { return completedTrips; }
    public void setCompletedTrips(int v) { completedTrips = v; }
    /** Reports with status PENDING or UNDER_REVIEW */
    public int getPendingReports() { return pendingReports; }
    public void setPendingReports(int v) { pendingReports = v; }
}

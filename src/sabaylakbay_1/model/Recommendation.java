package sabaylakbay_1.model;

/** A trip suggested on the Home page, with the reason it was picked ("Because you like Hiking"). */
public class Recommendation {
    private final TravelGroup group;
    private final String reason;
    private final double score;

    public Recommendation(TravelGroup group, String reason, double score) {
        this.group = group;
        this.reason = reason;
        this.score = score;
    }

    public TravelGroup getGroup() { return group; }
    public String getReason() { return reason; }
    public double getScore() { return score; }
}

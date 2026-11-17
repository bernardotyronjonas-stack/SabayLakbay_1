package sabaylakbay_1.model;

/**
 * Two numbers for one month, used by the analytics charts.
 * e.g. month "Jan", first = trips created, second = trips completed.
 */
public class MonthlyCount {

    private String month;
    private int first;
    private int second;

    public MonthlyCount() { }

    public MonthlyCount(String month, int first, int second) {
        this.month = month;
        this.first = first;
        this.second = second;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public int getFirst() { return first; }
    public void setFirst(int first) { this.first = first; }
    public int getSecond() { return second; }
    public void setSecond(int second) { this.second = second; }
}

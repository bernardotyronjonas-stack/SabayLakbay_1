package sabaylakbay_1.model;

/** Estimated expense per person for a trip. */
public class Expense {

    private int id;
    private int groupId;
    private String label;
    private double amount;

    public Expense() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

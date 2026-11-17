package sabaylakbay_1.model;

/** How hard a trip is. */
public enum Difficulty {
    EASY("Easy"), MODERATE("Moderate"), HARD("Hard");

    private final String label;

    Difficulty(String label) { this.label = label; }

    @Override public String toString() { return label; }
}

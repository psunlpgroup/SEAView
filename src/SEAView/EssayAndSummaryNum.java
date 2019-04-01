package SEAView;


/**
 * A class to represent the essay and summary number of a text selection.
 */
public class EssayAndSummaryNum {
    private int essayNumber;
    private boolean isSummary;

    public EssayAndSummaryNum(int essayNumber, boolean isSummary) {
        this.essayNumber = essayNumber;
        this.isSummary = isSummary;
    }

    public int getEssayNumber() {
        return essayNumber;
    }

    public boolean isSummary() {
        return isSummary;
    }
}

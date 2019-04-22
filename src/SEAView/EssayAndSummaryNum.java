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

    /**
     * Get the location of a text selection from within the model essays.
     * Indicate which essay it comes from, and whether it is from a summary or from a body.
     * @param startIndex the start of the selection
     * @param endIndex the end of the selection
     * @return the essay number and whether it is part of a summary
     *         if the regex headers are not initialized, return -1 for essay number
     *         if the selection is otherwise invalid, return 0 for essay number
     */
    public static EssayAndSummaryNum getEssayAndSummaryNumber(int startIndex, int endIndex, int[] startDocumentIndexes, int[] startBodyIndexes) {
        if (startDocumentIndexes == null || startBodyIndexes == null) {
            return new EssayAndSummaryNum(-1, false);
        }

        // Find essay number (first essay is 1)
        int essayNumber = 0;
        while (essayNumber < startDocumentIndexes.length && startIndex > startDocumentIndexes[essayNumber]) {
            essayNumber++;
        }

        // Find whether selection is part of summary
        boolean isSummary = startIndex < startBodyIndexes[essayNumber - 1];

        // Check if start and end indices are both within same section of the same essay
        if (essayNumber != startDocumentIndexes.length || isSummary) {
            int lowerBound = isSummary ? startDocumentIndexes[essayNumber - 1] : startBodyIndexes[essayNumber - 1];
            int upperBound = isSummary ? startBodyIndexes[essayNumber - 1] : startDocumentIndexes[essayNumber];
            if (startIndex < lowerBound || endIndex > upperBound) {
                return new EssayAndSummaryNum(0, isSummary);
            }
        }
        return new EssayAndSummaryNum(essayNumber, isSummary);
    }

    public int getEssayNumber() {
        return essayNumber;
    }

    public boolean isSummary() {
        return isSummary;
    }

}

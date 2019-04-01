package SEAView;

import java.io.Serializable;

public class SCUContributorPart implements Serializable {
    private int startIndex;
    private int endIndex;
    private String text;

    public SCUContributorPart(int startIndex, int endIndex, String text) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.text = text;
    }

    public String toString() {
        return this.text;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public String getText() {
        return this.text;
    }
}
package SEAView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class SCUContributor implements java.util.Comparator, Serializable {
    private ArrayList scuContributorParts;
    private String comment = "";

    public SCUContributor(SCUContributorPart part) {
        this(part, "");
    }

    public SCUContributor(SCUContributorPart part, String comment) {
        this.scuContributorParts = new ArrayList();
        this.scuContributorParts.add(part);
        this.comment = comment;
    }

    public void add(SCUContributorPart part) {
        this.scuContributorParts.add(part);
    }

    public int getNumParts() {
        return this.scuContributorParts.size();
    }

    public SCUContributorPart getSCUContributorPart(int index) {
        return (SCUContributorPart) this.scuContributorParts.get(index);
    }

    public java.util.Iterator elements() {
        return this.scuContributorParts.iterator();
    }

    public void removeSCUContributorPart(SCUContributorPart part) {
        this.scuContributorParts.remove(part);
    }

    public String toString() {
        if (this.scuContributorParts.size() == 1) {
            return this.scuContributorParts.get(0).toString();
        }


        Object[] parts = this.scuContributorParts.toArray();
        Arrays.sort(parts, this);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            if (buffer.length() > 0) {
                buffer.append("...");
            }
            buffer.append(((SCUContributorPart) parts[i]).getText());
        }
        return buffer.toString();
    }


    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int compare(Object o1, Object o2) {
        SCUContributorPart part1 = (SCUContributorPart) o1;
        SCUContributorPart part2 = (SCUContributorPart) o2;
        if (part1.getStartIndex() < part2.getStartIndex()) {
            return -1;
        }
        if (part2.getStartIndex() < part1.getStartIndex()) {
            return 1;
        }


        return 0;
    }
}

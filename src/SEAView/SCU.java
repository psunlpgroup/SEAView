package SEAView;

import java.io.Serializable;

public class SCU implements Serializable {
    private static int nextAvailableId = 1;
    private int id;
    private String tempId; // Used to order tree nodes and table cells
    private int weight;
    private String label;
    private String comment = "";
    private boolean isEdu = false;
    private boolean isPeer = false;

    public SCU(String label) {
        this.id = (nextAvailableId++);
        this.label = label;
    }

    public SCU(String label, int tempId) {
        this(label);
        this.tempId = Integer.toString(tempId);
    }

    public SCU(int id, String label) {
        this(id, label, "");
    }

    public SCU(int id, String label, String comment) {
        this.id = id;
        if (id >= nextAvailableId)
            nextAvailableId = id + 1;
        this.label = label;
        this.comment = comment;
    }

    public SCU(int id, String label, String comment, int tempId, int weight) {
        this(id, label, comment);
        this.tempId = Integer.toString(tempId);
        this.weight = weight;
    }

    public SCU(int id, String label, String comment, String tempId, int weight) {
        this(id, label, comment);
        this.tempId = tempId;
        this.weight = weight;
    }

    public String toString() {
        if (isEdu) {
            if (this.tempId != null) {
                return this.tempId + ". " + this.label;
            }
            return this.label;
        }
        if (isPeer) {
            return this.tempId + ". " + this.label;
        }
        return this.tempId + ". (" + this.weight + ") " + this.label;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() { return this.label; }

    public void setScuTempId(int tempId) { this.tempId = Integer.toString(tempId); }

    public void setEduTempId(String tempId) {
        this.tempId = tempId;
    }

    public void setEdu(boolean isEdu) {
        this.isEdu = isEdu;
        this.tempId = null;
    }

    public void isPeer(boolean peer) {
        isPeer = peer;
    }

    public int getId() {
        return this.id;
    }

    public String getTempId() {
        return tempId;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isEdu() {
        return isEdu;
    }
}
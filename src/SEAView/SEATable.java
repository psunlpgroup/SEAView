package SEAView;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class SEATable extends JTable implements MouseListener, MouseMotionListener {
    private DefaultTableModel tableModel;
    private SEAView seaView;
    private Vector<Integer> borderRows;
    private boolean isPeer = false;
    private boolean setLabelNow = true; // Labeling mode (if true, user chooses label when inserting EDUs, else, user must set it later)
    private final int eduColumn = 0;
    private final int scuColumn = 1;
    private final int initialRows = 300;
    private DataFlavor treeNodeFlavor = null;
    private final String treeNodeMimeType = "application/x-java-jvm-local-objectref; class=javax.swing.tree.DefaultMutableTreeNode";

    SEATable(SEAView seaView) {
        this.seaView = seaView;
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == eduColumn;
            }
        };
        tableModel.setColumnIdentifiers(new Object[]{"<html><b>EDU</b></html>", "<html><b>SCU</b></html>"});
        for (int i = 0; i < initialRows; i++) {
            tableModel.addRow(new Object[]{});
        }

        setModel(tableModel);

        setDefaultRenderer(Object.class, new SEATableCellRenderer());
        getColumnModel().getColumn(eduColumn).setCellEditor(new SEATableCellEditor());

        setCellSelectionEnabled(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        try {
            treeNodeFlavor = new DataFlavor(treeNodeMimeType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        setTransferHandler(new SEATableTransferHandler());

        FontMetrics metrics = getFontMetrics(getFont());
        setRowHeight(2 * metrics.getHeight());
        borderRows = null;
        setDragEnabled(true);
        addMouseListener(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        return isInvalidRow(row) ? "Each SCU must have a corresponding EDU" : ""; // Indicate invalid rows
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        JComponent c = (JComponent) super.prepareRenderer(renderer, row, column);
        if (borderRows != null && borderRows.contains(row)) {
            // When table is sorted, show row borders
            Color color = UIManager.getColor("Table.gridColor");
            MatteBorder border = new MatteBorder(2, 0, 0, 0, new Color(0,0,153));
            c.setBorder(border);
        }
        else {
            c.setBorder(UIManager.getBorder("Table.border"));
        }
        return c;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        for (int row = 0; row < getRowCount(); row++) {
            SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
            if (eduTree != null) {
                eduTree.setFont(font);
                setRowHeight(row, eduTree.getPreferredSize().height + 2);
            }
        }
    }

    public void setPeer(boolean peer) {
        isPeer = peer;
    }

    /**
     * Check for invalid rows in the table. A row is invalid if there is an SCU without a corresponding EDU.
     *
     * @param row the row to check
     * @return true if the row is invalid
     */
    private boolean isInvalidRow(int row) {
        return getValueAt(row, scuColumn) != null && getValueAt(row, eduColumn) == null;
    }

    /**
     * Clear the table
     */
    public void clear() {
        if (isEditing()) getCellEditor().stopCellEditing();
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(null, row, eduColumn);
            setValueAt(null, row, scuColumn);
        }
        borderRows = null;
        FontMetrics metrics = getFontMetrics(getFont());
        setRowHeight(2 * metrics.getHeight());
    }

    protected void clearTreeSelections() {
        // Clear tree node selections
        if (isEditing()) getCellEditor().stopCellEditing();
        seaView.pyramidTree.clearSelection();
        seaView.peerTree.clearSelection();
        for (int row = 0; row < getRowCount(); row++) {
            SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
            if (eduTree != null) {
                eduTree.clearSelection();
                tableModel.fireTableCellUpdated(row, eduColumn);
            }
        }
    }

    /**
     * Insert a tree representation of an EDU into a table cell
     *
     * @param text the text to be inserted into the table
     * @param row the row in which the text should be inserted
     * @return true if the insertion was successful; otherwise, false
     */
    private boolean insertEDUIntoCell(String text, int row) {
        EssayAndSummaryNum essayAndSummaryNum = null;
        if (isPeer) {
            if (seaView.essayPane.getSelectionStartIndex() < seaView.getStartBodyIndexes()[0]) {
                seaView.showError("Invalid EDU", "EDUs cannot come from the summary.");
                return false;
            }
        }
        else {
            int start = seaView.essayPane.getSelectionStartIndex();
            int end = seaView.essayPane.getSelectionEndIndex();
            int[] startDocumentIndexes = seaView.getStartDocumentIndexes();
            int[] startBodyIndexes = seaView.getStartBodyIndexes();
            essayAndSummaryNum = EssayAndSummaryNum.getEssayAndSummaryNumber(start, end, startDocumentIndexes, startBodyIndexes);
            if (essayAndSummaryNum.isSummary()) {
                seaView.showError("Invalid EDU", "EDUs cannot come from the summary.");
                return false;
            }
            if (!isPeer && essayAndSummaryNum.getEssayNumber() == -1) {
                seaView.showError("Invalid EDU", "Initialize the document RegEx before creating EDUs.");
                return false;
            }
            else if (essayAndSummaryNum.getEssayNumber() == 0) {
                seaView.showError("Invalid EDU", "Invalid text selection.");
                return false;
            }
        }
        SCUTree tree = (SCUTree) SEATable.this.getValueAt(row, eduColumn);
        SCU edu = new SCU(text);
        edu.setEdu(true);
        DefaultMutableTreeNode labelNode = new DefaultMutableTreeNode(edu);
        DefaultMutableTreeNode contributorNode = new DefaultMutableTreeNode(new SCUContributor(new SCUContributorPart(seaView.essayPane.getSelectionStartIndex(), seaView.essayPane.getSelectionEndIndex(), text)));
        labelNode.add(contributorNode);
        if (tree == null) {
            String eduLabel = edu.getLabel();
            if (setLabelNow) {
                /*JTextField labelField = new JTextField();
                labelField.setText(eduLabel);
                labelField.setSize(new Dimension(480, 10));
                labelField.setPreferredSize(new Dimension(480, labelField.getPreferredSize().height));
                //String newEduLabel = JOptionPane.showMessageDialog(null, labelPane, "Enter the label for the new EDU", JOptionPane.INFORMATION_MESSAGE);
                JOptionPane labelPane = new JOptionPane(labelField, JOptionPane.PLAIN_MESSAGE);
                labelPane.showInputDialog(seaView, "Enter the label for the new EDU", "Name EDU", JOptionPane.PLAIN_MESSAGE);

                showInputDialog(Component parentComponent, Object message, String title, int messageType)
                //String newEduLabel = (String) JOptionPane.showInputDialog(labelField, 'h');*/
                String newEduLabel = (String) JOptionPane.showInputDialog(seaView, "Enter the label for the new EDU", "Name EDU", JOptionPane.PLAIN_MESSAGE, null, null, eduLabel);

                if ((newEduLabel != null) && (newEduLabel.trim().length() > 0)) {
                    edu.setLabel(newEduLabel);
                }
            }
            seaView.msg("Creating new EDU \"" + eduLabel + "\"");
            tree = new SCUTree(SEATable.this.seaView);
            tree.reset();
            tree.insertNodeInto(labelNode, tree.getRootNode());
            tree.setSCUTextPane(seaView.essayPane);
            tree.setFont(getFont());
            SEATable.this.setValueAt(tree, row, eduColumn);
            seaView.essayPane.modifyTextSelection(seaView.essayPane.getSelectionStartIndex(), seaView.essayPane.getSelectionEndIndex(), true);
        }
        else {
            SCUContributor firstContributor = (SCUContributor) tree.getRootNode().getNextNode().getFirstLeaf().getUserObject();
            int startIndexOfParent = firstContributor.getSCUContributorPart(0).getStartIndex();
            int endIndexOfParent = firstContributor.getSCUContributorPart(0).getEndIndex();
            EssayAndSummaryNum essayAndSummaryNumParent = essayAndSummaryNum.getEssayAndSummaryNumber(startIndexOfParent, endIndexOfParent, seaView.getStartDocumentIndexes(), seaView.getStartBodyIndexes());
            if (isPeer || essayAndSummaryNum.getEssayNumber() == essayAndSummaryNumParent.getEssayNumber()) {
                seaView.msg("Adding EDU contributor \"" + text + "\" to EDU \"" + tree.getRootNode().getNextNode().toString() + "\"");
                tree.insertNodeInto(labelNode.getNextNode(), tree.getRootNode().getNextNode());
                //tableModel.fireTableCellUpdated(row, scuColumn);
                seaView.essayPane.modifyTextSelection(seaView.essayPane.getSelectionStartIndex(), seaView.essayPane.getSelectionEndIndex(), true);
            }
            else {
                seaView.showError("Invalid EDU", "This EDU Contributor must come from essay #" + essayAndSummaryNumParent.getEssayNumber() + ".");
                return false;
            }
        }
        tree.expandTree();
        setRowHeight(row, tree.getPreferredSize().height + 2); // Account for row borders
        borderRows = null;
        setModified();
        return true;
    }

    public void insertEDUTree(DefaultMutableTreeNode eduNode, int row, boolean ignoreRowHeightUpdates) {
        SCUTree tree = new SCUTree(SEATable.this.seaView);
        tree.reset();
        tree.insertNodeInto(eduNode, tree.getRootNode());
        tree.setSCUTextPane(seaView.essayPane);
        //tree.setPyramidReferenceTextPane(seaView.essayPane);
        tree.expandTree();
        if (!ignoreRowHeightUpdates) {
            setRowHeight(row, tree.getPreferredSize().height + 2);
        }
        setValueAt(tree, row, eduColumn);
    }

    public void changeEDULabel() {
        int row = getSelectedRow();
        SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
        if (eduTree != null) {
            DefaultMutableTreeNode eduNode = eduTree.getRootNode().getNextNode();
            if (eduNode != null) {
                SCU edu = (SCU) eduNode.getUserObject();
                String eduLabel = edu.getLabel();
                String newEduLabel = (String) JOptionPane.showInputDialog(seaView, "Enter the label for the selected EDU", "Rename EDU", JOptionPane.PLAIN_MESSAGE, null, null, eduLabel);

                if ((newEduLabel != null) && (newEduLabel.trim().length() > 0)) {
                    edu.setLabel(newEduLabel);
                    eduTree.nodeChanged(eduNode);
                    tableModel.fireTableCellUpdated(row, eduColumn);
                    setModified();
                }
            }
        }
    }

    /**
     * Re-order the table cells based on the order of the EDUs from the model essays
     */
    public Map orderEDUs(boolean isAlignment, boolean getPeer) {
        if (SEATable.this.isEditing()) SEATable.this.getCellEditor().stopCellEditing();
        Vector scueduPairs = getOrderedEDUs();
        if (scueduPairs.isEmpty()) {
            return null;
        }

        SCUEDUPair currentPair;
        int currentRow = 0;
        int currentEssay = 1;
        int currentEdu = 0;

        Map<String, List<String>> alignment = new HashMap<>();

        boolean orderTable = !(isPeer && !getPeer);
        if (orderTable) {
            clear();
            borderRows = new Vector<>();
            borderRows.add(0);
        }

        while (!scueduPairs.isEmpty()) {
            currentPair = (SCUEDUPair) scueduPairs.remove(0);
            SCUTree eduTree = currentPair.getEDUTree();
            SCU edu = (SCU) eduTree.getRootNode().getNextNode().getUserObject();
            DefaultMutableTreeNode scuNode = currentPair.getSCU();

            if (orderTable) {
                int essayNum = getEssayNumberofEdu(eduTree.getRootNode().getNextNode());
                if (essayNum > currentEssay) {
                    currentEssay = essayNum;
                    currentEdu = 1;
                    borderRows.add(currentRow);
                } else {
                    currentEdu++;
                }

                String eduId = isPeer ? Integer.toString(currentEdu) : currentEssay + "." + currentEdu;
                edu.setEduTempId(eduId);

                setValueAt(eduTree, currentRow, eduColumn);
                setValueAt(scuNode, currentRow, scuColumn);
                setRowHeight(currentRow, eduTree.getPreferredSize().height + 2);
                currentRow++;
            }

            if (isAlignment && scuNode != null) {
                SCU scu = (SCU) scuNode.getUserObject();
                String scuId = scu.getTempId();
                List<String> edusList = alignment.get(scuId);
                if (edusList == null) {
                    edusList = new ArrayList<>();
                }
                edusList.add(edu.getTempId());
                alignment.put(scuId, edusList);
            }
        }
        if (orderTable) {
            borderRows.add(currentRow);
            clearTreeSelections();
        }
        return isAlignment ? alignment : null;
    }

    /**
     * Order the EDUs in the table based on their index in the model essays
     * @return an ordered vector containing pairs of EDUs and SCUs, in order of the EDU's occurence in the text
     */
    private Vector getOrderedEDUs() {
        Vector<SCUEDUPair> scueduPairs = new Vector<>();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (isInvalidRow(row)) {
                seaView.showError("Invalid rows", "Fix invalid rows before proceeding.");
                return scueduPairs;
            }
        }
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (getValueAt(row, eduColumn) != null) {
                scueduPairs.add(new SCUEDUPair((SCUTree) getValueAt(row, eduColumn), (DefaultMutableTreeNode) getValueAt(row, scuColumn)));
            }
        }
        Collections.sort(scueduPairs);
        return scueduPairs;
    }

    public int getEssayNumberofEdu(DefaultMutableTreeNode eduNode) {
        SCUContributor contributor = (SCUContributor) eduNode.getFirstLeaf().getUserObject();
        int startIndex = contributor.getSCUContributorPart(0).getStartIndex();
        EssayAndSummaryNum essayAndSummaryNum = EssayAndSummaryNum.getEssayAndSummaryNumber(startIndex, startIndex + 1, seaView.getStartDocumentIndexes(), seaView.getStartBodyIndexes());
        return essayAndSummaryNum.getEssayNumber();
    }

    public void selectEdusByIndex(int index) {
        for (int row = 0; row < getRowCount(); row++) {
            SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
            if (eduTree != null) {
                DefaultMutableTreeNode eduNode = eduTree.getRootNode().getNextNode();
                SCU edu = (SCU) eduNode.getUserObject();
                Enumeration eduContributorNodeEnum = eduNode.children();

                while (eduContributorNodeEnum.hasMoreElements()) {
                    SCUContributor scuContributor = (SCUContributor) ((DefaultMutableTreeNode) eduContributorNodeEnum.nextElement()).getUserObject();


                    for (int i = 0; i < scuContributor.getNumParts(); i++) {
                        SCUContributorPart scuContributorPart = scuContributor.getSCUContributorPart(i);

                        if ((scuContributorPart.getStartIndex() <= index) && (scuContributorPart.getEndIndex() >= index)) {
                            clearTreeSelections();
                            eduTree.selectSCUNode(edu.getId());
                            tableModel.fireTableCellUpdated(row, eduColumn);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the SCU tempIds in the table when the SCU tree order is changed
     */
    public void updateSCUIndices() {
        for (int row = 0; row < getRowCount(); row++) {
            if (getValueAt(row, scuColumn) != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) getValueAt(row, scuColumn);
                SCU oldSCU = (SCU) node.getUserObject();
                SCU newSCU = isPeer ? seaView.peerTree.getSCUsByID(oldSCU.getId()) : seaView.pyramidTree.getSCUsByID(oldSCU.getId());
                node.setUserObject(newSCU);
                setValueAt(node, row, scuColumn);
            }
        }
    }

    public void remove() {
        int row = getSelectedRow();
        int col = getSelectedColumn();
        if (col == eduColumn) {
            SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
            if (eduTree != null) {
                DefaultMutableTreeNode eduNode = (DefaultMutableTreeNode) eduTree.getLastSelectedPathComponent();
                if (isEditing()) getCellEditor().stopCellEditing();
                SCUContributorPart eduContributorPart;
                if (eduNode != null) {
                    if (eduNode.getLevel() == 1) {
                        eduContributorPart = ((SCUContributor) eduNode.getNextNode().getUserObject()).getSCUContributorPart(0);
                        setValueAt(null, row, eduColumn);
                    }
                    else {
                        eduContributorPart = ((SCUContributor) eduNode.getUserObject()).getSCUContributorPart(0);
                        if (eduNode.getSiblingCount() == 1) {
                            setValueAt(null, row, eduColumn);
                        }
                        else {
                            eduTree.removeNodeFromParent(eduNode);
                        }
                    }
                    setRowHeight(row, eduTree.getPreferredSize().height + 2);
                    seaView.getChangeLabelBtn().setEnabled(false);
                    seaView.essayPane.modifyTextSelection(eduContributorPart.getStartIndex(), eduContributorPart.getEndIndex(), false);
                }
            }
        }
        else if (col == scuColumn){
            setValueAt(null, row, scuColumn);
        }
        tableModel.fireTableCellUpdated(row, col);
        setModified();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        clearTreeSelections();
        //int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());
        if (col == scuColumn) {
            seaView.getChangeLabelBtn().setEnabled(false);
            seaView.getRemoveBtn().setEnabled(false);


            // Select tree node in GUI
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) getValueAt(getSelectedRow(), getSelectedColumn());
            if (scuNode != null) {
                seaView.getRemoveBtn().setEnabled(true);
                SCU scu = (SCU) scuNode.getUserObject();
                int scuId = scu.getId();
                if (isPeer) {
                    seaView.peerTree.selectSCUNode(scuId);
                }
                else {
                    seaView.pyramidTree.selectSCUNode(scuId);
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    private void setModified() {
        if (isPeer) {
            seaView.setPeerModified(true);
        }
        else {
            seaView.setCrowdModified(true);
        }
    }

    public void setLabelMode(boolean setLabelNow) {
        this.setLabelNow = setLabelNow;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    /**
     * Support for table cell rendering of trees
     */
    private class SEATableCellRenderer extends DefaultTableCellRenderer {
        Object value;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.value = value;
            if (value instanceof SCUTree) {
                SCUTree tree = (SCUTree) value;
                //tree.setFont(getFont());
                //SEATable.this.setRowHeight(tree.getPreferredSize().height + 2);
                return tree;
            }
            //this.value = value;
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(isInvalidRow(row) ? new Color(220, 220, 220) : Color.white); // Set invalid row to light grey
            if (isSelected) {
                c.setBackground(getSelectionBackground());
            }
            return c;
        }
    }

    private class SEATableCellEditor extends AbstractCellEditor implements TableCellEditor {
        Object value;

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = value;
            if (value instanceof SCUTree) {
                SCUTree tree = (SCUTree) value;

                tree.addTreeExpansionListener(new TreeExpansionListener() {

                    @Override
                    public void treeExpanded(TreeExpansionEvent event) {
                        table.setRowHeight(row, tree.getPreferredSize().height + 2);
                    }

                    @Override
                    public void treeCollapsed(TreeExpansionEvent event) {
                        table.setRowHeight(row, tree.getPreferredSize().height + 2);
                    }
                });
                clearTreeSelections();
            }
            SEATableCellRenderer renderer = new SEATableCellRenderer();
            return renderer.getTableCellRendererComponent(table, value, isSelected, isSelected, row, column);
        }
    }

    private class SEATableTransferHandler extends TransferHandler {
        @Override
        public boolean importData(TransferSupport support) {
            if (SEATable.this.isEditing()) SEATable.this.getCellEditor().stopCellEditing();
            Point point = support.getDropLocation().getDropPoint();
            int row = SEATable.this.rowAtPoint(point);
            int column = SEATable.this.columnAtPoint(point);
            try {
                if (column == eduColumn) {
                    String text  = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    insertEDUIntoCell(text, row);
                }
                else {
                    DefaultMutableTreeNode node  = (DefaultMutableTreeNode) support.getTransferable().getTransferData(new DataFlavor(treeNodeMimeType));

                    if (!(node.toString().contains("All non-matching SCUs go here")) && node.getUserObject() instanceof SCU) {
                        SCU scu = (SCU) node.getUserObject();
                        if (!scu.isEdu()) {
                            SEATable.this.seaView.msg("Adding SCU \"" + node.toString() + "\" to table");
                            SEATable.this.setValueAt(node, row, column);
                            if (!isInvalidRow(row)) {
                                setModified();
                            }
                        }
                    }
                    //borderRows = null;
                }
                return true;
            }
            catch (UnsupportedFlavorException | IOException | ClassNotFoundException | ClassCastException ex) {
                SEATable.this.seaView.msg("Invalid drag and drop.");
                ex.printStackTrace();
                return false;
            }
        }

        @Override
        public boolean canImport(TransferSupport support) {
            //if (SEATable.this.isEditing()) SEATable.this.getCellEditor().stopCellEditing();
            Point point = support.getDropLocation().getDropPoint();
            int column = SEATable.this.columnAtPoint(point);
            Transferable transferable = support.getTransferable();

            if (support.getDropAction() == MOVE) {
                return false;
            }
            if (column == eduColumn && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return true;
            }
            else if (treeNodeFlavor != null) {
                if (column == scuColumn && transferable.isDataFlavorSupported(treeNodeFlavor)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            SEATable.this.remove();
        }
    }

    private class SEATableSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            //System.out.println(Integer.toString(getSelectedRow()) + Integer.toString(getSelectedColumn()));
            //if (isEditing()) getCellEditor().stopCellEditing();
            if (e.getValueIsAdjusting()) {
                //return;
            }
            // Clear tree node selections
            //seaView.pyramidTree.clearSelection();
            /*for (int row = 0; row < getRowCount(); row++) {
                if (row != getSelectedRow() || getSelectedColumn() == scuColumn) {
                    SCUTree eduTree = (SCUTree) getValueAt(row, eduColumn);
                    if (eduTree != null) {
                        eduTree.clearSelection();
                    }
                }
            }*/

            /*if (getSelectedColumn() == scuColumn) {
                //System.out.println("scu");
                seaView.getChangeLabelBtn().setEnabled(false);

                // Select tree node in GUI
                DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) getValueAt(getSelectedRow(), getSelectedColumn());
                if (scuNode != null) {
                    SCU scu = (SCU) scuNode.getUserObject();
                    int scuId = scu.getId();
                    seaView.pyramidTree.selectSCUNode(scuId);
                }
            }*/
            else {
                //seaView.getChangeLabelBtn().setEnabled(true);
                //seaView.essayPane.modifyTextHighlight(0, 10, true);
            }
            /*if (seaView.isCrowdLoaded) {
                SEAViewTextPane pane = seaView.essayPane;
                pane.modifyTextHighlight(0, pane.getText().length() - 1, false);
                //ListSelectionModel lsm = (ListSelectionModel) e.getSource();

                Enumeration contributors = null;
                if (getSelectedColumn() == eduColumn) {
                    SCUTree eduTree = (SCUTree) getValueAt(getSelectedRow(), getSelectedColumn());
                    if (eduTree != null) {
                        contributors = eduTree.getRootNode().getNextNode().children();
                    }
                }
                else {
                    pane = seaView.modelEssaysPane;
                    DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) getValueAt(getSelectedRow(), getSelectedColumn());
                    if (scuNode != null) {
                        contributors = scuNode.children();
                    }
                }

                if (contributors != null) {
                    ArrayList startIndexes = new ArrayList();
                    while (contributors.hasMoreElements()) {
                        DefaultMutableTreeNode contributor = (DefaultMutableTreeNode) contributors.nextElement();
                        SCUContributorPart contributorPart = ((SCUContributor) contributor.getUserObject()).getSCUContributorPart(0);
                        startIndexes.add(contributorPart.getStartIndex());
                        pane.modifyTextHighlight(contributorPart.getStartIndex(), contributorPart.getEndIndex(), true);
                    }
                    if (!startIndexes.isEmpty()) {
                        pane.showText((int) startIndexes.get(0));
                    }
                }
            }
            if (seaView.isPeerLoaded) {
                seaView.modelEssaysPane.modifyTextHighlight(0, seaView.modelEssaysPane.getText().length() - 1, false);
                //ListSelectionModel lsm = (ListSelectionModel) e.getSource();

                Enumeration contributors = null;
                if (getSelectedColumn() == eduColumn) {
                    DefaultMutableTreeNode eduNode = seaEDUTrees.get(getSelectedRow());
                    if (eduNode != null) {
                        contributors = eduNode.children();
                    }
                }
                else {
                    DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) getValueAt(getSelectedRow(), getSelectedColumn());
                    if (scuNode != null) {
                        contributors = scuNode.children();
                    }
                }

                if (contributors != null) {
                    ArrayList startIndexes = new ArrayList();
                    while (contributors.hasMoreElements()) {
                        DefaultMutableTreeNode contributor = (DefaultMutableTreeNode) contributors.nextElement();
                        SCUContributorPart contributorPart = ((SCUContributor) contributor.getUserObject()).getSCUContributorPart(0);
                        startIndexes.add(contributorPart.getStartIndex());
                        seaView.modelEssaysPane.modifyTextHighlight(contributorPart.getStartIndex(), contributorPart.getEndIndex(), true);
                    }
                    if (!startIndexes.isEmpty()) {
                        seaView.modelEssaysPane.showText((int) startIndexes.get(0));
                    }
                }
            }*/
        }
    }

    /**
     * Used to order SCU-EDU pairs
     */
    private class SCUEDUPair implements Comparable<SCUEDUPair>{
        private SCUTree eduTree;
        private DefaultMutableTreeNode scu;

        public SCUEDUPair(SCUTree eduTree, DefaultMutableTreeNode scu) {
            this.eduTree = eduTree;
            this.scu = scu;
        }

        public SCUTree getEDUTree() {
            return eduTree;
        }

        public DefaultMutableTreeNode getSCU() {
            return scu;
        }

        @Override
        public int compareTo(SCUEDUPair other) {
            SCUContributor thisContributor = (SCUContributor) this.eduTree.getRootNode().getNextNode().getFirstLeaf().getUserObject();
            SCUContributor otherContributor = (SCUContributor) other.eduTree.getRootNode().getNextNode().getFirstLeaf().getUserObject();

            if (thisContributor.getSCUContributorPart(0).getStartIndex() < otherContributor.getSCUContributorPart(0).getStartIndex()) {
                return -1;
            }
            else if (thisContributor.getSCUContributorPart(0).getStartIndex() > otherContributor.getSCUContributorPart(0).getStartIndex()) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }
}

package SEAView;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.Autoscroll;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class SCUTree extends JTree implements TreeSelectionListener, Comparator, Autoscroll, TreeExpansionListener {
    private DefaultTreeModel treeModel;
    private SEAView seaView;
    private ScuEduTextPane textPane = null;
    private ScuEduTextPane pyramidReferenceTextPane = null;
    private boolean noScrollOnNextNodeSelection = false;
    private Vector highlightedNodes = new Vector();
    private DataFlavor treeNodeFlavor = null;
    private final String treeNodeMimeType = "application/x-java-jvm-local-objectref; class=javax.swing.tree.DefaultMutableTreeNode";
    protected boolean isEduTree = true; // Allow node selections to enable the "Change Label" button

    public SCUTree(SEAView seaView) {
        this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Root Node"));
        setModel(this.treeModel);
        this.seaView = seaView;
        setRootVisible(false);
        setShowsRootHandles(true);
        getSelectionModel().setSelectionMode(1);
        setEditable(false);
        addTreeSelectionListener(this);
        addTreeExpansionListener(this);
        setCellRenderer(new SCUTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(this);

        setDragEnabled(true);
        try {
            treeNodeFlavor = new DataFlavor(treeNodeMimeType);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        setTransferHandler(new SCUTreeTransferHandler());
    }

    public void reset() {
        this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Root Node"));
        setModel(this.treeModel);
    }

    public void rebuildTree(javax.swing.tree.TreeNode root) {
        this.treeModel.setRoot(root);
        //expandTree();
    }

    public void expandTree() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
        scrollRowToVisible(0);
    }

    public void collapseTree() {
        for (int i = 0; i < getRowCount(); i++) {
            collapseRow(i);
        }
        scrollRowToVisible(0);
    }

    public void setSCUTextPane(ScuEduTextPane textPane) {
        this.textPane = textPane;
    }

    public void setPyramidReferenceTextPane(ScuEduTextPane pyramidReferenceTextPane) {
        this.pyramidReferenceTextPane = pyramidReferenceTextPane;
    }

    public DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) this.treeModel.getRoot();
    }

    public void insertNodeInto(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent) {
        insertNodeInto(newChild, parent, parent.getChildCount());
    }

    public void insertNodeInto(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent, int index) {
        //this.noScrollOnNextNodeSelection = true;
        this.treeModel.insertNodeInto(newChild, parent, index);
        scrollPathToVisible(new TreePath(newChild.getPath()));
    }

    public void nodeChanged(javax.swing.tree.TreeNode node) {
        this.treeModel.nodeChanged(node);
    }

    public void removeNodeFromParent(DefaultMutableTreeNode node) {
        this.treeModel.removeNodeFromParent(node);
    }

    public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();

        while (this.highlightedNodes.size() > 0) {
            DefaultMutableTreeNode highlightedNode = (DefaultMutableTreeNode) this.highlightedNodes.remove(0);

            nodeChanged(highlightedNode);
        }

        this.textPane.modifyTextHighlight(0, this.textPane.getText().length() - 1, false);
        if (this.pyramidReferenceTextPane != null) {
            this.pyramidReferenceTextPane.modifyTextHighlight(0, this.pyramidReferenceTextPane.getText().length() - 1, false);
        }
        if (node == null) {
            seaView.getChangeLabelBtn().setEnabled(false);
            seaView.getRemoveBtn().setEnabled(false);
        }
        else {
            if (isEduTree) {
                seaView.getRemoveBtn().setEnabled(true);
                if (node.getLevel() == 1) {
                    seaView.getChangeLabelBtn().setEnabled(true);
                }
                else {
                    seaView.getChangeLabelBtn().setEnabled(false);
                }
            }

            if (this.pyramidReferenceTextPane != null) {
                SCU scu = (SCU) ((DefaultMutableTreeNode) node.getPath()[1]).getUserObject();
                if (scu.getId() != 0) {

                    ArrayList highlightIndexes = new ArrayList();
                    Enumeration pyramidSCUs = this.seaView.pyramidTree.getRootNode().children();
                    while (pyramidSCUs.hasMoreElements()) {
                        DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) pyramidSCUs.nextElement();

                        if (((SCU) scuNode.getUserObject()).getId() == scu.getId()) {

                            Enumeration pyramidScuContributorNodeEnum = scuNode.children();
                            while (pyramidScuContributorNodeEnum.hasMoreElements()) {
                                Iterator pyramidSCUContributorIterator = ((SCUContributor) ((DefaultMutableTreeNode) pyramidScuContributorNodeEnum.nextElement()).getUserObject()).elements();


                                while (pyramidSCUContributorIterator.hasNext()) {

                                    SCUContributorPart scuContributorPart = (SCUContributorPart) pyramidSCUContributorIterator.next();

                                    this.pyramidReferenceTextPane.modifyTextHighlight(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);


                                    highlightIndexes.add(new Integer(scuContributorPart.getStartIndex()));
                                }
                            }

                            java.util.Collections.sort(highlightIndexes);
                            this.seaView.essayPaneHighlightIndexes = new int[highlightIndexes.size()];

                            for (int i = 0; i < highlightIndexes.size(); i++) {
                                this.seaView.essayPaneHighlightIndexes[i] = ((Integer) highlightIndexes.get(i)).intValue();
                            }

                            this.seaView.currentEssayPaneHighlightIndex = 0;
                            this.pyramidReferenceTextPane.showText(this.seaView.essayPaneHighlightIndexes[0]);

                            break;
                        }
                    }
                }
            }

            int smallestHighlightIndex = Integer.MAX_VALUE;

            if (node.getLevel() == 1) {
                Enumeration nodeEnum = node.children();
                while (nodeEnum.hasMoreElements()) {
                    DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) nodeEnum.nextElement();

                    Iterator iterator = ((SCUContributor) childNode.getUserObject()).elements();
                    while (iterator.hasNext()) {
                        SCUContributorPart scuContributorPart = (SCUContributorPart) iterator.next();
                        this.textPane.modifyTextHighlight(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);

                        smallestHighlightIndex = Math.min(smallestHighlightIndex, scuContributorPart.getStartIndex());
                    }

                }
            } else if (node.getLevel() == 2) {
                Iterator iterator = ((SCUContributor) node.getUserObject()).elements();
                while (iterator.hasNext()) {
                    SCUContributorPart scuContributorPart = (SCUContributorPart) iterator.next();
                    this.textPane.modifyTextHighlight(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);

                    smallestHighlightIndex = Math.min(smallestHighlightIndex, scuContributorPart.getStartIndex());
                }

            } else {
                SCUContributorPart scuContributorPart = (SCUContributorPart) node.getUserObject();

                this.textPane.modifyTextHighlight(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);

                smallestHighlightIndex = scuContributorPart.getStartIndex();
            }


            if ((this.textPane.getSelectedText() == null) && (smallestHighlightIndex != Integer.MAX_VALUE) && (!this.noScrollOnNextNodeSelection)) {


                this.textPane.showText(smallestHighlightIndex);
            }
            this.noScrollOnNextNodeSelection = false;
        }
    }

    public void order() {
        ArrayList scuNodeList = new ArrayList();
        DefaultMutableTreeNode rootNode = getRootNode();
        Enumeration scuNodeEnum = rootNode.children();
        while (scuNodeEnum.hasMoreElements()) {
            scuNodeList.add(scuNodeEnum.nextElement());
        }
        java.util.Collections.sort(scuNodeList, this);
        Iterator scuNodeIterator = scuNodeList.iterator();
        int scuNum = 1;
        while (scuNodeIterator.hasNext()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) scuNodeIterator.next();
            removeNodeFromParent(node);

            // Update tempId in tree
            SCU scu = (SCU) node.getUserObject();
            scu.setScuTempId(scuNum);
            scuNum++;
            insertNodeInto(node, rootNode);
        }
        this.seaView.table.updateSCUIndices();
    }

    public void orderAlphabetically() {
        ArrayList scuNodeList = new ArrayList();
        DefaultMutableTreeNode rootNode = getRootNode();
        Enumeration scuNodeEnum = rootNode.children();
        while (scuNodeEnum.hasMoreElements()) {
            scuNodeList.add(scuNodeEnum.nextElement());
        }
        Collections.sort(scuNodeList, new Comparator<DefaultMutableTreeNode>() {
            public int compare(DefaultMutableTreeNode node1, DefaultMutableTreeNode node2)
            {
                SCU scu1 = (SCU) node1.getUserObject();
                SCU scu2 = (SCU) node2.getUserObject();
                String label1 = scu1.getLabel().replaceAll("^\\([0-9]\\)\\s*", "");
                String label2 = scu2.getLabel().replaceAll("^\\([0-9]\\)\\s*", "");


                return label1.compareToIgnoreCase(label2);
            }
        });
        Iterator scuNodeIterator = scuNodeList.iterator();
        int scuNum = 1;
        while (scuNodeIterator.hasNext()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) scuNodeIterator.next();
            removeNodeFromParent(node);

            // Update tempId in tree
            SCU scu = (SCU) node.getUserObject();
            scu.setScuTempId(scuNum);
            scuNum++;
            insertNodeInto(node, rootNode);
        }
        this.seaView.table.updateSCUIndices();
    }

    public int compare(Object o1, Object o2) {
        DefaultMutableTreeNode n1 = (DefaultMutableTreeNode) o1;
        DefaultMutableTreeNode n2 = (DefaultMutableTreeNode) o2;

        if (n1.getChildCount() < n2.getChildCount()) {
            return 1;
        }
        if (n1.getChildCount() > n2.getChildCount()) {
            return -1;
        }


        SCU scu1 = (SCU) n1.getUserObject();
        SCU scu2 = (SCU) n2.getUserObject();
        Pattern p = Pattern.compile("^\\((\\d+)\\) ");
        Matcher m1 = p.matcher(scu1.toString());
        Matcher m2 = p.matcher(scu2.toString());
        if ((m1.lookingAt()) && (m2.lookingAt())) {
            int num1 = Integer.parseInt(m1.group(1));
            int num2 = Integer.parseInt(m2.group(1));
            if (num1 < num2) {
                return 1;
            }
            if (num2 < num1) {
                return -1;
            }


            return 0;
        }


        return scu1.toString().compareToIgnoreCase(scu2.toString());
    }

    public Vector getSCUNodesContainingIndex(int index) {
        Vector scuNodes = new Vector();

        Enumeration scuNodeEnum = getRootNode().children();
        label133:
        while (scuNodeEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodeEnum.nextElement();

            Enumeration scuContributorNodeEnum = scuNode.children();

            while (scuContributorNodeEnum.hasMoreElements()) {
                SCUContributor scuContributor = (SCUContributor) ((DefaultMutableTreeNode) scuContributorNodeEnum.nextElement()).getUserObject();


                for (int i = 0; i < scuContributor.getNumParts(); i++) {
                    SCUContributorPart scuContributorPart = scuContributor.getSCUContributorPart(i);

                    if ((scuContributorPart.getStartIndex() <= index) && (scuContributorPart.getEndIndex() >= index)) {

                        scuNodes.add(scuNode.getUserObject());

                        break label133;
                    }
                }
            }
        }
        return scuNodes;
    }

    public SCU getSCUsByID(int id) {
        Enumeration scuNodeEnum = getRootNode().children();
        while (scuNodeEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodeEnum.nextElement();
            SCU scu = (SCU) scuNode.getUserObject();
            if (scu.getId() == id) {
                return scu;
            }
        }
        return null;
    }

    public SCU getSCUsByTempID(String tempId) {
        Enumeration scuNodeEnum = getRootNode().children();
        while (scuNodeEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodeEnum.nextElement();
            SCU scu = (SCU) scuNode.getUserObject();
            if (scu.getTempId().equals(tempId)) {
                return scu;
            }
        }
        return null;
    }

    public void selectSCUNode(int scuId) {
        Enumeration scuNodeEnum = getRootNode().children();
        while (scuNodeEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodeEnum.nextElement();


            SCU scu = (SCU) scuNode.getUserObject();

            if (scu.getId() == scuId) {
                final TreePath path = new TreePath(scuNode.getPath());
                //this.noScrollOnNextNodeSelection = true;
                setSelectionPath(path);


                scrollRowToVisible(getRowCount() - 1);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    //private final TreePath val$path;

                    public void run() {
                        SCUTree.this.scrollPathToVisible(path);
                    }
                });
                break;
            }
        }
    }

    public int highlightSCUsNodesWithLabelmatchingPattern(Pattern pattern) {
        this.highlightedNodes.removeAllElements();
        Enumeration scuNodeEnum = getRootNode().children();
        while (scuNodeEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodeEnum.nextElement();


            Matcher matcher = pattern.matcher(scuNode.toString());
            if (matcher.find()) {
                this.highlightedNodes.add(scuNode);
                nodeChanged(scuNode);
            }
        }
        return this.highlightedNodes.size();
    }

    public java.awt.Insets getAutoscrollInsets() {
        int margin = 10;
        Rectangle outer = getBounds();
        Rectangle inner = getParent().getBounds();
        return new java.awt.Insets(inner.y - outer.y + margin, inner.x - outer.x + margin, outer.height - inner.height - inner.y + outer.y + margin, outer.width - inner.width - inner.x + outer.x + margin);
    }

    public void autoscroll(Point p) {
        int realrow = getClosestRowForLocation(p.x, p.y);
        Rectangle outer = getBounds();


        realrow = realrow < getRowCount() - 3 ? realrow + 3 : p.y + outer.y <= 10 ? realrow - 3 : realrow < 3 ? 0 : realrow;

        scrollRowToVisible(realrow);
    }

    /**
     * Overrides the setFont method so the row height in the tree is adjusted
     * to accommodate changes in font size
     */
    public void setFont (Font font) {
        super.setFont(font);
        FontMetrics metrics = getFontMetrics(getFont());
        setRowHeight(metrics.getHeight());
    }

    public void setEduTree(boolean isEduTree) {
        this.isEduTree = isEduTree;
    }

    public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
    }

    public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
        /*JButton btn = this.seaView.getCollapseBtn();
        btn.setText("Collapse");
        btn.setMnemonic('l');
        btn.setActionCommand("collapse");*/
    }

    private class SCUTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        public SCUTreeCellRenderer() {
            setClosedIcon(null);
            setOpenIcon(null);
            setLeafIcon(null);
        }


        public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);


            setFont(tree.getFont());

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

            if (node.getLevel() == 1) {
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setFont(getFont().deriveFont(Font.PLAIN));
            }
            if ((node.getLevel() == 1) && (node.toString().contains("All non-matching SCUs go here"))) {
                setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.yellow));
            } else {
                setBorder(null);
            }

            if (SCUTree.this.highlightedNodes.contains(node)) {
                setForeground(java.awt.Color.magenta);
            }


            Object userObject = node.getUserObject();

            String comment = null;

            if ((userObject instanceof SCU)) {
                comment = ((SCU) userObject).getComment();
            } else if ((userObject instanceof SCUContributor)) {
                comment = ((SCUContributor) userObject).getComment();
            }
            setToolTipText(null);
            if ((comment != null) && (comment.length() > 0)) {
                setToolTipText(comment);
                setText(getText() + " *");
            }

            return this;
        }
    }

    private class SCUTreeTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            SCUTree tree = (SCUTree) c;
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            Transferable transferable = new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{treeNodeFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return treeNodeFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return selectedNode;
                }

            };

            return transferable;
        }

        @Override
        public int getSourceActions(JComponent c) {
            if (SCUTree.this.isEduTree) {
                return MOVE;
            }
            else {
                return COPY;
            }
        }

        /*@Override
        public boolean importData(TransferSupport support) {
            if (!SCUTree.this.isEduTree) {
                SCUTree.this.seaView.table.remove();
            }
            return true;
        }*/

        /*@Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (SCUTree.this.isEduTree) {
                DefaultMutableTreeNode topNode = SCUTree.this.getRootNode().getNextNode();
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) SCUTree.this.getLastSelectedPathComponent();
                if (topNode.getChildCount() == 1 || selectedNode == topNode) {
                    SCUTree.this.setModel(null);
                }
                else {
                    SCUTree.this.treeModel.removeNodeFromParent(selectedNode);
                    SCUTree.this.treeModel.reload();
                    SCUTree.this.expandTree();
                }
                //DefaultTableModel model = (DefaultTableModel) SCUTree.this.seaView.table.getModel();
                //model.fireTableDataChanged();
            }
            super.exportDone(source, data, action);
        }*/

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if (SCUTree.this.isEduTree) {
                SCUTree.this.seaView.table.remove();
            }
        }
    }
}
package SEAView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SEAView extends JFrame implements ActionListener, ErrorHandler {
    private static final String eol = System.getProperty("line.separator");
    private static final String titleString = "SEAView";
    private static final String helpAbout = "v. 1.0 (c) 2019\nPennsylvania State University\nnlp.at.pennstate@gmail.com";
    protected boolean isCrowdLoaded = false;
    protected boolean isPeerLoaded = false;
    private boolean isCrowdModified = false;
    private boolean isPeerModified = false;
    private int currentTextSize;
    protected int[] essayPaneHighlightIndexes = null;
    protected int currentEssayPaneHighlightIndex = 0;
    private File crowdFile = null;
    private File peerFile = null;
    private JButton sortTableBtn;
    private JButton changeLabelBtn;
    private JButton removeBtn;
    private JButton orderByWeightBtn;
    private JButton orderAlphabeticallyBtn;
    private JButton expandCollapseBtn;
    private JButton showModelEssaysBtn;
    private JDialog SEADialog;
    private JDialog modelEssaysDialog;
    private JTable SEADialogTable;
    private JLabel statusLbl; // Displays messages at bottom of DucView window
    private JMenuItem fileNewCrowdMenuItem;
    private JMenuItem fileLoadCrowdMenuItem;
    private JMenuItem fileSaveCrowdMenuItem;
    private JMenuItem fileSaveCrowdAsMenuItem;
    private JMenuItem fileShowSCUEDUAlignmentMenuItem;
    private JMenuItem fileCloseCrowdMenuItem;
    private JMenuItem fileNewPeerMenuItem;
    private JMenuItem fileLoadPeerMenuItem;
    private JMenuItem fileSavePeerMenuItem;
    private JMenuItem fileSavePeerAsMenuItem;
    private JMenuItem fileShowPeerSCUEDUAlignmentMenuItem;
    private JMenuItem fileClosePeerMenuItem;
    private JMenuItem fileExitMenuItem;
    private JMenuItem editFindMenuItem;
    private JMenuItem editUndoMenuItem;
    private JMenuItem editRedoMenuItem;
    private JMenuItem documentStartRegexMenuItem;
    private JMenuItem summaryDividerRegexMenuItem;
    private JMenuItem dndLeftClickMenuItem;
    private JMenuItem dndRightClickMenuItem;
    private JRadioButtonMenuItem setLabelOnInsertionMenuItem;
    private JRadioButtonMenuItem setLabelAfterInsertionMenuItem;
    protected SEAViewTextPane essayPane;
    protected SEAViewTextPane modelEssaysPane;
    private JPanel mainPanel;
    private JPanel pyramidPanel;
    private JScrollPane pyramidScrollPane;
    protected SCUTree pyramidTree;
    protected SCUTree peerTree;
    protected SEATable table;
    private UndoController undoController = new UndoController();
    private String defaultFilePath;
    private String crowdInputFile = null;
    private String peerInputFile = null;
    private String headerRegEx = null; // Regular expression that delimits model essays
    private int[] startDocumentIndexes = null; // Indices at which model essays begin
    private String bodyRegEx = null; // Regular expression that delimits the summary from the body of an essay
    private int[] startBodyIndexes = null; // Indices at which essays begin

    public SEAView() {
        super(titleString);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new SEAViewWindowAdapter(this));
        setResizable(true);

        String path = SEAView.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = null;
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (decodedPath != null) {
            defaultFilePath = decodedPath;
        }
        else {
            defaultFilePath = System.getProperty("user.dir");
        }

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        this.currentTextSize = new JTextPane().getFont().getSize();

        contentPane.registerKeyboardAction(this, "find", KeyStroke.getKeyStroke(70, InputEvent.CTRL_DOWN_MASK), 2); // Ctrl + F shortcut
        contentPane.registerKeyboardAction(this, "undo", KeyStroke.getKeyStroke(90, InputEvent.CTRL_DOWN_MASK), 2); // Ctrl + Z shortcut
        contentPane.registerKeyboardAction(this, "redo", KeyStroke.getKeyStroke(89, InputEvent.CTRL_DOWN_MASK), 2); // Ctrl + Y shortcut

        setJMenuBar(createMenuBar());

        this.mainPanel = new JPanel(new CardLayout());

        this.essayPane = new SEAViewTextPane(this);
        this.essayPane.setTransferHandler(new RemovalHandler());

        this.modelEssaysDialog = new JDialog(this, "Model Essays");
        //JPanel modelEssaysPanel = new JPanel();
        this.modelEssaysPane = new SEAViewTextPane(this);
        // modelEssaysPanel.add(new JScrollPane(this.modelEssaysPane));
        modelEssaysDialog.add(new JScrollPane(this.modelEssaysPane));
        this.modelEssaysDialog.pack();

        JPanel tablePanel = new JPanel();
        tablePanel.setPreferredSize(new Dimension(250, 500));
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));

        Box tableButtonsPanel = Box.createHorizontalBox();
        tableButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        /*JPanel tableButtonsPanel = new JPanel(new GridLayout(1, 2));
        tableButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));*/

        this.sortTableBtn = new JButton("Sort EDUs");
        this.sortTableBtn.setMnemonic('r');
        this.sortTableBtn.setActionCommand("sort");
        this.sortTableBtn.addActionListener(this);
        this.sortTableBtn.setEnabled(false);
        tableButtonsPanel.add(this.sortTableBtn);

        this.changeLabelBtn = new JButton("Change label");
        this.changeLabelBtn.setMnemonic('c');
        this.changeLabelBtn.setActionCommand("changeLabel");
        this.changeLabelBtn.addActionListener(this);
        this.changeLabelBtn.setEnabled(false);
        tableButtonsPanel.add(this.changeLabelBtn);

        this.removeBtn = new JButton("Remove");
        this.removeBtn.setMnemonic('e');
        this.removeBtn.setActionCommand("remove");
        this.removeBtn.addActionListener(this);
        this.removeBtn.setEnabled(false);
        tableButtonsPanel.add(this.removeBtn);

        tablePanel.add(tableButtonsPanel);

        this.table = new SEATable(this);
        tablePanel.add(new JScrollPane(this.table));


        this.pyramidPanel = new JPanel();
        pyramidPanel.setPreferredSize(new Dimension(250, 500));
        pyramidPanel.setLayout(new BoxLayout(pyramidPanel, BoxLayout.Y_AXIS));

        /*JPanel pyramidButtonsPanel = new JPanel(new GridLayout(2, 2));
        pyramidButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));*/

        Box pyramidButtonsPanelRow1 = Box.createHorizontalBox();
        pyramidButtonsPanelRow1.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

        Box pyramidButtonsPanelRow2 = Box.createHorizontalBox();
        pyramidButtonsPanelRow2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.expandCollapseBtn = new JButton(" Expand ");
        this.expandCollapseBtn.setMnemonic('p');
        this.expandCollapseBtn.setActionCommand("expand");
        this.expandCollapseBtn.addActionListener(this);
        this.expandCollapseBtn.setEnabled(false);
        pyramidButtonsPanelRow1.add(this.expandCollapseBtn);

        this.showModelEssaysBtn = new JButton("Show model essays");
        this.showModelEssaysBtn.setMnemonic('s');
        this.showModelEssaysBtn.setActionCommand("showModelEssays");
        this.showModelEssaysBtn.addActionListener(this);
        this.showModelEssaysBtn.setEnabled(false);
        pyramidButtonsPanelRow1.add(this.showModelEssaysBtn);

        this.orderByWeightBtn = new JButton("Order by Weight");
        this.orderByWeightBtn.setMnemonic('w');
        this.orderByWeightBtn.setActionCommand("orderByWeight");
        this.orderByWeightBtn.addActionListener(this);
        this.orderByWeightBtn.setEnabled(false);
        pyramidButtonsPanelRow2.add(this.orderByWeightBtn);

        this.orderAlphabeticallyBtn = new JButton("Order Alphabetically");
        this.orderAlphabeticallyBtn.setMnemonic('a');
        this.orderAlphabeticallyBtn.setActionCommand("orderAlphabetically");
        this.orderAlphabeticallyBtn.addActionListener(this);
        this.orderAlphabeticallyBtn.setEnabled(false);
        pyramidButtonsPanelRow2.add(this.orderAlphabeticallyBtn);

        pyramidPanel.add(pyramidButtonsPanelRow1);
        pyramidPanel.add(pyramidButtonsPanelRow2);

        this.pyramidTree = new SCUTree(this);
        this.pyramidTree.setSCUTextPane(this.essayPane);
        this.pyramidTree.setEduTree(false);
        //this.pyramidTree.setPyramidReferenceTextPane(essayPane);

        this.peerTree = new SCUTree(this);
        this.peerTree.setSCUTextPane(this.essayPane);
        this.peerTree.setEduTree(false);
        this.peerTree.setPyramidReferenceTextPane(this.modelEssaysPane);
        this.modelEssaysPane.setTree(this.peerTree);


        this.essayPane.setTree(this.pyramidTree);

        this.pyramidScrollPane = new JScrollPane(this.pyramidTree);
        pyramidPanel.add(pyramidScrollPane);

        JSplitPane splitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, pyramidPanel);

        splitPane1.setResizeWeight(1.0D);

        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(this.essayPane), splitPane1);


        splitPane2.setResizeWeight(0.0D);

        this.mainPanel.add(splitPane2);

        contentPane.add(this.mainPanel, "Center");

        this.statusLbl = new JLabel("Ready");
        this.statusLbl.setBorder(BorderFactory.createBevelBorder(1));
        this.statusLbl.setPreferredSize(new Dimension(1, 24));
        this.statusLbl.setFont(this.statusLbl.getFont().deriveFont(14.0f));
        this.statusLbl.setForeground(new Color(0,0,153));
        contentPane.add(this.statusLbl, "South");

        pack();

        splitPane2.setDividerLocation(Math.round(((float) splitPane2.getSize().width - splitPane2.getInsets().left - splitPane2.getInsets().right) / 3 + splitPane2.getInsets().left));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(screenSize.width / 2 - getWidth() / 2, screenSize.height / 2 - getHeight() / 2, getWidth(), getHeight());

        this.SEADialog = new JDialog(this, "SCU-EDU Alignment");

        this.SEADialogTable = new JTable() {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                int height = getPreferredSize().height;
                int width = getPreferredSize().width;
                return new Dimension(width, height);
            }
        };
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.setColumnIdentifiers(new Object[] { "<html><b>SCU</b></html>", "<html><b>EDU(s)</b></html>", "<html><b># of EDUs</b></html>" });
        this.SEADialogTable.setModel(tableModel);

        // Enable sorting by SCU weight or # of EDUs
        TableRowSorter sorter = new TableRowSorter();
        this.SEADialogTable.setRowSorter(sorter);
        sorter.setModel(this.SEADialogTable.getModel());
        Comparator comparator = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                // Get SCU weights
                String w1 = s1.split("w = ")[1];
                String comp1 = w1.substring(0, w1.indexOf(")"));
                String w2 = s2.split("w = ")[1];
                String comp2 = w2.substring(0, w2.indexOf(")"));
                return Integer.compare(Integer.parseInt(comp1), Integer.parseInt(comp2));
            }
        };
        sorter.setComparator(0, comparator);



        this.SEADialog.add(new JScrollPane(this.SEADialogTable));
        this.SEADialog.pack();
    }

    public static void main(String[] args) throws Exception {
        SEAView seaView = new SEAView();
        seaView.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("fileNewCrowd")) {
            if ((!this.isCrowdModified) || (saveModifiedCrowd()) && (!this.isPeerModified) || (saveModifiedPeer())) {
                SEAViewFileChooser chooser = new SEAViewFileChooser(false, true, true);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setDialogTitle("Choose the initial pyramid file");
                if (chooser.showOpenDialog(this) == 0) {
                    try {
                        this.crowdInputFile = chooser.getSelectedFile().getName();

                        clearDocumentRegex();

                        loadEssay(chooser.getSelectedFile(), false, true);
                        loadPyramid(chooser.getSelectedFile(), false, essayPane);

                        this.setTitle(titleString + " - SEA Annotation: " + chooser.getSelectedFile());
                        msg("Loaded file " + chooser.getSelectedFile());
                        this.defaultFilePath = chooser.getSelectedFile().getCanonicalPath();
                        this.crowdFile = null;
                        setCrowdLoaded(true);

                    } catch (IOException | SAXException | ParserConfigurationException ex) {
                        ex.printStackTrace();
                        msg(ex.getMessage());
                    }
                }
            }
        }
        else if (e.getActionCommand().equals("fileLoadCrowd")) {
            if ((!this.isCrowdModified) || (saveModifiedCrowd()) && (!this.isPeerModified) || (saveModifiedPeer())) {
                SEAViewFileChooser chooser = new SEAViewFileChooser(false, true, false);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setDialogTitle("Choose the SEA Annotation file");
                if (chooser.showOpenDialog(this) == 0) {
                    try {
                        clearDocumentRegex();

                        loadEssay(chooser.getSelectedFile(), false, true);
                        loadPyramid(chooser.getSelectedFile(), false, essayPane);
                        loadTable(chooser.getSelectedFile(), false, false);

                        this.setTitle(titleString + " - SEA Annotation: " + chooser.getSelectedFile());
                        msg("Loaded file " + chooser.getSelectedFile());
                        this.defaultFilePath = chooser.getSelectedFile().getCanonicalPath();
                        this.crowdFile = chooser.getSelectedFile();
                        setCrowdLoaded(true);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        msg(ex.getMessage());
                    }
                }
            }
        }
        else if (e.getActionCommand().equals("fileSaveCrowd")) {
            if (this.crowdFile == null) {
                saveCrowd(true);
            } else {
                saveCrowd(false);
            }
        }
        else if (e.getActionCommand().equals("fileSaveCrowdAs")) {
            saveCrowd(true);
        }
        else if (e.getActionCommand().equals("fileShowSCUEDUAlignment")) {
            Map<String, List<String>>alignment = table.orderEDUs(true, isPeerLoaded);
            if (alignment != null && Collections.frequency(alignment.values(), null) != alignment.size()) {
                TreeMap sortedAlignment = new TreeMap<String, List<String>>(
                        new Comparator<String>() {
                            @Override
                            public int compare(String s1, String s2) {
                                return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                            }
                        });
                sortedAlignment.putAll(alignment);
                Set keySet = sortedAlignment.keySet();
                Iterator iterator = keySet.iterator();

                DefaultTableModel model = (DefaultTableModel) SEADialogTable.getModel();
                model.setRowCount(0); // Clear old data
                while (iterator.hasNext()) {
                    String scu = (String) iterator.next();
                    int weight = pyramidTree.getSCUsByTempID(scu).getWeight();
                    List edus = (List) sortedAlignment.get(scu);
                    String formattedEdus = edus.toString().replace("[", "").replace("]", "");
                    model.addRow(new Object[] { scu + " (w = " + weight + ")", formattedEdus, edus.size() });
                }
                SEADialog.pack();
                SEADialog.setVisible(true);
            }
        }
        else if (e.getActionCommand().equals("fileCloseCrowd")) {
            if ((!this.isCrowdModified) || (saveModifiedCrowd())) {
                this.essayPane.loadText("");
                this.table.clear();
                this.pyramidTree.reset();
                setCrowdLoaded(false);
                setTitle(titleString);
                msg("Closed " + this.defaultFilePath);
            }
        }
        else if (e.getActionCommand().equals("fileNewPeer")) {
            if ((!this.isPeerModified) || (saveModifiedPeer()) && (!this.isCrowdModified) || (saveModifiedCrowd())) {
                SEAViewFileChooser chooser = new SEAViewFileChooser(false, false, true);
                chooser.setDialogTitle("Choose the initial peer annotation file");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (chooser.showOpenDialog(this) == 0) {
                    try {
                        clearDocumentRegex();

                        setPeerLoaded(true);
                        table.clear();
                        loadEssay(chooser.getSelectedFile(), true, true);
                        loadPyramid(chooser.getSelectedFile(), false, modelEssaysPane);
                        loadPyramid(chooser.getSelectedFile(), true, essayPane);

                        this.essayPane.setTree(this.peerTree);

                        startDocumentIndexes = null;
                        startBodyIndexes = null;
                        //initializeStartDocumentIndexes(bodyRegEx, false);

                        this.setTitle(titleString + " - SEP Annotation: " + chooser.getSelectedFile());
                        msg("Loaded file " + chooser.getSelectedFile());
                        this.defaultFilePath = chooser.getSelectedFile().getCanonicalPath();
                        this.peerFile = chooser.getSelectedFile();
                    } catch (IOException | SAXException | ParserConfigurationException ex) {
                        ex.printStackTrace();
                        msg(ex.getMessage());
                    }
                }
            }
        }
        else if (e.getActionCommand().equals("fileLoadPeer")) {
            if (((!this.isPeerModified) || (saveModifiedPeer())) && ((!this.isCrowdModified) || (saveModifiedCrowd()))) {
                SEAViewFileChooser chooser = new SEAViewFileChooser(false, false, false);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setDialogTitle("Choose the SEP Annotation file");
                if (chooser.showOpenDialog(this) == 0) {
                    try {
                        clearDocumentRegex();

                        setPeerLoaded(true);
                        table.clear();
                        loadEssay(chooser.getSelectedFile(), true, true);
                        loadPyramid(chooser.getSelectedFile(), false, modelEssaysPane);
                        loadPyramid(chooser.getSelectedFile(), true, essayPane);
                        loadTable(chooser.getSelectedFile(), true, false);

                        this.essayPane.setTree(this.peerTree);


                        this.setTitle(titleString + " - SEP Annotation: " + chooser.getSelectedFile());
                        msg("Loaded file " + chooser.getSelectedFile());
                        this.defaultFilePath = chooser.getSelectedFile().getCanonicalPath();
                        this.peerFile = chooser.getSelectedFile();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        msg(ex.getMessage());
                    }
                }
            }
        }
        else if (e.getActionCommand().equals("fileSavePeer")) {
            if (this.peerFile == null) {
                savePeer(true);
            } else {
                savePeer(false);
            }
        }
        else if (e.getActionCommand().equals("fileSavePeerAs")) {
            savePeer(true);
        }
        else if (e.getActionCommand().equals("fileClosePeer")) {
            if ((!this.isPeerModified) || (saveModifiedPeer())) {
                this.essayPane.loadText("");
                this.modelEssaysPane.loadText("");
                this.table.clear();
                this.pyramidTree.reset();
                this.peerTree.reset();
                setPeerLoaded(false);
                setTitle(titleString);
                msg("Closed " + this.defaultFilePath);
            }
        }
        else if (e.getActionCommand().equals("exit")) {
            WindowListener[] listeners = getWindowListeners();
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].windowClosing(null);
            }
        }
        else if (e.getActionCommand().equals("find")) {

        }
        else if ((e.getActionCommand().equals("undo")) || (e.getActionCommand().equals("redo"))) {
            //UndoController undoController;
            /*DefaultTableModel tableModel;
            SCUTree tree;
            SEAViewTextPane textPane = this.essayPane;
            UndoController undoController = this.undoController;
            if (this.isPeerLoaded) {
                tableModel = this.table.getPeerModel();
                tree = this.peerTree;

            } else {
                tableModel = this.table.getTableModel();
                tree = this.pyramidTree;
            }
            //javax.swing.tree.DefaultMutableTreeNode rootNode;
            javax.swing.tree.DefaultMutableTreeNode rootNode;
            if (e.getActionCommand().equals("undo")) {
                rootNode = (javax.swing.tree.DefaultMutableTreeNode) undoController.undo();
            } else {
                rootNode = (javax.swing.tree.DefaultMutableTreeNode) undoController.redo();
            }
            tree.rebuildTree(rootNode);
            textPane.loadText(textPane.getText());


            java.util.Enumeration scuNodeEnum = tree.getRootNode().children();
            while (scuNodeEnum.hasMoreElements()) {
                javax.swing.tree.DefaultMutableTreeNode scuNode = (javax.swing.tree.DefaultMutableTreeNode) scuNodeEnum.nextElement();
                java.util.Enumeration scuContributorNodeEnum = scuNode.children();

                while (scuContributorNodeEnum.hasMoreElements()) {
                    SCUContributor scuContributor = (SCUContributor) ((javax.swing.tree.DefaultMutableTreeNode) scuContributorNodeEnum.nextElement()).getUserObject();


                    for (int i = 0; i < scuContributor.getNumParts(); i++) {
                        SCUContributorPart scuContributorPart = scuContributor.getSCUContributorPart(i);
                        textPane.modifyTextSelection(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);
                    }
                }
            }

            if (this.isPeerLoaded) {
                this.isPeerModified = true;
                this.fileSavePeerMenuItem.setEnabled(true);
                this.fileSavePeerAsMenuItem.setEnabled(true);
            } else {
                this.isCrowdModified = true;
                this.fileSaveCrowdMenuItem.setEnabled(true);
                this.fileSaveCrowdAsMenuItem.setEnabled(true);
            }*/
        }
        else if (e.getActionCommand().startsWith("Text Size")) {
            float fontSize = Float.parseFloat(e.getActionCommand().substring(10));
            this.essayPane.setFont(this.essayPane.getFont().deriveFont(fontSize));
            this.modelEssaysPane.setFont(this.modelEssaysPane.getFont().deriveFont(fontSize));
            this.pyramidTree.setFont(this.pyramidTree.getFont().deriveFont(fontSize));
            this.peerTree.setFont(this.peerTree.getFont().deriveFont(fontSize));
            this.pyramidTree.revalidate();
            this.peerTree.revalidate();
            this.table.setFont(this.table.getFont().deriveFont(fontSize));
        }
        else if (e.getActionCommand().startsWith("Look And Feel")) {
            try {
                javax.swing.UIManager.setLookAndFeel(e.getActionCommand().substring(14));
            } catch (Exception ex) {
                msg(ex.getMessage());
            }
            SwingUtilities.updateComponentTreeUI(this);
            this.essayPane.updateSelectedStyle();
        }
        else if (e.getActionCommand().startsWith("setLabel")) {
            table.setLabelMode(e.getActionCommand().endsWith("Now"));
        }
        else if (e.getActionCommand().startsWith("drag")) {
            essayPane.setRightClickMode(e.getActionCommand().endsWith("Right"));
        }
        else if (e.getActionCommand().equals("headerRegex")) {
            String headerRegExTemp = this.headerRegEx;

            do {
                headerRegExTemp = (String) JOptionPane.showInputDialog(this, "Enter the regular expression for the beginning of a new document", "Document Header RegEx", JOptionPane.PLAIN_MESSAGE, null, null, headerRegExTemp);


                if (headerRegExTemp == null) {
                    break;
                }
            } while (!initializeStartDocumentIndexes(headerRegExTemp, true));
            if (this.startDocumentIndexes != null) {
                JOptionPane.showMessageDialog(this, "Your regular expression found " + this.startDocumentIndexes.length + " documents", "RegEx Result", JOptionPane.PLAIN_MESSAGE);
            }
        }
        else if (e.getActionCommand().equals("summaryRegex")) {
            String bodyRegExTemp = this.bodyRegEx;

            do {
                bodyRegExTemp = (String) JOptionPane.showInputDialog(this, "Enter the regular expression for the divider between summary and body", "Summary Divider RegEx", JOptionPane.PLAIN_MESSAGE, null, null, bodyRegExTemp);


                if (bodyRegExTemp == null) {
                    break;
                }
            } while (!initializeStartDocumentIndexes(bodyRegExTemp, false));
            if (this.startBodyIndexes != null) {
                JOptionPane.showMessageDialog(this, "Your regular expression found " + this.startBodyIndexes.length + " summaries/bodies", "RegEx Result", JOptionPane.PLAIN_MESSAGE);
            }
        }
        else if (e.getActionCommand().equals("helpAbout")) {
            JTextArea help = new JTextArea(helpAbout);
            help.setEditable(false);
            help.setOpaque(false);
            JOptionPane.showMessageDialog(this, help, "About SEAView v. 1.0", JOptionPane.INFORMATION_MESSAGE);
        }
        else if (e.getActionCommand().equals("sort")) {
            table.orderEDUs(false, isPeerLoaded);
        }
        else if (e.getActionCommand().equals("changeLabel")) {
            table.changeEDULabel();
        }
        else if (e.getActionCommand().equals("remove")) {
            table.remove();
        }
        else if (e.getActionCommand().equals("showModelEssays")) {
            modelEssaysDialog.setVisible(true);
        }
        else if (e.getActionCommand().equals("orderByWeight")) {
            if (isPeerLoaded) {
                this.peerTree.order();
                if (this.expandCollapseBtn.getText().equals("Collapse")) {
                    this.peerTree.expandTree();
                }
            }
            else {
                this.pyramidTree.order();
                if (this.expandCollapseBtn.getText().equals("Collapse")) {
                    this.pyramidTree.expandTree();
                }
            }
        }
        else if (e.getActionCommand().equals("orderAlphabetically")) {
            if (isPeerLoaded) {
                this.peerTree.orderAlphabetically();
                if (this.expandCollapseBtn.getText().equals("Collapse")) {
                    this.peerTree.expandTree();
                }
            }
            else {
                this.pyramidTree.orderAlphabetically();
                if (this.expandCollapseBtn.getText().equals("Collapse")) {
                    this.pyramidTree.expandTree();
                }
            }
        }
        else if (e.getActionCommand().equals("collapse")) {
            this.pyramidTree.collapseTree();
            this.peerTree.collapseTree();
            this.expandCollapseBtn.setText(" Expand ");
            this.expandCollapseBtn.setMnemonic('p');
            this.expandCollapseBtn.setActionCommand("expand");
        }
        else if (e.getActionCommand().equals("expand")) {
            this.pyramidTree.expandTree();
            this.peerTree.expandTree();
            this.expandCollapseBtn.setText("Collapse");
            this.expandCollapseBtn.setMnemonic('l');
            this.expandCollapseBtn.setActionCommand("collapse");
        }
    }

    private Document makeDocument(File file) throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
        InputStream inputStream= new FileInputStream(file);
        Reader reader = new InputStreamReader(inputStream,"UTF-8");
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setValidating(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        dbFactory.setIgnoringComments(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        dBuilder.setErrorHandler(this);
        return dBuilder.parse(is);
        /*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        documentBuilder.setErrorHandler(this);
        return documentBuilder.parse(file);*/
    }

    /**
     * Display a message at the bottom of the window
     *
     * @param text  the message to be displayed
     */
    public void msg(String text) {
        this.statusLbl.setText(text);
    }

    private String xmlize(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\"", "&quot;").replaceAll("\n", "");
    }

    /**
     *
     * @param   file  the file to be written
     * @param   writePeer whether the file to be written is a SEA peer annotation
     * @return  true if the file writeout is successful, false if not
     */
    private boolean writeout(File file, boolean writePeer) {
        boolean success = true;
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)), true);

            writer.println("<?xml version=\"1.0\"?>");
            writer.println("<!DOCTYPE " + (writePeer ? "SEP" : "SEA") + " [");

            writer.println(writePeer ? getPeerDTD() : getCrowdDTD());
            writer.println("]>");
            writer.println();
            if (writePeer) {
                writer.println("<SEP>");

                writer.println("<pyramid>");
                writer.println(getPyrXML(false));
                writer.println("</pyramid>");

                writer.println("<annotation>");
                writer.println(getPyrXML(true));
                writer.println("</annotation>");

                writer.println("<sepAnnotation>");
                writer.println(getSEAXML(true));
                writer.println("</sepAnnotation>");

                writer.println("</SEP>");

                this.peerFile = file;
                setPeerModified(false);
            }
            else {
                writer.println("<SEA>");

                writer.println("<pyramid>");
                writer.println(getPyrXML(false));
                writer.println("</pyramid>");

                writer.println("<seaAnnotation>");
                writer.println(getSEAXML(false));
                writer.println("</seaAnnotation>");

                writer.println("</SEA>");

                this.crowdFile = file;
                setCrowdModified(false);
            }
            writer.close();
            msg("Saved " + file);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            msg(ex.getMessage());
            success = false;
        }
        return success;
    }

    /**
     * Write the DTD for the XML for a pyramid
     *
     * @return the DTD (string)
     */
    private String getCrowdDTD() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(" <!ELEMENT SEA (pyramid, seaAnnotation)>").append(eol);

        buffer.append(" <!ELEMENT pyramid (startDocumentRegEx?,text,scu*)>").append(eol);
        buffer.append(" <!ELEMENT startDocumentRegEx (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT text (line*)>").append(eol);
        buffer.append(" <!ELEMENT line (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT scu (contributor)+>").append(eol);
        buffer.append(" <!ATTLIST scu uid CDATA #REQUIRED").append(eol);
        buffer.append("               label CDATA #REQUIRED").append(eol);
        buffer.append("               comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT contributor (part)+>").append(eol);
        buffer.append(" <!ATTLIST contributor label CDATA #REQUIRED").append(eol);
        buffer.append("                       comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT part EMPTY>").append(eol);
        buffer.append(" <!ATTLIST part label CDATA #REQUIRED").append(eol);
        buffer.append("                start CDATA #REQUIRED").append(eol);
        buffer.append("                end   CDATA #REQUIRED>").append(eol);

        buffer.append(" <!ELEMENT seaAnnotation (startBodyRegEx, seaTable*, seaAlignment)>").append(eol);
        buffer.append(" <!ELEMENT startBodyRegEx (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT seaTable (eduscuPair)*>").append(eol);
        buffer.append(" <!ATTLIST seaTable essayNum CDATA #REQUIRED>").append(eol);
        buffer.append(" <!ELEMENT eduscuPair (edu, scu?)>").append(eol);
        buffer.append(" <!ELEMENT edu (contributor)+>").append(eol);
        buffer.append(" <!ATTLIST edu uid CDATA #REQUIRED").append(eol);
        buffer.append("               label CDATA #REQUIRED").append(eol);
        buffer.append("               comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT seaAlignment (alignment)*>").append(eol);
        buffer.append(" <!ELEMENT alignment (scuId, scuWeight, eduId+, numEdus)>").append(eol);
        buffer.append(" <!ELEMENT scuId (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT scuWeight (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT eduId (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT numEdus (#PCDATA)>").append(eol);

        return buffer.toString();
    }

    /**
     * Write the DTD for the XML for a peer annotation with a pyramid and score
     *
     * @return the DTD (string)
     */
    private String getPeerDTD() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(" <!ELEMENT SEP (pyramid, annotation, sepAnnotation)>").append(eol);

        buffer.append(" <!ELEMENT pyramid (startDocumentRegEx?,text,scu*)>").append(eol);
        buffer.append(" <!ELEMENT startDocumentRegEx (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT text (line*)>").append(eol);
        buffer.append(" <!ELEMENT line (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT scu (contributor)+>").append(eol);
        buffer.append(" <!ATTLIST scu uid CDATA #REQUIRED").append(eol);
        buffer.append("               label CDATA #REQUIRED").append(eol);
        buffer.append("               comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT contributor (part)+>").append(eol);
        buffer.append(" <!ATTLIST contributor label CDATA #REQUIRED").append(eol);
        buffer.append("                       comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT part EMPTY>").append(eol);
        buffer.append(" <!ATTLIST part label CDATA #REQUIRED").append(eol);
        buffer.append("                start CDATA #REQUIRED").append(eol);
        buffer.append("                end   CDATA #REQUIRED>").append(eol);

        buffer.append(" <!ELEMENT annotation (text,peerscu+)>").append(eol);
        buffer.append(" <!ELEMENT peerscu (contributor)*>").append(eol);
        buffer.append(" <!ATTLIST peerscu uid CDATA #REQUIRED").append(eol);
        buffer.append("                   label CDATA #REQUIRED").append(eol);
        buffer.append("                   comment CDATA #IMPLIED>").append(eol);

        buffer.append(" <!ELEMENT sepAnnotation (startBodyRegEx, sepTable, sepAlignment)>").append(eol);
        buffer.append(" <!ELEMENT startBodyRegEx (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT sepTable (peereduscuPair)*>").append(eol);
        buffer.append(" <!ELEMENT peereduscuPair (peeredu, peerscu?)>").append(eol);
        buffer.append(" <!ELEMENT peeredu (contributor)+>").append(eol);
        buffer.append(" <!ATTLIST peeredu uid CDATA #REQUIRED").append(eol);
        buffer.append("                   label CDATA #REQUIRED").append(eol);
        buffer.append("                   comment CDATA #IMPLIED>").append(eol);
        buffer.append(" <!ELEMENT sepAlignment (alignment)*>").append(eol);
        buffer.append(" <!ELEMENT alignment (scuId, scuWeight, eduId+, numEdus)>").append(eol);
        buffer.append(" <!ELEMENT scuId (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT scuWeight (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT eduId (#PCDATA)>").append(eol);
        buffer.append(" <!ELEMENT numEdus (#PCDATA)>").append(eol);

        return buffer.toString();
    }

    private String getSEAXML(boolean getPeer) {
        StringBuffer buffer = new StringBuffer();
        DefaultTableModel model = table.getTableModel();
        SCUTree tree = getPeer ? peerTree : pyramidTree;

        if (this.bodyRegEx != null) {
            buffer.append("<startBodyRegEx><![CDATA[").append(this.bodyRegEx).append("]]></startBodyRegEx>").append(eol);
        }

        // SEA Annotation XML
        int currentEssayNum = 1;
        int essayNum;
        int row = 0;
        Map<String, List<String>>alignment = table.orderEDUs(true, getPeer);

        SCUTree eduTree = (SCUTree) model.getValueAt(row, 0);
        DefaultMutableTreeNode eduNode = eduTree.getRootNode().getNextNode();

        buffer.append(getPeer ? "<sepTable>" : "<seaTable essayNum=\"1\">").append(eol);
        while (eduNode != null && row < model.getRowCount() - 1) {
            if (!getPeer) {
                essayNum = table.getEssayNumberofEdu(eduNode);

                if (essayNum > currentEssayNum && row != 0) {
                    currentEssayNum = essayNum;
                    buffer.append("</seaTable>").append(eol);
                    buffer.append("<seaTable essayNum=\"").append(currentEssayNum).append("\">").append(eol);
                }
            }
            buffer.append("  <").append(getPeer ? "peer" : "").append("eduscuPair>").append(eol);
            SCU edu = (SCU) eduNode.getUserObject();
            String eduComment = edu.getComment();
            if ((eduComment != null) && (eduComment.length() > 0)) {
                eduComment = " comment=\"" + xmlize(eduComment) + "\"";
            } else {
                eduComment = "";
            }
            buffer.append("  <").append(getPeer ? "peer" : "").append("edu uid=\"").append(edu.getTempId()).append("\" label=\"").append(xmlize(edu.getLabel())).append("\"").append(eduComment).append(">").append(eol);
            Enumeration eduContributorEnum = eduNode.children();
            while (eduContributorEnum.hasMoreElements()) {
                SCUContributor eduContributor = (SCUContributor) ((DefaultMutableTreeNode) eduContributorEnum.nextElement()).getUserObject();


                String eduContributorComment = eduContributor.getComment();
                if ((eduContributorComment != null) && (eduContributorComment.length() > 0)) {
                    eduContributorComment = " comment=\"" + xmlize(eduContributorComment) + "\"";
                } else {
                    eduContributorComment = "";
                }
                buffer.append("   <contributor label=\"").append(xmlize(eduContributor.toString())).append("\"").append(eduContributorComment).append(">").append(eol);

                Iterator eduContributorParts = eduContributor.elements();
                while (eduContributorParts.hasNext()) {
                    SCUContributorPart eduContributorPart = (SCUContributorPart) eduContributorParts.next();

                    buffer.append("    <part label=\"").append(xmlize(eduContributorPart.toString())).append("\" start=\"").append(eduContributorPart.getStartIndex()).append("\" end=\"").append(eduContributorPart.getEndIndex()).append("\"/>").append(eol);
                }


                buffer.append("   </contributor>").append(eol);
            }
            buffer.append("  </").append(getPeer ? "peer" : "").append("edu>").append(eol);

            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) model.getValueAt(row, 1);
            if (scuNode != null) {
                SCU scu = (SCU) scuNode.getUserObject();
                String scuComment = scu.getComment();
                if ((scuComment != null) && (scuComment.length() > 0)) {
                    scuComment = " comment=\"" + xmlize(scuComment) + "\"";
                } else {
                    scuComment = "";
                }
                buffer.append("  <").append(getPeer ? "peer" : "").append("scu uid=\"").append(scu.getId()).append("\" label=\"").append(xmlize(scu.getLabel())).append("\"").append(scuComment).append(">").append(eol);
                Enumeration scuContributorEnum = scuNode.children();
                while (scuContributorEnum.hasMoreElements()) {
                    SCUContributor scuContributor = (SCUContributor) ((DefaultMutableTreeNode) scuContributorEnum.nextElement()).getUserObject();


                    String scuContributorComment = scuContributor.getComment();
                    if ((scuContributorComment != null) && (scuContributorComment.length() > 0)) {
                        scuContributorComment = " comment=\"" + xmlize(scuContributorComment) + "\"";
                    } else {
                        scuContributorComment = "";
                    }
                    buffer.append("   <contributor label=\"").append(xmlize(scuContributor.toString())).append("\"").append(scuContributorComment).append(">").append(eol);

                    Iterator scuContributorParts = scuContributor.elements();
                    while (scuContributorParts.hasNext()) {
                        SCUContributorPart scuContributorPart = (SCUContributorPart) scuContributorParts.next();

                        buffer.append("    <part label=\"").append(xmlize(scuContributorPart.toString())).append("\" start=\"").append(scuContributorPart.getStartIndex()).append("\" end=\"").append(scuContributorPart.getEndIndex()).append("\"/>").append(eol);
                    }


                    buffer.append("   </contributor>").append(eol);
                }
                buffer.append("  </").append(getPeer ? "peer" : "").append("scu>").append(eol);
            }
            buffer.append("  </").append(getPeer ? "peer" : "").append("eduscuPair>").append(eol);
            row++;
            if (row < model.getRowCount()) {
                eduTree = (SCUTree) model.getValueAt(row, 0);
                if (eduTree != null) {
                    eduNode = eduTree.getRootNode().getNextNode();
                } else {
                    eduNode = null;
                }
            }
            else {
                eduNode = null;
            }
        }
        buffer.append(getPeer ? "</sepTable>" : "</seaTable>").append(eol);

        // SEA Alignment XML
        buffer.append(getPeer ? "<sepAlignment>" : "<seaAlignment>").append(eol);
        if (alignment != null && Collections.frequency(alignment.values(), null) != alignment.size()) {
            TreeMap sortedAlignment = new TreeMap<String, List<String>>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                        }
                    });
            sortedAlignment.putAll(alignment);
            Set keySet = sortedAlignment.keySet();
            Iterator iterator = keySet.iterator();

            while (iterator.hasNext()) {
                buffer.append(" <alignment>").append(eol);

                String scuTempID = (String) iterator.next();
                SCU scu = tree.getSCUsByTempID(scuTempID);
                int id = scu.getId();
                int weight = scu.getWeight();

                buffer.append("  <scuId>").append(id).append("</scuId>").append(eol);

                buffer.append("  <scuWeight>").append(weight).append("</scuWeight>").append(eol);

                List edus = (List) sortedAlignment.get(scuTempID);
                for (ListIterator iter = edus.listIterator(); iter.hasNext(); ) {
                    String eduId = (String) iter.next();
                    buffer.append("  <eduId>").append(eduId).append("</eduId>").append(eol);
                }
                buffer.append("  <numEdus>").append(Integer.toString(edus.size())).append("</numEdus>").append(eol);
                buffer.append(" </alignment>").append(eol);
            }
        }
        buffer.append(getPeer ? "</sepAlignment>" : "</seaAlignment>");
        return buffer.toString();
    }

    private String getPyrXML(boolean getPeer) {
        StringBuffer buffer = new StringBuffer();
        SEAViewTextPane textPane;
        SCUTree tree;

        if (isPeerLoaded) {
            if (getPeer) {
                textPane = essayPane;
                tree = peerTree;
            }
            else {
                textPane = modelEssaysPane;
                tree = pyramidTree;
            }
        }
        else {
            textPane = essayPane;
            tree = pyramidTree;
        }

        if (!getPeer && this.headerRegEx != null) {
            buffer.append("<startDocumentRegEx><![CDATA[").append(this.headerRegEx).append("]]></startDocumentRegEx>").append(eol);
        }

        buffer.append(" <text>").append(eol);
        String[] lines = textPane.getText().split("\n");
        for (int i = 0; i < lines.length; i++) {
            buffer.append("  <line>").append(xmlize(lines[i])).append("</line>").append(eol);
        }
        buffer.append(" </text>").append(eol);
        Enumeration scuNodesEnum = tree.getRootNode().children();
        boolean wroteSCU = false;
        while (scuNodesEnum.hasMoreElements()) {
            DefaultMutableTreeNode scuNode = (DefaultMutableTreeNode) scuNodesEnum.nextElement();

            SCU scu = (SCU) scuNode.getUserObject();

            if (wroteSCU) {
                buffer.append(eol);
            }
            wroteSCU = true;

            String scuComment = scu.getComment();
            if ((scuComment != null) && (scuComment.length() > 0)) {
                scuComment = " comment=\"" + xmlize(scuComment) + "\"";
            } else {
                scuComment = "";
            }
            buffer.append(" <").append(getPeer ? "peer" : "").append("scu uid=\"").append(scu.getId()).append("\" label=\"").append(xmlize(scu.getLabel())).append("\"").append(scuComment).append(">").append(eol);

            Enumeration scuContributorEnum = scuNode.children();
            while (scuContributorEnum.hasMoreElements()) {
                SCUContributor scuContributor = (SCUContributor) ((DefaultMutableTreeNode) scuContributorEnum.nextElement()).getUserObject();


                String scuContributorComment = scuContributor.getComment();
                if ((scuContributorComment != null) && (scuContributorComment.length() > 0)) {
                    scuContributorComment = " comment=\"" + xmlize(scuContributorComment) + "\"";
                } else {
                    scuContributorComment = "";
                }
                buffer.append("  <contributor label=\"").append(xmlize(scuContributor.toString())).append("\"").append(scuContributorComment).append(">").append(eol);

                Iterator scuContributorParts = scuContributor.elements();
                while (scuContributorParts.hasNext()) {
                    SCUContributorPart scuContributorPart = (SCUContributorPart) scuContributorParts.next();

                    buffer.append("   <part label=\"").append(xmlize(scuContributorPart.toString())).append("\" start=\"").append(scuContributorPart.getStartIndex()).append("\" end=\"").append(scuContributorPart.getEndIndex()).append("\"/>").append(eol);
                }


                buffer.append("  </contributor>").append(eol);
            }
            buffer.append(" </").append(getPeer ? "peer" : "").append("scu>");
        }
        return buffer.toString();
    }

    protected void setCrowdModified(boolean isModified) {
        this.isCrowdModified = isModified;
        this.fileSaveCrowdMenuItem.setEnabled(isModified);
        this.fileSaveCrowdAsMenuItem.setEnabled(isModified);
        //this.undoController.add(deepCopy(this.table.getTableModel()));
    }

    protected void setPeerModified(boolean isModified) {
        this.isPeerModified = isModified;
        this.fileSavePeerMenuItem.setEnabled(isModified);
        this.fileSavePeerAsMenuItem.setEnabled(isModified);
        //this.peerUndoController.add(deepCopy(this.peerTree.getRootNode()));
    }

    private void setCrowdLoaded(boolean isLoaded) {
        this.isCrowdLoaded = isLoaded;
        this.fileCloseCrowdMenuItem.setEnabled(isLoaded);
        this.fileShowSCUEDUAlignmentMenuItem.setEnabled(isLoaded);
        this.documentStartRegexMenuItem.setEnabled(isLoaded);
        this.summaryDividerRegexMenuItem.setEnabled(isLoaded);
        this.sortTableBtn.setEnabled(isLoaded);
        this.expandCollapseBtn.setEnabled(isLoaded);
        this.orderByWeightBtn.setEnabled(isLoaded);
        this.orderAlphabeticallyBtn.setEnabled(isLoaded);

        this.pyramidTree.setSCUTextPane(this.essayPane);

        if (!isLoaded) {
            setCrowdModified(false);
            this.changeLabelBtn.setEnabled(false);
            this.removeBtn.setEnabled(false);
        }
        /*
        if (isLoaded) {
            //showCard("pyramid");
            //this.undoController.setActive(true);
            //this.undoController.setActive(false);
            //this.undoController.clear();
            //this.undoController.add(deepCopy(this.table));
            //this.undoController.add(deepCopy(this.table.getTableModel().getValueAt(0, 1)));
        } else {
            //this.undoController.clear();
            //this.undoController.setActive(false);
            setCrowdModified(false);
            //this.orderByWeightBtn.setEnabled(false);
            //this.collapseBtn.setEnabled(false);
        }*/
    }

    private void setPeerLoaded(boolean isLoaded) {
        this.isPeerLoaded = isLoaded;
        this.fileClosePeerMenuItem.setEnabled(isLoaded);
        this.fileShowPeerSCUEDUAlignmentMenuItem.setEnabled(isLoaded);
        this.summaryDividerRegexMenuItem.setEnabled(isLoaded);
        this.sortTableBtn.setEnabled(isLoaded);
        this.expandCollapseBtn.setEnabled(isLoaded);
        this.orderByWeightBtn.setEnabled(isLoaded);
        this.orderAlphabeticallyBtn.setEnabled(isLoaded);
        this.showModelEssaysBtn.setEnabled(isLoaded);

        this.table.setPeer(isLoaded);

        if (!isLoaded) {
            setPeerModified(false);
            this.changeLabelBtn.setEnabled(false);
            this.removeBtn.setEnabled(false);
        }

        //this.pyramidTree.setSCUTextPane(this.essayPane);
        //this.pyramidTree.setDragEnabled(!isLoaded);


        /*if (isLoaded) {
            showCard("peer");
            this.fileClosePeerAnnotationMenuItem.setEnabled(true);
            this.scoreDlg.setText(getScore());
            this.scoreDlg.pack();

            this.pyramidUndoController.setActive(false);
            this.peerUndoController.setActive(true);
            this.peerUndoController.clear();
            this.peerUndoController.add(deepCopy(this.peerTree.getRootNode()));
        } else {
            showCard("pyramid");
            this.fileClosePeerAnnotationMenuItem.setEnabled(false);
            setPeerModified(false);
            this.fileShowPeerAnnotationScoreMenuItem.setSelected(false);
            this.scoreDlg.setVisible(false);

            this.peerUndoController.clear();
            this.peerUndoController.setActive(false);
            this.pyramidUndoController.setActive(true);
        }*/
    }

    private boolean saveModifiedCrowd() {
        int choice = JOptionPane.showConfirmDialog(this, "Save changes to the loaded SEA annotation?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            if (this.crowdFile == null) {
                return saveCrowd(true);
            }
            return saveCrowd(false);
        }
        else if (choice == JOptionPane.NO_OPTION) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean saveModifiedPeer() {
        int choice = JOptionPane.showConfirmDialog(this, "Save changes to the loaded SEP annotation?", "Save changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            if (this.peerFile == null) {
                return savePeer(true);
            }
            return savePeer(false);
        }
        else if (choice == JOptionPane.NO_OPTION) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean saveCrowd(boolean useNewFile) {
        if (useNewFile) {
            JFileChooser chooser = new SEAViewFileChooser(true, true, false);
            if (chooser.showSaveDialog(this) == 0) {
                return writeout(chooser.getSelectedFile(), false);
            }
            return false;
        }
        return writeout(this.crowdFile, false);
    }

    private boolean savePeer(boolean useNewFile) {
        if (useNewFile) {
            JFileChooser chooser = new SEAViewFileChooser(true, false, false);
            if (chooser.showSaveDialog(this) == 0) {
                return writeout(chooser.getSelectedFile(), true);
            }
            return false;
        }
        return writeout(this.peerFile, true);
    }

    private void loadEssay(File file, boolean isPeer, boolean initIndexes) throws ParserConfigurationException, SAXException, IOException {
        Document doc = makeDocument(file);
        NodeList lineNodeList = ((Element) doc.getElementsByTagName((isPeer ? "annotation" : "pyramid")).item(0)).getElementsByTagName("line");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < lineNodeList.getLength(); i++) {
            if (lineNodeList.item(i).getFirstChild() != null) {
                buffer.append(lineNodeList.item(i).getFirstChild().getNodeValue());
            }
            buffer.append("\n");
        }
        this.essayPane.loadText(buffer.toString());

        if (isPeer) {
            // Load models
            NodeList lineNodeList2 = ((Element) doc.getElementsByTagName("pyramid").item(0)).getElementsByTagName("line");
            StringBuilder buffer2 = new StringBuilder();
            for (int i = 0; i < lineNodeList2.getLength(); i++) {
                if (lineNodeList2.item(i).getFirstChild() != null) {
                    buffer2.append(lineNodeList2.item(i).getFirstChild().getNodeValue());
                }
                buffer2.append("\n");
            }
            this.modelEssaysPane.loadText(buffer2.toString());
        }

        if (initIndexes) {
            String regexStr = null;
            try {
                regexStr = doc.getElementsByTagName("startDocumentRegEx").item(0).getFirstChild().getNodeValue();
            } catch (NullPointerException ex) {
            }
            if (regexStr != null) {
                headerRegEx = regexStr;
                if (!isPeer) {
                    initializeStartDocumentIndexes(regexStr, true);
                }
            }
            String regexStr2 = null;
            try {
                regexStr2 = doc.getElementsByTagName("startBodyRegEx").item(0).getFirstChild().getNodeValue();
            } catch (NullPointerException ex) {
            }
            if (regexStr2 != null) {
                bodyRegEx = regexStr2;
                initializeStartDocumentIndexes(regexStr2, false);
            }
        }
    }

    private void loadPyramid(File file, boolean isPeer, SEAViewTextPane pane) throws ParserConfigurationException, SAXException, IOException {
        Document doc = makeDocument(file);
        Element top = (Element) doc.getElementsByTagName((isPeer ? "annotation" : "pyramid")).item(0);

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root Node");
        NodeList scuNodeList = top.getElementsByTagName((isPeer ? "peer" : "") + "scu");

        for (int scuCnt = 0; scuCnt < scuNodeList.getLength(); scuCnt++) {
            Element scuElement = (Element) scuNodeList.item(scuCnt);
            DefaultMutableTreeNode scuNode = getSCUNodeFromXML(scuElement, false, pane, scuCnt + 1, isPeer);
            SCU scu = (SCU) scuNode.getUserObject();
            scu.isPeer(isPeer);
            rootNode.add(scuNode);
        }
        if (isPeer) {
            this.peerTree.rebuildTree(rootNode);
            pyramidPanel.remove(pyramidScrollPane);
            pyramidScrollPane = new JScrollPane(this.peerTree);
            pyramidPanel.add(pyramidScrollPane);
            pyramidPanel.revalidate();
            pyramidPanel.repaint();
        }
        else {
            this.pyramidTree.rebuildTree(rootNode);
        }
        this.table.updateSCUIndices();
        this.orderByWeightBtn.setEnabled(true);
        this.orderAlphabeticallyBtn.setEnabled(true);
        this.expandCollapseBtn.setEnabled(true);
        this.sortTableBtn.setEnabled(true);
    }

    private void loadTable(File file, boolean isPeer, boolean loadCrowdForSEP) throws ParserConfigurationException, SAXException, IOException {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        Document doc = makeDocument(file);
        Element top = (Element) doc.getElementsByTagName((isPeer ? "sepAnnotation" : "seaAnnotation")).item(0);
        NodeList tableList = top.getElementsByTagName(isPeer ? "sepTable" : "seaTable");

        DefaultTableModel model = table.getTableModel();

        int row = 0;
        for (int tableCnt = 0; tableCnt < tableList.getLength(); tableCnt++) {
            Node eduTable = tableList.item(tableCnt);
            NodeList eduscuPairsList = eduTable.getChildNodes();
            for (int pair = 0; pair < eduscuPairsList.getLength(); pair++) {
                NodeList eduScu = eduscuPairsList.item(pair).getChildNodes();
                Element edu = (Element) eduScu.item(0);
                SEAViewTextPane textPane;
                if (!isPeer && loadCrowdForSEP) {
                    textPane = modelEssaysPane;
                }
                else {
                    textPane = essayPane;
                }
                DefaultMutableTreeNode eduNode = getSCUNodeFromXML(edu, true, textPane, 0, isPeer);
                table.insertEDUTree(eduNode, row, loadCrowdForSEP);

                // Get SCU from pair
                Element scu = (Element) eduScu.item(1);
                if (scu != null) {
                    DefaultMutableTreeNode scuNode = getSCUNodeFromXML(scu, false, textPane, 0, isPeer);
                    SCU scuObject = (SCU) scuNode.getUserObject();
                    scuObject.isPeer(isPeer);
                    model.setValueAt(scuNode, row, 1);
                }
                row++;

            }
        }
        table.updateSCUIndices();
        table.orderEDUs(false, isPeer);
    }

    private DefaultMutableTreeNode getSCUNodeFromXML(Element scuElement, boolean isEdu, SEAViewTextPane textPane, int nextAvailableTempId, boolean isPeer) {
        String scuLabel = scuElement.getAttribute("label");
        String scuComment = scuElement.getAttribute("comment");
        NodeList scuContributorNodeList = scuElement.getElementsByTagName("contributor");
        DefaultMutableTreeNode scuNode;
        if (isEdu) {
            String id = scuElement.getAttribute("uid");
            SCU edu = new SCU(0, scuLabel, scuComment, id, scuContributorNodeList.getLength());
            edu.setEdu(true);
            scuNode = new DefaultMutableTreeNode(edu);
        }
        else {
            int id = Integer.parseInt(scuElement.getAttribute("uid"));
            int weight = scuContributorNodeList.getLength();
            if (isPeer) {
                Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(scuLabel);
                if (m.find()) {
                    weight = Integer.parseInt(m.group(1));
                }
            }
            scuNode = new DefaultMutableTreeNode(new SCU(id, scuLabel, scuComment, id, weight));
            if (nextAvailableTempId > 0) {
                // Temp ID has been specified
                ((SCU) scuNode.getUserObject()).setScuTempId(nextAvailableTempId);
            }
        }

        for (int scuContributorCnt = 0;
             scuContributorCnt < scuContributorNodeList.getLength();
             scuContributorCnt++) {
            Element scuContributorElement = (Element) scuContributorNodeList.item(scuContributorCnt);

            String scuContributorComment = scuContributorElement.getAttribute("comment");
            NodeList scuContributorPartNodeList = scuContributorElement.getElementsByTagName("part");

            Element scuContributorPartElement = (Element) scuContributorPartNodeList.item(0);

            int startIndex = Integer.parseInt(scuContributorPartElement.getAttribute("start"));

            int endIndex = Integer.parseInt(scuContributorPartElement.getAttribute("end"));

            String label = scuContributorPartElement.getAttribute("label");
            SCUContributorPart scuContributorPart = new SCUContributorPart(startIndex, endIndex, label);

            textPane.modifyTextSelection(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);

            SCUContributor scuContributor = new SCUContributor(scuContributorPart, scuContributorComment);
            DefaultMutableTreeNode scuContributorNode = new DefaultMutableTreeNode(scuContributor);

            for (int scuContributorPartCnt = 1;
                 scuContributorPartCnt < scuContributorPartNodeList.getLength();
                 scuContributorPartCnt++) {
                if (scuContributorPartCnt == 1) {

                    scuContributorNode.add(new DefaultMutableTreeNode(scuContributorPart));
                }
                scuContributorPartElement = (Element) scuContributorPartNodeList.item(scuContributorPartCnt);

                startIndex = Integer.parseInt(scuContributorPartElement.getAttribute("start"));

                endIndex = Integer.parseInt(scuContributorPartElement.getAttribute("end"));

                label = scuContributorPartElement.getAttribute("label");
                scuContributorPart = new SCUContributorPart(startIndex, endIndex, label);

                textPane.modifyTextSelection(scuContributorPart.getStartIndex(), scuContributorPart.getEndIndex(), true);

                scuContributor.add(scuContributorPart);
                scuContributorNode.add(new DefaultMutableTreeNode(scuContributorPart));
            }
            scuNode.add(scuContributorNode);
        }
        return scuNode;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('f');

        JMenu crowdMenu = new JMenu("SEA Annotation");
        crowdMenu.setMnemonic('s');

        this.fileNewCrowdMenuItem = new JMenuItem("New...");
        this.fileNewCrowdMenuItem.setMnemonic('n');
        this.fileNewCrowdMenuItem.setActionCommand("fileNewCrowd");
        this.fileNewCrowdMenuItem.addActionListener(this);
        crowdMenu.add(this.fileNewCrowdMenuItem);

        this.fileLoadCrowdMenuItem = new JMenuItem("Load...");
        this.fileLoadCrowdMenuItem.setMnemonic('l');
        this.fileLoadCrowdMenuItem.setActionCommand("fileLoadCrowd");
        this.fileLoadCrowdMenuItem.addActionListener(this);
        crowdMenu.add(this.fileLoadCrowdMenuItem);

        this.fileSaveCrowdMenuItem = new JMenuItem("Save");
        this.fileSaveCrowdMenuItem.setMnemonic('s');
        this.fileSaveCrowdMenuItem.setActionCommand("fileSaveCrowd");
        this.fileSaveCrowdMenuItem.addActionListener(this);
        this.fileSaveCrowdMenuItem.setEnabled(false);
        crowdMenu.add(this.fileSaveCrowdMenuItem);

        this.fileSaveCrowdAsMenuItem = new JMenuItem("Save As...");
        this.fileSaveCrowdAsMenuItem.setMnemonic('a');
        this.fileSaveCrowdAsMenuItem.setActionCommand("fileSaveCrowdAs");
        this.fileSaveCrowdAsMenuItem.addActionListener(this);
        this.fileSaveCrowdAsMenuItem.setEnabled(false);
        crowdMenu.add(this.fileSaveCrowdAsMenuItem);

        this.fileShowSCUEDUAlignmentMenuItem = new JMenuItem("Show SCU-EDU Alignment");
        this.fileShowSCUEDUAlignmentMenuItem.setMnemonic('h');
        this.fileShowSCUEDUAlignmentMenuItem.setActionCommand("fileShowSCUEDUAlignment");
        this.fileShowSCUEDUAlignmentMenuItem.addActionListener(this);
        this.fileShowSCUEDUAlignmentMenuItem.setEnabled(false);
        crowdMenu.add(this.fileShowSCUEDUAlignmentMenuItem);

        this.fileCloseCrowdMenuItem = new JMenuItem("Close");
        this.fileCloseCrowdMenuItem.setMnemonic('c');
        this.fileCloseCrowdMenuItem.setActionCommand("fileCloseCrowd");
        this.fileCloseCrowdMenuItem.addActionListener(this);
        this.fileCloseCrowdMenuItem.setEnabled(false);
        crowdMenu.add(this.fileCloseCrowdMenuItem);

        fileMenu.add(crowdMenu);

        JMenu peerMenu = new JMenu("SEA Peer Annotation");
        peerMenu.setMnemonic('e');

        this.fileNewPeerMenuItem = new JMenuItem("New...");
        this.fileNewPeerMenuItem.setMnemonic('n');
        this.fileNewPeerMenuItem.setActionCommand("fileNewPeer");
        this.fileNewPeerMenuItem.addActionListener(this);
        peerMenu.add(this.fileNewPeerMenuItem);

        this.fileLoadPeerMenuItem = new JMenuItem("Load...");
        fileLoadPeerMenuItem.setMnemonic('l');
        fileLoadPeerMenuItem.setActionCommand("fileLoadPeer");
        fileLoadPeerMenuItem.addActionListener(this);
        peerMenu.add(fileLoadPeerMenuItem);

        this.fileSavePeerMenuItem = new JMenuItem("Save");
        this.fileSavePeerMenuItem.setMnemonic('s');
        this.fileSavePeerMenuItem.setActionCommand("fileSavePeer");
        this.fileSavePeerMenuItem.addActionListener(this);
        this.fileSavePeerMenuItem.setEnabled(false);
        peerMenu.add(this.fileSavePeerMenuItem);

        this.fileSavePeerAsMenuItem = new JMenuItem("Save As...");
        this.fileSavePeerAsMenuItem.setMnemonic('a');
        this.fileSavePeerAsMenuItem.setActionCommand("fileSavePeerAs");
        this.fileSavePeerAsMenuItem.addActionListener(this);
        this.fileSavePeerAsMenuItem.setEnabled(false);
        peerMenu.add(this.fileSavePeerAsMenuItem);

        this.fileShowPeerSCUEDUAlignmentMenuItem = new JMenuItem("Show SCU-EDU Alignment");
        this.fileShowPeerSCUEDUAlignmentMenuItem.setMnemonic('h');
        this.fileShowPeerSCUEDUAlignmentMenuItem.setActionCommand("fileShowSCUEDUAlignment");
        this.fileShowPeerSCUEDUAlignmentMenuItem.addActionListener(this);
        this.fileShowPeerSCUEDUAlignmentMenuItem.setEnabled(false);
        peerMenu.add(this.fileShowPeerSCUEDUAlignmentMenuItem);

        this.fileClosePeerMenuItem = new JMenuItem("Close");
        this.fileClosePeerMenuItem.setMnemonic('c');
        this.fileClosePeerMenuItem.setActionCommand("fileClosePeer");
        this.fileClosePeerMenuItem.addActionListener(this);
        this.fileClosePeerMenuItem.setEnabled(false);
        peerMenu.add(this.fileClosePeerMenuItem);

        fileMenu.add(peerMenu);

        this.fileExitMenuItem = new JMenuItem("Exit");
        fileExitMenuItem.setMnemonic('x');
        fileExitMenuItem.setActionCommand("exit");
        fileExitMenuItem.addActionListener(this);
        fileMenu.add(fileExitMenuItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('e');

        this.editFindMenuItem = new JMenuItem("Find...       Ctrl+F");
        editFindMenuItem.setMnemonic('f');
        editFindMenuItem.setActionCommand("find");
        editFindMenuItem.addActionListener(this);
        editMenu.add(editFindMenuItem);

        this.editUndoMenuItem = new JMenuItem("Undo...     Ctrl+Z");
        this.editUndoMenuItem.setMnemonic('u');
        this.editUndoMenuItem.setActionCommand("undo");
        this.editUndoMenuItem.addActionListener(this);
        this.editUndoMenuItem.setEnabled(false);
        editMenu.add(this.editUndoMenuItem);

        this.editRedoMenuItem = new JMenuItem("Redo...     Ctrl+Y");
        this.editRedoMenuItem.setMnemonic('r');
        this.editRedoMenuItem.setActionCommand("redo");
        this.editRedoMenuItem.addActionListener(this);
        this.editRedoMenuItem.setEnabled(false);
        editMenu.add(this.editRedoMenuItem);

        //menuBar.add(editMenu);

        JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic('o');

        JMenu textSizeSubmenu = new JMenu("Text Size");
        textSizeSubmenu.setMnemonic('t');

        int[] textSizes = {6, 8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40, 44, 48, 52, 56, 60};


        ButtonGroup sizeGroup = new ButtonGroup();
        for (int i = 0; i < textSizes.length; i++) {
            JRadioButtonMenuItem textSizeMenuItem = new JRadioButtonMenuItem(String.valueOf(textSizes[i]));

            textSizeMenuItem.setActionCommand("Text Size " + String.valueOf(textSizes[i]));
            textSizeMenuItem.addActionListener(this);
            sizeGroup.add(textSizeMenuItem);
            if (this.currentTextSize == textSizes[i]) {
                textSizeMenuItem.setSelected(true);
            }
            textSizeSubmenu.add(textSizeMenuItem);
        }
        optionsMenu.add(textSizeSubmenu);

        JMenu lookAndFeelSubmenu = new JMenu("Look And Feel");
        lookAndFeelSubmenu.setMnemonic('l');

        UIManager.LookAndFeelInfo[] installedLooks = UIManager.getInstalledLookAndFeels();

        ButtonGroup lookAndFeelGroup = new ButtonGroup();
        for (int i = 0; i < installedLooks.length; i++) {
            JRadioButtonMenuItem lookAndFeelMenuItem = new JRadioButtonMenuItem(installedLooks[i].getName());

            lookAndFeelMenuItem.setActionCommand("Look And Feel " + installedLooks[i].getClassName());

            lookAndFeelMenuItem.addActionListener(this);
            lookAndFeelGroup.add(lookAndFeelMenuItem);
            if (UIManager.getLookAndFeel().getName().equals(installedLooks[i].getName())) {
                lookAndFeelMenuItem.setSelected(true);
            }
            lookAndFeelSubmenu.add(lookAndFeelMenuItem);
        }

        //optionsMenu.add(lookAndFeelSubmenu);

        JMenu setLabelMenu = new JMenu("Set Label Mode");
        setLabelMenu.setMnemonic('d');

        ButtonGroup setLabelGroup = new ButtonGroup();

        this.setLabelOnInsertionMenuItem = new javax.swing.JRadioButtonMenuItem("Set labels immediately when inserting EDUs");
        this.setLabelOnInsertionMenuItem.setMnemonic('i');
        this.setLabelOnInsertionMenuItem.setActionCommand("setLabelNow");
        this.setLabelOnInsertionMenuItem.addActionListener(this);
        this.setLabelOnInsertionMenuItem.setSelected(true);
        //this.setLabelOnInsertionMenuItem.setEnabled(false);
        setLabelGroup.add(this.setLabelOnInsertionMenuItem);
        setLabelMenu.add(this.setLabelOnInsertionMenuItem);

        this.setLabelAfterInsertionMenuItem = new javax.swing.JRadioButtonMenuItem("Set labels later using button");
        this.setLabelAfterInsertionMenuItem.setMnemonic('a');
        this.setLabelAfterInsertionMenuItem.setActionCommand("setLabelLater");
        this.setLabelAfterInsertionMenuItem.addActionListener(this);
        //this.setLabelAfterInsertionMenuItem.setEnabled(false);
        setLabelGroup.add(this.setLabelAfterInsertionMenuItem);
        setLabelMenu.add(this.setLabelAfterInsertionMenuItem);

        optionsMenu.add(setLabelMenu);

        JMenu setdndClickModeMenu = new JMenu("Set DND Mode");
        setLabelMenu.setMnemonic('n');

        ButtonGroup setdndClickGroup = new ButtonGroup();

        this.dndLeftClickMenuItem = new javax.swing.JRadioButtonMenuItem("Drag and drop text using left click");
        this.dndLeftClickMenuItem.setMnemonic('l');
        this.dndLeftClickMenuItem.setActionCommand("dragLeft");
        this.dndLeftClickMenuItem.addActionListener(this);
        this.dndLeftClickMenuItem.setSelected(true);
        setdndClickGroup.add(this.dndLeftClickMenuItem);
        setdndClickModeMenu.add(this.dndLeftClickMenuItem);

        this.dndRightClickMenuItem = new javax.swing.JRadioButtonMenuItem("Drag and drop text using right click");
        this.dndRightClickMenuItem.setMnemonic('r');
        this.dndRightClickMenuItem.setActionCommand("dragRight");
        this.dndRightClickMenuItem.addActionListener(this);
        setdndClickGroup.add(this.dndRightClickMenuItem);
        setdndClickModeMenu.add(this.dndRightClickMenuItem);

        optionsMenu.add(setdndClickModeMenu);

        JMenu regexSubMenu = new JMenu("Set RegEx");
        regexSubMenu.setMnemonic('r');

        this.documentStartRegexMenuItem = new JMenuItem("Document Header RegEx");
        this.documentStartRegexMenuItem.setMnemonic('d');
        this.documentStartRegexMenuItem.setActionCommand("headerRegex");
        this.documentStartRegexMenuItem.addActionListener(this);
        this.documentStartRegexMenuItem.setEnabled(false);
        regexSubMenu.add(this.documentStartRegexMenuItem);

        this.summaryDividerRegexMenuItem = new JMenuItem("Summary Divider RegEx");
        this.summaryDividerRegexMenuItem.setMnemonic('s');
        this.summaryDividerRegexMenuItem.setActionCommand("summaryRegex");
        this.summaryDividerRegexMenuItem.addActionListener(this);
        this.summaryDividerRegexMenuItem.setEnabled(false);
        regexSubMenu.add(this.summaryDividerRegexMenuItem);

        optionsMenu.add(regexSubMenu);

        menuBar.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('h');

        JMenuItem helpAboutMenuItem = new JMenuItem("About...");
        helpAboutMenuItem.setMnemonic('a');
        helpAboutMenuItem.setActionCommand("helpAbout");
        helpAboutMenuItem.addActionListener(this);
        helpMenu.add(helpAboutMenuItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Display an error message.
     *
     * @param title     the title of the error message
     * @param message   the contents of the error message
     */
    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show a warning when an XML parsing exception occurs.
     *
     * @param ex An exception created during XML parsing.
     */
    public void warning(SAXParseException ex) {
        String message = "XML Parsing warning: " + ex.getMessage() + " at line " + ex.getLineNumber() + " col " + ex.getColumnNumber();

        System.err.println(message);
        msg(message);
    }

    /**
     * Show an error message when an XML parsing error occurs.
     *
     * @param ex An exception created during XML parsing.
     */
    public void error(org.xml.sax.SAXParseException ex) {
        String message = "XML Parsing error: " + ex.getMessage() + " at line " + ex.getLineNumber() + " col " + ex.getColumnNumber();

        System.err.println(message);
        msg(message);
        //showError("Error", message);
    }

    /**
     * Show an error message when an XML parsing fatal error occurs.
     *
     * @param ex An exception created during XML parsing.
     */
    public void fatalError(org.xml.sax.SAXParseException ex) {
        String message = "XML Parsing fatal error: " + ex.getMessage() + " at line " + ex.getLineNumber() + " col " + ex.getColumnNumber();

        System.err.println(message);
        msg(message);
        //showError("Error", message);
    }

    /**
     * Find the indices within each essay that correspond to the start of the body (and the
     * end of the summary)
     *
     * @param regexStr a regular expression that delimits the summary and body
     * @param isDocumentHeaderRegEx true if the indexes to be initialized are for the document headers;
     *                              false if the indexes are for the delimiter for the body of the essay
     *
     * @return true if at least two bodies were found using regexStr
     *         false if one or zero bodies were found, or if regexStr was not a valid regular expression
     */
    private boolean initializeStartDocumentIndexes(String regexStr, boolean isDocumentHeaderRegEx) {
        if (regexStr.trim().length() == 0) {
            showError("Regular Expression Error", "The regular expression is empty");
            return false;
        }

        Pattern p;
        try {
            p = Pattern.compile(regexStr);
        } catch (PatternSyntaxException ex) {
            showError("Regular Expression Error", "The regular expression is invalid:\n" + ex.getMessage());
            return false;
        }

        Matcher m = p.matcher(this.essayPane.getText());
        ArrayList indexes = new ArrayList();
        while (m.find()) {
            indexes.add(new Integer(m.start()));
        }
        if (indexes.isEmpty()) {
            showError("Regular Expression Error", "The regular expression did not match any text");
            return false;
        }
        if (!isPeerLoaded) {
            if (indexes.size() == 1) {
                showError("Regular Expression Error", "The regular expression only found one document");
                return false;
            }
            if (!isDocumentHeaderRegEx && startDocumentIndexes != null && indexes.size() != startDocumentIndexes.length) {
                showError("Regular Expression Error", "The regular expression found more summaries than essays");
                return false;
            }
            if (isDocumentHeaderRegEx && startBodyIndexes != null && indexes.size() != startBodyIndexes.length) {
                showError("Regular Expression Error", "The regular expression found more essays than summaries");
                return false;
            }
        }
        if (isDocumentHeaderRegEx) {
            this.startDocumentIndexes = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                this.startDocumentIndexes[i] = ((Integer) indexes.get(i)).intValue();
            }
            this.headerRegEx = regexStr;
        }
        else {
            this.startBodyIndexes = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                this.startBodyIndexes[i] = ((Integer) indexes.get(i)).intValue();
            }
            this.bodyRegEx = regexStr;
        }
        return true;
    }

    private void clearDocumentRegex() {
        headerRegEx = null;
        bodyRegEx = null;
        startDocumentIndexes = null;
        startBodyIndexes = null;
    }

    public int[] getStartDocumentIndexes() {
        return startDocumentIndexes;
    }

    public int[] getStartBodyIndexes() {
        return startBodyIndexes;
    }

    public JButton getChangeLabelBtn() {
        return changeLabelBtn;
    }

    public JButton getRemoveBtn() {
        return removeBtn;
    }

    private class SEAViewWindowAdapter extends WindowAdapter {
        private SEAView seaView;

        public SEAViewWindowAdapter(SEAView seaView) {
            this.seaView = seaView;
        }

        // Revisit
        public void windowClosing(WindowEvent e) {
            if (((SEAView.this.isPeerModified) && (!SEAView.this.saveModifiedPeer())) || ((!SEAView.this.isCrowdModified) || (SEAView.this.saveModifiedCrowd()))) {
                this.seaView.dispose();
            }
        }
    }

    private class SEAViewFileChooser extends JFileChooser {
        private boolean isSavingFile;
        private boolean isCrowd;
        private boolean isNew;

        public SEAViewFileChooser(boolean isSavingFile, boolean isCrowd, boolean isNew) {
            super();
            this.isSavingFile = isSavingFile;
            this.isCrowd = isCrowd;
            this.isNew = isNew;

            File f = new File(defaultFilePath);
            setCurrentDirectory(f);


            String defaultName = isCrowd ? SEAView.this.crowdInputFile : SEAView.this.peerInputFile;
            if ((defaultName == null) || (defaultName.trim().length() == 0))
                defaultName = "untitled";
            defaultName = defaultName.replaceFirst("\\.pyr$|\\.pan$", "");
            defaultName = defaultName + (isCrowd ? ".sea" : ".sep");

            if (isSavingFile) {
                setSelectedFile(new File(defaultName));
            }
            this.setFileFilter(new ScuEduFileFilter());
        }

        public void approveSelection() {
            if ((this.isSavingFile) && (getSelectedFile().exists()) && (JOptionPane.showConfirmDialog(this, "The file " + getSelectedFile().getName() + " already exists, would you like to overwrite it?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != 0)) {
                return;
            }
            super.approveSelection();
        }

        private class ScuEduFileFilter extends FileFilter {

            public boolean accept(java.io.File file) {
                if (file.isDirectory()) {
                    return true;
                }
                if (SEAViewFileChooser.this.isCrowd) {
                    if (SEAViewFileChooser.this.isNew) {
                        return file.getName().endsWith(".pyr");
                    }
                    else {
                        return file.getName().endsWith(".sea");
                    }
                }
                if (SEAViewFileChooser.this.isNew) {
                    return file.getName().endsWith(".pan");
                }
                return file.getName().endsWith(".sep");
            }


            public String getDescription() {
                if (SEAViewFileChooser.this.isCrowd) {
                    if (SEAViewFileChooser.this.isNew) {
                        return "Pyramid files (*.pyr)";
                    }
                    else {
                        return "SEA Annotation Files (*.sea)";
                    }
                }
                if (SEAViewFileChooser.this.isNew) {
                    return "Peer annotation files (*.pan)";
                }
                return "SEA Peer Annotation Files (*.sep)";
            }

            private ScuEduFileFilter() {
            }
        }
    }

    private class RemovalHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }
    }

    private static Object deepCopy(Object orig) {
        Object obj = null;

        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();


            java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bos.toByteArray()));

            obj = in.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    private class UndoController {

        private java.util.Vector states = new java.util.Vector();

        private boolean isUndoEnabled = false;
        private boolean isRedoEnabled = false;
        private boolean isActive = false;

        private int undoIndex = -1;

        private void expressGUI() {
            if (this.isActive) {
                SEAView.this.editUndoMenuItem.setEnabled(this.isUndoEnabled);
                SEAView.this.editRedoMenuItem.setEnabled(this.isRedoEnabled);
            }
        }


        public void setActive(boolean isActive) {
            this.isActive = isActive;
            expressGUI();
        }

        public void clear() {
            this.states.clear();
            this.undoIndex = -1;
            this.isUndoEnabled = (this.isRedoEnabled = false);
            expressGUI();
        }

        public void add(Object state) {
            this.undoIndex += 1;
            if (this.undoIndex < this.states.size())
                this.states.setSize(this.undoIndex);
            this.states.add(state);
            this.isUndoEnabled = (this.undoIndex > 0);
            this.isRedoEnabled = false;
            expressGUI();
        }


        public Object undo() {
            if (this.isUndoEnabled) {
                this.undoIndex -= 1;
                this.isUndoEnabled = (this.undoIndex > 0);
                this.isRedoEnabled = true;
                expressGUI();

                return SEAView.deepCopy(this.states.get(this.undoIndex));
            }


            return null;
        }


        public Object redo() {
            if (this.isRedoEnabled) {
                this.undoIndex += 1;
                this.isUndoEnabled = true;
                this.isRedoEnabled = (this.states.size() > this.undoIndex + 1);
                expressGUI();

                return SEAView.deepCopy(this.states.get(this.undoIndex));
            }


            return null;
        }

        private UndoController() {
        }
    }
}
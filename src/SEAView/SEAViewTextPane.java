package SEAView;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;

public class SEAViewTextPane extends JTextPane implements CaretListener, ActionListener {
    private SEAView seaView;
    private boolean ignoreNextCaretUpdate = false;
    private boolean ignoreAllCaretUpdates = false;
    private boolean rightClickMode = false;
    private String selectedText = null;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private short[] selectionIndexes;
    private short[] highlightIndexes;
    private SCUTree tree = null;
    private JPopupMenu popupMenu;
    private DragSource ds;
    private Transferable transferable;

    public SEAViewTextPane(SEAView seaView) {
        this.seaView = seaView;
        setEditable(false);
        addCaretListener(this);
        setDragEnabled(false);
        ds = new DragSource();
        transferable = new Transferable() {

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.stringFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.stringFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) {
                return selectedText;
            }
        };
        ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, new DragGestureListener() {
            @Override
            public void dragGestureRecognized(DragGestureEvent dge) {
                if (getSelectedText() != null) {
                    MouseEvent event = (MouseEvent) dge.getTriggerEvent();
                    Point point = event.getPoint();
                    int index = viewToModel(point);
                    if (getSelectionStartIndex() <= index && getSelectionEndIndex() >= index) {
                        if ((rightClickMode && SwingUtilities.isRightMouseButton(event)) || (!rightClickMode && SwingUtilities.isLeftMouseButton(event))) {
                            ds.startDrag(dge, DragSource.DefaultCopyDrop, transferable, new DragSourceListener() {

                                @Override
                                public void dragEnter(DragSourceDragEvent dsde) {
                                }

                                @Override
                                public void dragOver(DragSourceDragEvent dsde) {
                                }

                                @Override
                                public void dropActionChanged(DragSourceDragEvent dsde) {
                                }

                                @Override
                                public void dragExit(DragSourceEvent dse) {
                                }

                                @Override
                                public void dragDropEnd(DragSourceDropEvent dsde) {
                                    int selectionEnd = getSelectionEnd();
                                    setSelectionStart(selectionEnd);
                                    setSelectionEnd(selectionEnd);
                                }

                            });
                        }
                    }
                }
            }
        });
        setPreferredSize(new Dimension(500, 500));

        addStyle("plain", null);
        Style grayedStyle = addStyle("grayed", null);
        StyleConstants.setForeground(grayedStyle, java.awt.Color.blue);
        Style selectedStyle = addStyle("selected", null);
        StyleConstants.setBackground(selectedStyle, UIManager.getLookAndFeelDefaults().getColor("TextArea.selectionBackground"));
        StyleConstants.setForeground(selectedStyle, UIManager.getLookAndFeelDefaults().getColor("TextArea.selectionForeground"));
        Style underlinedStyle = addStyle("highlighted", null);
        StyleConstants.setBackground(underlinedStyle, new Color(255, 255, 100));

        this.popupMenu = new JPopupMenu("SCUs in which this text appears");
    }

    public void setTree(SCUTree tree) {
        this.tree = tree;
    }

    public void updateSelectedStyle() {
        Style selectedStyle = addStyle("selected", null);
        StyleConstants.setBackground(selectedStyle, UIManager.getLookAndFeelDefaults().getColor("TextArea.selectionBackground"));
        StyleConstants.setForeground(selectedStyle, UIManager.getLookAndFeelDefaults().getColor("TextArea.selectionForeground"));
    }

    public String getSelectedText() {
        return this.selectedText;
    }

    public int getSelectionStartIndex() {
        return this.selectionStart;
    }

    public int getSelectionEndIndex() {
        return this.selectionEnd;
    }

    public void setRightClickMode(boolean rightClickMode) {
        this.rightClickMode = rightClickMode;
    }

    public void setIgnoreNextCaretUpdate() {
        this.ignoreNextCaretUpdate = true;
    }

    public void caretUpdate(CaretEvent e) {
        if (getHighlighter() == null) {
            return;
        }
        if (this.ignoreNextCaretUpdate) {
            this.ignoreNextCaretUpdate = false;
            return;
        }

        if ((this.ignoreAllCaretUpdates) || (getText() == null) || (getText().length() == 0)) {
            return;
        }

        if (getSelectionEnd() - getSelectionStart() > 0) {
            this.selectionStart = getSelectionStart();
            this.selectionEnd = getSelectionEnd();


            char[] buff = getText().replaceAll("[\\t\\n\\r\\f\\,\\.\\-\\:]", " ").toCharArray();


            if (buff[this.selectionStart] != ' ') {

                while ((this.selectionStart > 0) && (buff[(this.selectionStart - 1)] != ' ')) {
                    this.selectionStart -= 1;
                }
            }


            while ((this.selectionStart < buff.length - 1) && (buff[this.selectionStart] == ' ')) {
                this.selectionStart += 1;
            }


            if (buff[(this.selectionEnd - 1)] != ' ') {

                while ((this.selectionEnd < buff.length - 1) && (buff[this.selectionEnd] != ' ')) {
                    this.selectionEnd += 1;
                }
            }


            while ((this.selectionEnd > 0) && (buff[(this.selectionEnd - 1)] == ' ')) {
                this.selectionEnd -= 1;
            }

            this.selectedText = getText().substring(this.selectionStart, this.selectionEnd);
            this.ignoreNextCaretUpdate = true;
            select(getSelectionEnd(), getSelectionEnd());

        } else {
            this.selectedText = null;


            Vector scuNodes = this.tree.getSCUNodesContainingIndex(getSelectionStart());
            seaView.table.selectEdusByIndex(getSelectionStart());

            if (scuNodes.size() == 1) {
                this.tree.selectSCUNode(((SCU) scuNodes.get(0)).getId());
            } else if (scuNodes.size() > 1) {
                this.popupMenu.removeAll();

                java.util.Enumeration scuNodeEnum = scuNodes.elements();
                while (scuNodeEnum.hasMoreElements()) {
                    SCU scu = (SCU) scuNodeEnum.nextElement();
                    JMenuItem menuItem = new JMenuItem(scu.toString());
                    menuItem.setActionCommand(String.valueOf(scu.getId()));
                    menuItem.addActionListener(this);
                    this.popupMenu.add(menuItem);
                }
                this.popupMenu.pack();
                try {
                    java.awt.Rectangle rect = modelToView(getSelectionStart());
                    this.popupMenu.show(this, rect.x, rect.y);
                } catch (javax.swing.text.BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        }
        updateTextColors();
    }

    public void loadFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
            buffer.append("\n");
        }
        loadText(buffer.toString());
        reader.close();
    }

    public void showText(final int position) {
        boolean prevVal = this.ignoreAllCaretUpdates;
        this.ignoreAllCaretUpdates = true;


        setCaretPosition(getText().length());
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SEAViewTextPane.this.setCaretPosition(position);
            }
        });
        this.ignoreAllCaretUpdates = false;
        this.ignoreAllCaretUpdates = prevVal;
        this.ignoreNextCaretUpdate = true;
    }

    public void loadText(String text) {
        this.ignoreAllCaretUpdates = true;
        setText(text);
        setCaretPosition(0);
        this.ignoreAllCaretUpdates = false;

        this.selectionIndexes = new short[getText().length()];
        for (int i = 0; i < this.selectionIndexes.length; i++) {
            this.selectionIndexes[i] = 0;
        }

        this.highlightIndexes = new short[getText().length()];
        for (int i = 0; i < this.highlightIndexes.length; i++) {
            this.highlightIndexes[i] = 0;
        }
    }

    public void modifyTextSelection(int startIndex, int endIndex, boolean selectionAdded) {
        int increment = selectionAdded ? 1 : -1;
        if (selectionAdded) {
            this.selectedText = null;
        }

        for (int i = startIndex; i <= endIndex; i++) {
            int tmp35_33 = i;
            short[] tmp35_30 = this.selectionIndexes;
            tmp35_30[tmp35_33] = ((short) (tmp35_30[tmp35_33] + increment));
        }
        updateTextColors();
    }

    public void modifyTextHighlight(int startIndex, int endIndex, boolean selectionAdded) {
        int increment = selectionAdded ? 1 : -1; // If selectionAdded, add highlights; if !selectionAdded, remove highlights

        for (int currentIndex = startIndex; currentIndex <= endIndex; currentIndex++) {
            this.highlightIndexes[currentIndex] = ((short) (this.highlightIndexes[currentIndex] + increment));
            if (this.highlightIndexes[currentIndex] < 0) {
                this.highlightIndexes[currentIndex] = 0;
            }
            if (this.highlightIndexes[currentIndex] > 1) {
                this.highlightIndexes[currentIndex] = 1;
            }
        }
        updateTextColors();
    }

    private void updateTextColors() {
        getStyledDocument().setCharacterAttributes(0, getText().length(), getStyle("plain"), true);


        for (int i = 0; i < this.selectionIndexes.length; i++) {
            if (this.selectionIndexes[i] > 0) {
                int j;

                for (j = i + 1; (j < this.selectionIndexes.length) && (this.selectionIndexes[j] > 0); j++) {
                }


                getStyledDocument().setCharacterAttributes(i, j - i, getStyle("grayed"), true);

                i = j;
            }
        }

        for (int i = 0; i < this.highlightIndexes.length; i++) {
            if (this.highlightIndexes[i] > 0) {
                int j;

                for (j = i + 1; (j < this.highlightIndexes.length) && (this.highlightIndexes[j] > 0); j++) {
                }


                getStyledDocument().setCharacterAttributes(i, j - i - 1, getStyle("highlighted"), false);

                i = j;
            }
        }


        if ((getSelectedText() != null) && (getSelectedText().length() > 0)) {
            getStyledDocument().setCharacterAttributes(getSelectionStartIndex(), getSelectionEndIndex() - getSelectionStartIndex(), getStyle("selected"), true);
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        this.tree.selectSCUNode(Integer.parseInt(e.getActionCommand()));
    }
}
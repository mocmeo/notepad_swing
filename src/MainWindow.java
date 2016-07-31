
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Date;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;

// render each font within fontnameList 
class FontNameRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        // create new label, and set its attributes corresponding to each rendering font
        // the value variable here is font family
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        Font font = new Font((String) value, Font.PLAIN, 15);
        label.setFont(font);
        return label;
    }
}

class FontStyleRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        // the value variable here is font style
        // deriveFont() -> create new font based on the previous one (size, style, but not font family)
        // the switch is to convert string type of font style to int type (Font.PLAIN, Font.ITALIC,...)
        label.setFont(new Font("Arial", Font.PLAIN, 15));
        switch ((String) value) {
            case "Regular": {
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
                break;
            }
            case "Italic": {
                label.setFont(label.getFont().deriveFont(Font.ITALIC));
                break;
            }
            case "Bold": {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                break;
            }
            case "Bold Italic": {
                label.setFont(label.getFont().deriveFont(Font.BOLD + Font.ITALIC));
                break;
            }
        }
        return label;
    }
}

public class MainWindow extends javax.swing.JFrame {

    private String filename = "";
    private String filepath = "";
    private boolean isSaveCancel = false;
    private boolean isSaveAs = false;
    protected UndoManager undoManager = new UndoManager();
    final static Color HILIT_COLOR = Color.LIGHT_GRAY;
    final static Color ERROR_COLOR = Color.PINK;
    private Highlighter hilighter;
    private Highlighter.HighlightPainter painter;
    private boolean isFindNext = false;
    FindDialog findDialog;
    int findNextPos;
    private boolean isSelectAll = false;
    private Object highlightTag = null;

    class MyThread extends Thread {

        public MyThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                    if (clipboardContents.getTransferData(DataFlavor.stringFlavor).equals(null)) {
                        pasteButton.setEnabled(false);
                        pasteMenu.setEnabled(false);
                        pastePopupMenu.setEnabled(false);

                    } else {
                        pasteButton.setEnabled(true);
                        pasteMenu.setEnabled(true);
                        pastePopupMenu.setEnabled(true);
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    public MainWindow() {
        initComponents();
        undo_redoCommand();

        // Initial font settings
        textArea.setFont(new Font("Arial", Font.PLAIN, 15));
        hilighter = textArea.getHighlighter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);
        textArea.setHighlighter(hilighter);

        String text = "hello world. How are you?";
        textArea.setText(text);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        statusMenu.setSelected(false);
        statusBar.setVisible(false);
        rightToLeftMenu.setSelected(false);

        this.setTitle("Untitled.txt - Notepad");

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(null), null);
        initButtonState();
        MyThread runner = new MyThread("mocmeo");
        runner.setDaemon(true);
        runner.start();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                int confirmed = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to exit?", "Quit",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                    quitMenu.doClick();
                }
            }
        });
    }

    public void initButtonState() {
        cutButton.setEnabled(false);
        cutMenu.setEnabled(false);
        cutPopupMenu.setEnabled(false);

        copyButton.setEnabled(false);
        copyMenu.setEnabled(false);
        copyPopupMenu.setEnabled(false);

        pasteButton.setEnabled(false);
        pasteMenu.setEnabled(false);
        pastePopupMenu.setEnabled(false);

        deleteMenu.setEnabled(false);
        deletePopupMenu.setEnabled(false);
    }

    public final void undo_redoCommand() {
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        undoMenu.setEnabled(false);
        redoMenu.setEnabled(false);

        textArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
                updateButtons();
            }
        });

        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    undoManager.undo();
                } catch (CannotRedoException cre) {
                }
                updateButtons();
            }
        });

        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    undoManager.redo();
                } catch (CannotRedoException cre) {
                }
                updateButtons();
            }
        });

        undoMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    undoManager.undo();
                } catch (CannotRedoException cre) {
                }
                updateButtons();
            }
        });

        redoMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    undoManager.redo();
                } catch (CannotRedoException cre) {
                }
                updateButtons();
            }
        });

        undoPopupMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    undoManager.undo();
                } catch (CannotRedoException cre) {
                }
                updateButtons();
            }
        });
    }

    public void updateButtons() {
        undoButton.setEnabled(undoManager.canUndo());
        redoButton.setEnabled(undoManager.canRedo());
        undoMenu.setEnabled(undoManager.canUndo());
        redoMenu.setEnabled(undoManager.canRedo());
        undoPopupMenu.setEnabled(undoManager.canUndo());
    }

    // being referenced by FontDialog later
    // we access textArea of parent frame through this function within FontDialog
    public JTextArea getTextArea() {
        return textArea;
    }

    public String getFileName() {
        return filename;
    }

    public Highlighter getHighlighter() {
        return hilighter;
    }

    public Highlighter.HighlightPainter getPainter() {
        return painter;
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public void setFilePath(String filepath) {
        this.filepath = filepath;
    }

    public void titleNameEmpty() {
        if (filename.isEmpty()) {
            this.setTitle("Untitled" + " - Notepad");
        } else {
            this.setTitle(filename + " - Notepad");
        }
    }

    public void setFindNext(boolean enable) {
        this.isFindNext = enable;
    }

    public void setHighlightTag(Object highlight) {
        highlightTag = highlight;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popUpMenu = new javax.swing.JPopupMenu();
        undoPopupMenu = new javax.swing.JMenuItem();
        separator1 = new javax.swing.JPopupMenu.Separator();
        cutPopupMenu = new javax.swing.JMenuItem();
        copyPopupMenu = new javax.swing.JMenuItem();
        pastePopupMenu = new javax.swing.JMenuItem();
        deletePopupMenu = new javax.swing.JMenuItem();
        separator2 = new javax.swing.JPopupMenu.Separator();
        selectallPopupMenu = new javax.swing.JMenuItem();
        rightToLeftMenu = new javax.swing.JCheckBoxMenuItem();
        jToolBar1 = new javax.swing.JToolBar();
        newButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        cutButton = new javax.swing.JButton();
        copyButton = new javax.swing.JButton();
        pasteButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        statusBar = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMMenu = new javax.swing.JMenu();
        newMenu = new javax.swing.JMenuItem();
        openMenu = new javax.swing.JMenuItem();
        saveMenu = new javax.swing.JMenuItem();
        saveasMenu = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        pageSetupMenu = new javax.swing.JMenuItem();
        printMenu = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        quitMenu = new javax.swing.JMenuItem();
        editMMenu = new javax.swing.JMenu();
        undoMenu = new javax.swing.JMenuItem();
        redoMenu = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        cutMenu = new javax.swing.JMenuItem();
        copyMenu = new javax.swing.JMenuItem();
        pasteMenu = new javax.swing.JMenuItem();
        deleteMenu = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        findMenu = new javax.swing.JMenuItem();
        findnextMenu = new javax.swing.JMenuItem();
        replaceMenu = new javax.swing.JMenuItem();
        gotoMenu = new javax.swing.JMenuItem();
        selectallMenu = new javax.swing.JMenuItem();
        timedateMenu = new javax.swing.JMenuItem();
        formatMMenu = new javax.swing.JMenu();
        wordwrapMenu = new javax.swing.JCheckBoxMenuItem();
        fontMenu = new javax.swing.JMenuItem();
        viewMMenu = new javax.swing.JMenu();
        statusMenu = new javax.swing.JCheckBoxMenuItem();
        helpMMenu = new javax.swing.JMenu();
        viewhelpMenu = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        aboutMenu = new javax.swing.JMenuItem();

        undoPopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undoPopupMenu.setText("Undo");
        popUpMenu.add(undoPopupMenu);
        popUpMenu.add(separator1);

        cutPopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutPopupMenu.setText("Cut");
        cutPopupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutPopupMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(cutPopupMenu);

        copyPopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyPopupMenu.setText("Copy");
        copyPopupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyPopupMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(copyPopupMenu);

        pastePopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pastePopupMenu.setText("Paste");
        pastePopupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pastePopupMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(pastePopupMenu);

        deletePopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        deletePopupMenu.setText("Delete");
        deletePopupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deletePopupMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(deletePopupMenu);
        popUpMenu.add(separator2);

        selectallPopupMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        selectallPopupMenu.setText("Select All");
        selectallPopupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectallPopupMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(selectallPopupMenu);

        rightToLeftMenu.setSelected(true);
        rightToLeftMenu.setText("Right to left Reading order");
        rightToLeftMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightToLeftMenuActionPerformed(evt);
            }
        });
        popUpMenu.add(rightToLeftMenu);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jToolBar1.setRollover(true);

        newButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/new.png"))); // NOI18N
        newButton.setFocusable(false);
        newButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(newButton);

        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/open.png"))); // NOI18N
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(openButton);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/save.png"))); // NOI18N
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButton);
        jToolBar1.add(jSeparator1);

        cutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/cut.png"))); // NOI18N
        cutButton.setFocusable(false);
        cutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(cutButton);

        copyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/copy.png"))); // NOI18N
        copyButton.setFocusable(false);
        copyButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(copyButton);

        pasteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/paste.png"))); // NOI18N
        pasteButton.setFocusable(false);
        pasteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pasteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        pasteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(pasteButton);
        jToolBar1.add(jSeparator2);

        undoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/undo.png"))); // NOI18N
        undoButton.setFocusable(false);
        undoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        undoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(undoButton);

        redoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/redo.png"))); // NOI18N
        redoButton.setFocusable(false);
        redoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        redoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(redoButton);

        textArea.setColumns(20);
        textArea.setRows(5);
        textArea.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                textAreaCaretUpdate(evt);
            }
        });
        textArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                textAreaMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                textAreaMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(textArea);

        statusBar.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        statusBar.setText("Ln 1, Col 1");

        fileMMenu.setText("File");

        newMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/new.png"))); // NOI18N
        newMenu.setText("New");
        newMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(newMenu);

        openMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/open.png"))); // NOI18N
        openMenu.setText("Open...");
        openMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(openMenu);

        saveMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/save.png"))); // NOI18N
        saveMenu.setText("Save");
        saveMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(saveMenu);

        saveasMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        saveasMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/save.png"))); // NOI18N
        saveasMenu.setText("Save as...");
        saveasMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveasMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(saveasMenu);
        fileMMenu.add(jSeparator7);

        pageSetupMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/pagesetup.png"))); // NOI18N
        pageSetupMenu.setText("Page Setup...");
        pageSetupMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pageSetupMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(pageSetupMenu);

        printMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        printMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/printer.png"))); // NOI18N
        printMenu.setText("Print...");
        printMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(printMenu);
        fileMMenu.add(jSeparator3);

        quitMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK));
        quitMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/quit.png"))); // NOI18N
        quitMenu.setText("Quit");
        quitMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuActionPerformed(evt);
            }
        });
        fileMMenu.add(quitMenu);

        jMenuBar1.add(fileMMenu);

        editMMenu.setText("Edit");

        undoMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/undo.png"))); // NOI18N
        undoMenu.setText("Undo");
        editMMenu.add(undoMenu);

        redoMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        redoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/redo.png"))); // NOI18N
        redoMenu.setText("Redo");
        editMMenu.add(redoMenu);
        editMMenu.add(jSeparator4);

        cutMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/cut.png"))); // NOI18N
        cutMenu.setText("Cut");
        cutMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutMenuActionPerformed(evt);
            }
        });
        editMMenu.add(cutMenu);

        copyMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/copy.png"))); // NOI18N
        copyMenu.setText("Copy");
        copyMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuActionPerformed(evt);
            }
        });
        editMMenu.add(copyMenu);

        pasteMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/paste.png"))); // NOI18N
        pasteMenu.setText("Paste");
        pasteMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuActionPerformed(evt);
            }
        });
        editMMenu.add(pasteMenu);

        deleteMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        deleteMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/delete.png"))); // NOI18N
        deleteMenu.setText("Delete");
        deleteMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuActionPerformed(evt);
            }
        });
        editMMenu.add(deleteMenu);
        editMMenu.add(jSeparator5);

        findMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/find.png"))); // NOI18N
        findMenu.setText("Find...");
        findMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMenuActionPerformed(evt);
            }
        });
        editMMenu.add(findMenu);

        findnextMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        findnextMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/find.png"))); // NOI18N
        findnextMenu.setText("Find Next");
        findnextMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findnextMenuActionPerformed(evt);
            }
        });
        editMMenu.add(findnextMenu);

        replaceMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
        replaceMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/replace.png"))); // NOI18N
        replaceMenu.setText("Replace...");
        replaceMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceMenuActionPerformed(evt);
            }
        });
        editMMenu.add(replaceMenu);

        gotoMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        gotoMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/goto.png"))); // NOI18N
        gotoMenu.setText("Go To...");
        gotoMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoMenuActionPerformed(evt);
            }
        });
        editMMenu.add(gotoMenu);

        selectallMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        selectallMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/selectall.png"))); // NOI18N
        selectallMenu.setText("Select All");
        selectallMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectallMenuActionPerformed(evt);
            }
        });
        editMMenu.add(selectallMenu);

        timedateMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        timedateMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/timedate.png"))); // NOI18N
        timedateMenu.setText("Time/Date");
        timedateMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timedateMenuActionPerformed(evt);
            }
        });
        editMMenu.add(timedateMenu);

        jMenuBar1.add(editMMenu);

        formatMMenu.setText("Format");

        wordwrapMenu.setSelected(true);
        wordwrapMenu.setText("Word Wrap");
        wordwrapMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wordwrapMenuActionPerformed(evt);
            }
        });
        formatMMenu.add(wordwrapMenu);

        fontMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/font.png"))); // NOI18N
        fontMenu.setText("Font...");
        fontMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fontMenuActionPerformed(evt);
            }
        });
        formatMMenu.add(fontMenu);

        jMenuBar1.add(formatMMenu);

        viewMMenu.setText("View");

        statusMenu.setSelected(true);
        statusMenu.setText("Status Bar");
        statusMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusMenuActionPerformed(evt);
            }
        });
        viewMMenu.add(statusMenu);

        jMenuBar1.add(viewMMenu);

        helpMMenu.setText("Help");

        viewhelpMenu.setText("View Help");
        viewhelpMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewhelpMenuActionPerformed(evt);
            }
        });
        helpMMenu.add(viewhelpMenu);
        helpMMenu.add(jSeparator6);

        aboutMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/about.png"))); // NOI18N
        aboutMenu.setText("About Notepad");
        aboutMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuActionPerformed(evt);
            }
        });
        helpMMenu.add(aboutMenu);

        jMenuBar1.add(helpMMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 687, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
            .addComponent(statusBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusBar))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fontMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fontMenuActionPerformed
        // open up FontDialog
        FontDialog fontDialog = new FontDialog(this, true);
        fontDialog.setVisible(true);
    }//GEN-LAST:event_fontMenuActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        // if save action is canceled, no action is allowed to perform
        saveButtonActionPerformed(evt);
        if (isSaveCancel) {
            isSaveCancel = false;
            return;
        }

        JFileChooser fc = new JFileChooser();

        setFileName("");
        textArea.setText("");
        fc.setMultiSelectionEnabled(false);

        FileFilter type1 = new ExtensionFilter(".java", "Java source file");
        FileFilter type2 = new ExtensionFilter(".txt", "Text file");
        fc.addChoosableFileFilter(type1);
        fc.addChoosableFileFilter(type2);
        fc.setFileFilter(type2);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // if users click on Open
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            // get selected file
            File f = fc.getSelectedFile();
            if (f != null && (f.getName().endsWith(".txt") || f.getName().endsWith(".java"))) {
                try {
                    LineNumberReader lines = new LineNumberReader(new FileReader(f));
                    String line = "";
                    while ((line = lines.readLine()) != null) {
                        textArea.append(line + "\n");
                    }
                    lines.close();
                    setFileName(f.getName());
                    setFilePath(fc.getSelectedFile().getAbsolutePath());
                } catch (Exception e) {
                    textArea.setText("" + e);
                }
            }
        }
        titleNameEmpty();
    }//GEN-LAST:event_openButtonActionPerformed

    private void openMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuActionPerformed
        // TODO add your handling code here:
        openButtonActionPerformed(evt);
    }//GEN-LAST:event_openMenuActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        isFindNext = false;
        JFileChooser fc = new JFileChooser();
        String fname = getFileName();

        // ------ SAVE FILE BEFORE OPENING ---------
        // textArea has content, fname is empty 
        //    -> the situation in which user typed some texts, then opened new file -> run
        // fname was assigned, textArea is empty 
        //    -> probably user clear file content -> run
        // if fname is empty (no file has been opened) and textArea has no content 
        //    -> return
        if (!(fname.isEmpty() && textArea.getText().isEmpty())) {
            // follow notepad convention
            if (fname.isEmpty()) {
                fname = "Untitled.txt";
            }

            int result = JOptionPane.showConfirmDialog(this, "Do you want to save changes to "
                    + fname + "?", "Notepad", JOptionPane.YES_NO_CANCEL_OPTION);

            // confirmation dialog pops up
            switch (result) {
                case JOptionPane.YES_OPTION:
                    BufferedWriter writer;
                    File file = new File(fname);
                    int sf;

                    // there's no file has been saved
                    //   -> we have to save the file first
                    // or file was saved (filepath's not empty), and saveAsAction was called
                    if (filepath.isEmpty() || isSaveAs) {
                        isSaveAs = false;
                        fc.setSelectedFile(file);
                        sf = fc.showSaveDialog(null);

                        // the default path of showSaveDialog() is /home/username
                        // which did not match the actual saving folder for fname (file)
                        // File file = new File(fname);  -> lead us to the project directory.
                        // assign filepath to getAbsolutePath() to keep track of states
                        // where we already have the file displayed on textArea or not.
                        filepath = fc.getSelectedFile().getAbsolutePath();
                        if (sf == JFileChooser.APPROVE_OPTION) {
                            try {
                                writer = new BufferedWriter(new FileWriter(filepath));
                                writer.write(textArea.getText());
                                writer.close();
                                setFileName(fc.getSelectedFile().getName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // when user choose YES option, then Cancel the Save dialog,
                            // filepath will be automatically assigned to the project directory 
                            //   -> file is saved without permission, when hit Save again, 
                            //   the Save dialog didn't show up.
                            // The problem is, we must let users specify the location before saving.
                            // to fix that, and to have another chance to open the Save dialog, 
                            // we can do as following:
                            filepath = "";
                            isSaveCancel = true;
                        }
                    } else {
                        // the file has been saved, and we did modify it
                        // we need to save it before opening new file.
                        try {
                            writer = new BufferedWriter(new FileWriter(filepath));
                            writer.write(textArea.getText());
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case JOptionPane.NO_OPTION:
                    // do not save the file
                    break;
                case JOptionPane.CLOSED_OPTION:
                case JOptionPane.CANCEL_OPTION:
                    // dispose the dialog, and return to where you have left before
                    // isSaveCancel: newAction, openAction, quitAction relied on saveAction
                    //  -> no action is allowed to run without saving.
                    titleNameEmpty();
                    isSaveCancel = true;
                    return;
            }
        }
        titleNameEmpty();
    }//GEN-LAST:event_saveButtonActionPerformed


    private void saveasMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveasMenuActionPerformed
        // TODO add your handling code here:
        isSaveAs = true;
        saveButtonActionPerformed(evt);
    }//GEN-LAST:event_saveasMenuActionPerformed

    private void saveMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuActionPerformed
        // TODO add your handling code here:
        saveButtonActionPerformed(evt);
    }//GEN-LAST:event_saveMenuActionPerformed

    private void newMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuActionPerformed
        // if save action is canceled, newAction is not allowed to perform
        saveButtonActionPerformed(evt);
        if (isSaveCancel) {
            isSaveCancel = false;
            return;
        }
        textArea.setText("");
        filename = "";
        filepath = "";
        titleNameEmpty();
    }//GEN-LAST:event_newMenuActionPerformed

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        newMenuActionPerformed(evt);
    }//GEN-LAST:event_newButtonActionPerformed

    private void quitMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuActionPerformed
        saveButtonActionPerformed(evt);
        if (isSaveCancel) {
            isSaveCancel = false;
            return;
        }
        System.exit(0);
    }//GEN-LAST:event_quitMenuActionPerformed

    public static void setClipBoard(String content) {
        StringSelection stringSelection;
        stringSelection = new StringSelection(content);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    private void cutMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutMenuActionPerformed
        // TODO add your handling code here:
        textArea.cut();
        for (Highlighter.Highlight highlight : hilighter.getHighlights()) {
            int index = highlight.getStartOffset();
            int end = highlight.getEndOffset();

            setClipBoard(textArea.getText().substring(index, end));
            textArea.replaceRange("", index, end);
        }
        highlightTag = null;
    }//GEN-LAST:event_cutMenuActionPerformed

    private void copyMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuActionPerformed
        // TODO add your handling code here:
        textArea.copy();
        for (Highlighter.Highlight highlight : hilighter.getHighlights()) {
            int index = highlight.getStartOffset();
            int end = highlight.getEndOffset();
            setClipBoard(textArea.getText().substring(index, end));
        }
    }//GEN-LAST:event_copyMenuActionPerformed

    private void pasteMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuActionPerformed
        // TODO add your handling code here:
        textArea.paste();
    }//GEN-LAST:event_pasteMenuActionPerformed

    private void deleteMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuActionPerformed
        // TODO add your handling code here:
        textArea.replaceSelection("");
        if (isSelectAll) {
            textArea.setText("");
        }

        for (Highlighter.Highlight highlight : hilighter.getHighlights()) {
            textArea.replaceRange("", highlight.getStartOffset(), highlight.getEndOffset());
        }
        highlightTag = null;
    }//GEN-LAST:event_deleteMenuActionPerformed

    private void cutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutButtonActionPerformed
        // TODO add your handling code here:
        cutMenuActionPerformed(evt);

    }//GEN-LAST:event_cutButtonActionPerformed

    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
        // TODO add your handling code here:
        copyMenuActionPerformed(evt);
    }//GEN-LAST:event_copyButtonActionPerformed

    private void pasteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteButtonActionPerformed
        // TODO add your handling code here:
        pasteMenuActionPerformed(evt);
    }//GEN-LAST:event_pasteButtonActionPerformed

    private void findMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMenuActionPerformed
        // TODO add your handling code here:
        findDialog = new FindDialog(this);
        findDialog.setVisible(true);
        findDialog.setAlwaysOnTop(true);
    }//GEN-LAST:event_findMenuActionPerformed

    private void findnextMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findnextMenuActionPerformed
        if (!isFindNext) {
            JOptionPane.showMessageDialog(this, "Keyword not found.");
            return;
        }

        findNextPos = textArea.getCaretPosition();
        hilighter.removeAllHighlights();

        String keyword = findDialog.getKeyword();
        if (keyword.isEmpty()) {
            return;
        }

        String content = textArea.getText();
        int index = content.indexOf(keyword, findNextPos);
        if (findDialog.getFindCase()) {
            index = content.toLowerCase().indexOf(keyword.toLowerCase(), findNextPos);
        }

        if (findDialog.getBackWard()) {
            index = content.lastIndexOf(keyword, findNextPos - 1);
            if (findDialog.getFindCase()) {
                index = content.toLowerCase().lastIndexOf(keyword.toLowerCase(), findNextPos - 1);
            }
        }

        if (index >= 0) {   // match found
            try {
                int end = index + keyword.length();
                findNextPos = end;
                highlightTag = hilighter.addHighlight(index, end, painter);

                textArea.setCaretPosition(end);
                if (findDialog.getBackWard()) {
                    textArea.setCaretPosition(index);
                    highlightTag = hilighter.addHighlight(index, end, painter);
                }
                isFindNext = true;
            } catch (BadLocationException e) {
            }
        } else {
            highlightTag = null;
            JOptionPane.showMessageDialog(this, "Cannot find \"" + keyword + "\"",
                    "Notepad", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_findnextMenuActionPerformed

    private void replaceMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceMenuActionPerformed
        ReplaceDialog replaceDialog = new ReplaceDialog(this);
        replaceDialog.setVisible(true);
        replaceDialog.setAlwaysOnTop(true);
    }//GEN-LAST:event_replaceMenuActionPerformed

    private void selectallMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectallMenuActionPerformed
        textArea.selectAll();
    }//GEN-LAST:event_selectallMenuActionPerformed

    private void timedateMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timedateMenuActionPerformed
        Date date = new Date();
        textArea.insert(date.toString(), textArea.getCaretPosition());
    }//GEN-LAST:event_timedateMenuActionPerformed

    private void gotoMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoMenuActionPerformed
        GoToLineDialog gotoDialog = new GoToLineDialog(this, true);
        gotoDialog.setVisible(true);
    }//GEN-LAST:event_gotoMenuActionPerformed

    private void wordwrapMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wordwrapMenuActionPerformed
        if (wordwrapMenu.isSelected()) {
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
        } else {
            textArea.setWrapStyleWord(false);
            textArea.setLineWrap(false);
        }
    }//GEN-LAST:event_wordwrapMenuActionPerformed

    private void aboutMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuActionPerformed
        AboutDialog aboutDialog = new AboutDialog(this, true);
        aboutDialog.setVisible(true);
    }//GEN-LAST:event_aboutMenuActionPerformed

    private void viewhelpMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewhelpMenuActionPerformed
        try {
            String url = "http://go.microsoft.com/fwlink/?LinkId=517009";
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }//GEN-LAST:event_viewhelpMenuActionPerformed

    private void statusMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusMenuActionPerformed
        if (statusMenu.isSelected()) {
            statusBar.setVisible(true);
        } else {
            statusBar.setVisible(false);
        }
    }//GEN-LAST:event_statusMenuActionPerformed

    private void printMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printMenuActionPerformed
        PrintDialog printDialog = new PrintDialog(this, true);
        printDialog.setVisible(true);
    }//GEN-LAST:event_printMenuActionPerformed

    private void pageSetupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pageSetupMenuActionPerformed
        PageSetupDialog pageSetupDialog = new PageSetupDialog(this, true);
        pageSetupDialog.setVisible(true);
    }//GEN-LAST:event_pageSetupMenuActionPerformed

    private void cutPopupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutPopupMenuActionPerformed
        cutButtonActionPerformed(evt);
    }//GEN-LAST:event_cutPopupMenuActionPerformed

    private void copyPopupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyPopupMenuActionPerformed
        copyButtonActionPerformed(evt);
    }//GEN-LAST:event_copyPopupMenuActionPerformed

    private void pastePopupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pastePopupMenuActionPerformed
        pasteButtonActionPerformed(evt);
    }//GEN-LAST:event_pastePopupMenuActionPerformed

    private void deletePopupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deletePopupMenuActionPerformed
        deleteMenuActionPerformed(evt);
    }//GEN-LAST:event_deletePopupMenuActionPerformed

    private void selectallPopupMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectallPopupMenuActionPerformed
        selectallMenuActionPerformed(evt);
    }//GEN-LAST:event_selectallPopupMenuActionPerformed

    private void rightToLeftMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightToLeftMenuActionPerformed
        if (rightToLeftMenu.isSelected()) {
            textArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            textArea.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        }
    }//GEN-LAST:event_rightToLeftMenuActionPerformed

    public void setSelectionButton(boolean isEnable) {
        cutButton.setEnabled(isEnable);
        cutMenu.setEnabled(isEnable);
        cutPopupMenu.setEnabled(isEnable);

        copyButton.setEnabled(isEnable);
        copyMenu.setEnabled(isEnable);
        copyPopupMenu.setEnabled(isEnable);

        deleteMenu.setEnabled(isEnable);
        deletePopupMenu.setEnabled(isEnable);
    }
    
    private void textAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textAreaMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            popUpMenu.show(textArea, evt.getX(), evt.getY());
        }
        if (highlightTag != null) {
            hilighter = textArea.getHighlighter();
            hilighter.removeAllHighlights();
            highlightTag = null;
            setSelectionButton(false);
        }
    }//GEN-LAST:event_textAreaMouseReleased

    private void textAreaCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_textAreaCaretUpdate
        updateStatus();
        int dot = evt.getDot();
        int mark = evt.getMark();

        if (dot == mark && highlightTag == null) {
            setSelectionButton(false);
        } else {
            setSelectionButton(true);
        }
    }//GEN-LAST:event_textAreaCaretUpdate

    private void textAreaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textAreaMouseClicked
    }//GEN-LAST:event_textAreaMouseClicked

    public void updateStatus() {
        int linenum = 1;
        int columnnum = 1;

        try {
            int caretpos = textArea.getCaretPosition();
            linenum = textArea.getLineOfOffset(caretpos);
            columnnum = caretpos - textArea.getLineStartOffset(linenum);
            linenum += 1;
        } catch (Exception ex) {
        }
        statusBar.setText("Ln " + linenum + ", Col " + columnnum);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow().setVisible(true);
            }
        });

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenu;
    private javax.swing.JButton copyButton;
    private javax.swing.JMenuItem copyMenu;
    private javax.swing.JMenuItem copyPopupMenu;
    private javax.swing.JButton cutButton;
    private javax.swing.JMenuItem cutMenu;
    private javax.swing.JMenuItem cutPopupMenu;
    private javax.swing.JMenuItem deleteMenu;
    private javax.swing.JMenuItem deletePopupMenu;
    private javax.swing.JMenu editMMenu;
    private javax.swing.JMenu fileMMenu;
    private javax.swing.JMenuItem findMenu;
    private javax.swing.JMenuItem findnextMenu;
    private javax.swing.JMenuItem fontMenu;
    private javax.swing.JMenu formatMMenu;
    private javax.swing.JMenuItem gotoMenu;
    private javax.swing.JMenu helpMMenu;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton newButton;
    private javax.swing.JMenuItem newMenu;
    private javax.swing.JButton openButton;
    private javax.swing.JMenuItem openMenu;
    private javax.swing.JMenuItem pageSetupMenu;
    private javax.swing.JButton pasteButton;
    private javax.swing.JMenuItem pasteMenu;
    private javax.swing.JMenuItem pastePopupMenu;
    private javax.swing.JPopupMenu popUpMenu;
    private javax.swing.JMenuItem printMenu;
    private javax.swing.JMenuItem quitMenu;
    private javax.swing.JButton redoButton;
    private javax.swing.JMenuItem redoMenu;
    private javax.swing.JMenuItem replaceMenu;
    private javax.swing.JCheckBoxMenuItem rightToLeftMenu;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem saveMenu;
    private javax.swing.JMenuItem saveasMenu;
    private javax.swing.JMenuItem selectallMenu;
    private javax.swing.JMenuItem selectallPopupMenu;
    private javax.swing.JPopupMenu.Separator separator1;
    private javax.swing.JPopupMenu.Separator separator2;
    private javax.swing.JLabel statusBar;
    private javax.swing.JCheckBoxMenuItem statusMenu;
    private javax.swing.JTextArea textArea;
    private javax.swing.JMenuItem timedateMenu;
    private javax.swing.JButton undoButton;
    private javax.swing.JMenuItem undoMenu;
    private javax.swing.JMenuItem undoPopupMenu;
    private javax.swing.JMenu viewMMenu;
    private javax.swing.JMenuItem viewhelpMenu;
    private javax.swing.JCheckBoxMenuItem wordwrapMenu;
    // End of variables declaration//GEN-END:variables
}

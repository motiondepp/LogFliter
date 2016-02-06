package com.lehome.tool.view;

import com.lehome.tool.T;
import com.lehome.tool.Utils;
import com.lehome.tool.model.DumpsysViewTableModel;
import com.lehome.tool.presenter.DumpsysViewPresenter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.StringTokenizer;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class DumpsysViewDialog extends JDialog implements ActionListener {
    private final JButton mNextSearchResultButton;
    private final JButton mPreSearchResultButton;
    private final JTextField mSearchTextField;
    private JTable mMainTable;
    private JButton mRefreshButton;
    private JButton mSaveButton;
    private JProgressBar mProgressBar;

    private DumpsysViewDialogListener mListener;
    private DumpsysViewPresenter mDumpsysViewPresenter;
    private DumpsysViewTableModel mDumpsysViewTableModel;
    private String mSearchTarget = "";

    public DumpsysViewDialog(JFrame frame, String title, String deviceId, String cmd, DumpsysViewDialogListener listener) {
        super(frame, title, true);

        mDumpsysViewPresenter = new DumpsysViewPresenter(this, deviceId, cmd);
        this.mListener = listener;

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.setPreferredSize(new Dimension(960, 600));
        initMainTable(c);

        JPanel actionPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        mProgressBar = new JProgressBar();
        mProgressBar.setVisible(false);
        buttonPanel.add(mProgressBar);

        mRefreshButton = new JButton("Refresh");
        mRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mDumpsysViewPresenter.onRefreshButtonPressed();
            }
        });
        buttonPanel.add(mRefreshButton);
        mSaveButton = new JButton("Save");
        mSaveButton.addActionListener(mSaveButtonActionlistener);
        buttonPanel.add(mSaveButton);
        actionPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        mSearchTextField = new JTextField();
        mSearchTextField.setPreferredSize(new Dimension(150, 20));
        mSearchTextField.getDocument().addDocumentListener(mSearchDocumentListener);
        mSearchTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (mSearchTextField.getText() != null && mSearchTextField.getText().length() != 0) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ENTER: {
                            mDumpsysViewPresenter.onNextSearchResultButtonPressed();
                        }
                        break;
                    }
                }
            }
        });
        searchPanel.add(mSearchTextField);

        JButton cleanButton = new JButton("clean");
        cleanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mSearchTextField.setText("");
            }
        });
        searchPanel.add(cleanButton);

        mPreSearchResultButton = new JButton("<");
        mPreSearchResultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mDumpsysViewPresenter.onPreSearchResultButtonPressed();
            }
        });
        searchPanel.add(mPreSearchResultButton);

        mNextSearchResultButton = new JButton(">");
        mNextSearchResultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mDumpsysViewPresenter.onNextSearchResultButtonPressed();
            }
        });
        searchPanel.add(mNextSearchResultButton);

        actionPanel.add(searchPanel, BorderLayout.CENTER);
        c.add(actionPanel, BorderLayout.SOUTH);
        this.pack();
        mDumpsysViewPresenter.onStart();
    }

    private void initMainTable(Container c) {
        mDumpsysViewTableModel = new DumpsysViewTableModel();
        mMainTable = new JTable(mDumpsysViewTableModel);
//        mMainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mMainTable.getTableHeader().setReorderingAllowed(false);
        mMainTable.setOpaque(false);
        mMainTable.setAutoscrolls(false);
        mMainTable.setIntercellSpacing(new Dimension(0, 0));

        mMainTable.getColumnModel().getColumn(0).setResizable(true);
        mMainTable.getColumnModel().getColumn(0).setMinWidth(600);
        mMainTable.getColumnModel().getColumn(0).setPreferredWidth(960);

//        mMainTable.getTableHeader().addMouseListener(new ColumnHeaderListener());
        mMainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = mMainTable.rowAtPoint(p);
                if (row < 0 || row > mMainTable.getRowCount()) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    String value = (String) mMainTable.getModel().getValueAt(row, 0);
                    if (e.getClickCount() == 2) {
                        if (mListener != null) {
                            mListener.onRowDoubleClick(value);
                        }
                    } else if (e.getClickCount() == 1) {
                        if (mListener != null) {
                            mListener.onRowSingleClick(value);
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                        JPopupMenu popup = createRightClickPopUp();
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        for (int iIndex = 0; iIndex < mMainTable.getColumnCount(); iIndex++) {
            mMainTable.getColumnModel().getColumn(iIndex).setCellRenderer(new DumpsysCellRenderer());
        }

        JScrollPane scrollPane = new JScrollPane(mMainTable);
        c.add(scrollPane, BorderLayout.CENTER);

        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK, false);
        mMainTable.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);

        mMainTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                boolean altPressed = ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK);
                boolean ctrlPressed = ((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F: {
                        if (ctrlPressed) {
                            mSearchTextField.requestFocus();
                        }
                    }
                    break;
                    default:
                        break;
                }
            }
        });
    }

    public JTable getMainTable() {
        return mMainTable;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        copySelectedRows();
    }

    private ActionListener mSaveButtonActionlistener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FileDialog fd = new FileDialog(DumpsysViewDialog.this, "File Save", FileDialog.SAVE);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                mDumpsysViewPresenter.saveDumpsysInfoToFile(new File(fd.getDirectory() + fd.getFile()));
            }
        }
    };

    private DocumentListener mSearchDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (e.getDocument().equals(mSearchTextField.getDocument())) {
                onSearchTextChanged();
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument().equals(mSearchTextField.getDocument())) {
                onSearchTextChanged();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (e.getDocument().equals(mSearchTextField.getDocument())) {
                onSearchTextChanged();
            }
        }

        private void onSearchTextChanged() {
            mSearchTarget = mSearchTextField.getText();
            mMainTable.invalidate();
            mMainTable.repaint();
        }
    };

    public String getSearchTarget() {
        return mSearchTarget;
    }

    public void setSearchTarget(String mSearchTarget) {
        this.mSearchTarget = mSearchTarget;
    }

    public class ColumnHeaderListener extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {

            if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                JTable table = ((JTableHeader) evt.getSource()).getTable();
                TableColumnModel colModel = table.getColumnModel();

                // The index of the column whose header was clicked
                int vColIndex = colModel.getColumnIndexAtX(evt.getX());

                if (vColIndex == -1) {
                    T.d("vColIndex == -1");
                    return;
                }
                packColumn(vColIndex, 1);
            }
        }
    }

    private JPopupMenu createRightClickPopUp() {

        JPopupMenu menuPopup = new JPopupMenu();
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });

        menuPopup.add(copyRowToClipboard);

        return menuPopup;
    }

    private void copySelectedRows() {
        int[] rowsSelected = mMainTable.getSelectedRows();
        StringBuffer sb = new StringBuffer();
        for (int row : rowsSelected) {
            sb.append(mMainTable.getValueAt(row, 0));
            sb.append("\n");
        }
        Utils.sendContentToClipboard(sb.toString());
    }

    public void packColumn(int vColIndex, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) mMainTable.getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width = 0;

        JViewport viewport = (JViewport) mMainTable.getParent();
        Rectangle viewRect = viewport.getViewRect();
        int nFirst = mMainTable.rowAtPoint(new Point(0, viewRect.y));
        int nLast = mMainTable.rowAtPoint(new Point(0, viewRect.height - 1));

        if (nLast < 0) {
            nLast = mMainTable.getRowCount();
        }
        // Get width of column header
        TableCellRenderer renderer;
        Component comp;
        // Get maximum width of column data
        for (int r = nFirst; r < nFirst + nLast; r++) {
            renderer = mMainTable.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(
                    mMainTable, mMainTable.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // Add margin
        width += 2 * margin;

        // Set the width
        col.setPreferredWidth(width);
    }

    public void onStartLoadingPackages() {
        mProgressBar.setVisible(true);
        mProgressBar.setIndeterminate(true);
        mRefreshButton.setEnabled(false);
    }

    public void onStopLoadingPackages() {
        mProgressBar.setVisible(false);
        mProgressBar.setIndeterminate(false);
        mRefreshButton.setEnabled(true);
    }

    public class DumpsysCellRenderer extends DefaultTableCellRenderer {

        boolean m_bChanged;

        public DumpsysCellRenderer() {
            super();
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            if (value != null) {
                value = remakeData(value.toString());
            }
            Component c = super.getTableCellRendererComponent(table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
            return c;
        }

        String remakeData(String strText) {
            m_bChanged = false;
            strText = strText.replace(" ", "\u00A0");
            strText = remakeFind(strText, mSearchTarget, "#00FF00", true);
            if (m_bChanged)
                strText = "<html><nobr>" + strText + "</nobr></html>";

            return strText.replace("\t", "    ");
        }

        String remakeFind(String strText, String strFind, String strColor, boolean bUseSpan) {
            if (strFind == null || strFind.length() <= 0) return strText;

            strFind = strFind.replace(" ", "\u00A0");
            StringTokenizer stk = new StringTokenizer(strFind, "|");
            String newText;
            String strToken;

            while (stk.hasMoreElements()) {
                strToken = stk.nextToken();

                int idx = strText.toLowerCase().indexOf(strToken.toLowerCase());
                if (idx != -1) {
                    String prefix = strText.substring(0, idx);
                    String suffix = strText.substring(idx + strToken.length());
                    String target = strText.substring(idx, idx + strToken.length());

                    if (bUseSpan)
                        newText = "<span style=\"background-color:" + strColor + "\"><b>";
                    else
                        newText = "<font color=" + strColor + "><b>";
                    newText += target;
                    if (bUseSpan)
                        newText += "</b></span>";
                    else
                        newText += "</b></font>";
                    strText = prefix + newText + suffix;
                    m_bChanged = true;
                }
            }
            return strText;
        }
    }

    public void changeTableSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend, boolean bMove) {
        if (rowIndex < 0)
            rowIndex = 0;
        if (rowIndex > mMainTable.getRowCount() - 1)
            rowIndex = mMainTable.getRowCount() - 1;
        mMainTable.changeSelection(rowIndex, columnIndex, toggle, extend);
        if (bMove)
            showRow(rowIndex);
    }

    public void showRow(int row) {
        if (row < 0) row = 0;
        if (row > mMainTable.getRowCount() - 1)
            row = mMainTable.getRowCount() - 1;

        Rectangle rList = mMainTable.getVisibleRect();
        Rectangle rCell = mMainTable.getCellRect(row, 0, true);
        if (rList != null) {
            Rectangle scrollToRect = new Rectangle((int) rList.getX(), (int) rCell.getY(), (int) (rList.getWidth()), (int) rCell.getHeight());
            mMainTable.scrollRectToVisible(scrollToRect);
        }
    }

    public interface DumpsysViewDialogListener {

        void onRowSingleClick(String value);

        void onRowDoubleClick(String value);
    }
}
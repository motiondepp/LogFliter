package com.bt.tool;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.BitSet;

public class SubLogTable extends JTable implements FocusListener, ActionListener, ILogRenderResolver {
    private static final long serialVersionUID = 1L;

    LogFilterMain m_LogFilterMain;
    String m_strFilterFind;
    String m_strSearchHighlight;
    String m_strHighlight;
    String m_strPidShow;
    String m_strTidShow;
    String m_strTagShow;
    String m_strTagRemove;
    String m_strTagBookmark;
    float m_fFontSize;
    boolean m_bAltPressed;

    boolean[] m_arbShow;
    public int mMaxShownCol = 0;
    public int mMinShownCol = LogFilterTableModel.COMUMN_MAX;

    public SubLogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel);
        m_LogFilterMain = filterMain;
        m_strSearchHighlight = "";
        m_strFilterFind = "";
        m_strHighlight = "";
        m_strPidShow = "";
        m_strTidShow = "";
        m_strTagShow = "";
        m_strTagRemove = "";
        m_strTagBookmark = "";
        m_arbShow = new boolean[LogFilterTableModel.COMUMN_MAX];
        init();
        setColumnWidth();
    }

    private void init() {
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK, false);
        registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);

        addFocusListener(this);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getTableHeader().setReorderingAllowed(false);
        m_fFontSize = 12;
        setOpaque(false);
        setAutoscrolls(false);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        for (int iIndex = 0; iIndex < getColumnCount(); iIndex++) {
            getColumnModel().getColumn(iIndex).setCellRenderer(new LogCellRenderer(this, this));
        }

        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        showInfoInLogTable(logInfo);
                    } else if (m_bAltPressed) {
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int colum = columnAtPoint(p);

                    boolean hasSelected = false;
                    for (int sRow : getSelectedRows()) {
                        if (sRow == row) {
                            hasSelected = true;
                            break;
                        }
                    }
                    if (!hasSelected) {
                        setRowSelectionInterval(row, row);
                        setColumnSelectionInterval(colum, colum);
                    }

                    T.d("m_bAltPressed = " + m_bAltPressed);
                    if (m_bAltPressed) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                    } else {
                        if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                            JPopupMenu popup = createRightClickPopUp();
                            popup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        getTableHeader().addMouseListener(new ColumnHeaderListener());
    }

    private void showInfoInLogTable(LogInfo logInfo) {
        m_LogFilterMain.notiEvent(
                new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_SELECTION, logInfo)
        );
    }

    private JPopupMenu createRightClickPopUp() {

        JPopupMenu menuPopup = new JPopupMenu();

        JMenuItem markMenuItem = new JMenuItem(new AbstractAction("remove") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRows();
                LogInfo[] selectedInfo = new LogInfo[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedInfo[i] = ((LogFilterTableModel) getModel()).getRow(selectedRows[i]);
                }
                for (int i = 0; i < selectedRows.length; i++) {
                    LogInfo info = selectedInfo[i];
                    m_LogFilterMain.markLogInfo(selectedRows[i], info.getLine() - 1, !info.isMarked());
                }

            }
        });
        menuPopup.add(markMenuItem);

        JMenuItem copyToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });
        menuPopup.add(copyToClipboard);

        if (getSelectedRowCount() == 1) {
            JMenuItem showInLogTable = new JMenuItem(new AbstractAction("show in log table") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                    showInfoInLogTable(logInfo);
                }
            });
            menuPopup.add(showInLogTable);
        }

        return menuPopup;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    String GetFilterShowPid() {
        return m_strPidShow;
    }

    String GetFilterShowTid() {
        return m_strTidShow;
    }

    public String GetFilterShowTag() {
        return m_strTagShow;
    }

    public String GetFilterFind() {
        return m_strFilterFind;
    }

    public String GetHighlight() {
        return m_strHighlight;
    }

    public String GetSearchHighlight() {
        return m_strSearchHighlight;
    }

    public String GetFilterRemoveTag() {
        return m_strTagRemove;
    }

    public String GetFilterBookmarkTag() {
        return m_strTagBookmark;
    }

    int getVisibleRowCount() {
        return getVisibleRect().height / getRowHeight();
    }

    void setFilterFind(String strFind) {
        m_strFilterFind = strFind;
    }

    public void hideColumn(int nColumn) {
        getColumnModel().getColumn(nColumn).setWidth(0);
        getColumnModel().getColumn(nColumn).setMinWidth(0);
        getColumnModel().getColumn(nColumn).setMaxWidth(0);
        getColumnModel().getColumn(nColumn).setPreferredWidth(0);
        getColumnModel().getColumn(nColumn).setResizable(false);
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        m_bAltPressed = e.isAltDown();
//        if(e.getID() == KeyEvent.KEY_RELEASED)
        {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_END:
                    changeSelection(getRowCount() - 1, 0, false, false);
                    return true;
                case KeyEvent.VK_HOME:
                    changeSelection(0, 0, false, false);
                    return true;
            }
        }
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    public void packColumn(int vColIndex, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width = 0;

        JViewport viewport = (JViewport) getParent();
        Rectangle viewRect = viewport.getViewRect();
        int nFirst = this.rowAtPoint(new Point(0, viewRect.y));
        int nLast = this.rowAtPoint(new Point(0, viewRect.height - 1));

        if (nLast < 0) {
            nLast = this.getRowCount();
        }
        // Get width of column header
        TableCellRenderer renderer;
        Component comp;
        // Get maximum width of column data
        for (int r = nFirst; r < nFirst + nLast; r++) {
            renderer = getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(
                    this, getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // Add margin
        width += 2 * margin;

        // Set the width
        col.setPreferredWidth(width);
        LogFilterTableModel.ColWidth[vColIndex] = width;
    }

    @Override
    public boolean isColumnShown(int idx) {
        return m_arbShow[idx];
    }

    @Override
    public int getMinShownColumn() {
        return mMinShownCol;
    }

    @Override
    public int getMaxShownColumn() {
        return mMaxShownCol;
    }

    public float getFontSize() {
        return m_fFontSize;
    }

    public void showColumn(int nColumn, boolean bShow) {
        m_arbShow[nColumn] = bShow;
        if (bShow) {
            if (mMaxShownCol <= nColumn)
                mMaxShownCol = nColumn;
            if (mMinShownCol >= nColumn)
                mMinShownCol = nColumn;
            getColumnModel().getColumn(nColumn).setResizable(true);
            getColumnModel().getColumn(nColumn).setMaxWidth(LogFilterTableModel.ColWidth[nColumn] * 1000);
            getColumnModel().getColumn(nColumn).setMinWidth(1);
            getColumnModel().getColumn(nColumn).setWidth(LogFilterTableModel.ColWidth[nColumn]);
            getColumnModel().getColumn(nColumn).setPreferredWidth(LogFilterTableModel.ColWidth[nColumn]);
        } else {
            if (nColumn >= mMaxShownCol) {
                for (int i = m_arbShow.length - 1; i >= 0; i--) {
                    if (m_arbShow[i]) {
                        mMaxShownCol = i;
                        break;
                    }
                }
            }
            if (nColumn <= mMinShownCol) {
                for (int i = 0; i < m_arbShow.length; i++) {
                    if (m_arbShow[i]) {
                        mMinShownCol = i;
                        break;
                    }
                }
            }
            hideColumn(nColumn);
        }
    }

    public void setColumnWidth() {
        for (int iIndex = 0; iIndex < getColumnCount(); iIndex++) {
            showColumn(iIndex, true);
        }
    }

    void SetFilterShowTag(String strShowTag) {
        m_strTagShow = strShowTag;
    }

    void SetFilterShowPid(String strShowPid) {
        m_strPidShow = strShowPid;
    }

    void SetFilterShowTid(String strShowTid) {
        m_strTidShow = strShowTid;
    }

    void SetHighlight(String strHighlight) {
        m_strHighlight = strHighlight;
    }

    void SetSearchHighlight(String strHighlight) {
        m_strSearchHighlight = strHighlight;
    }

    void SetFilterRemoveTag(String strRemoveTag) {
        m_strTagRemove = strRemoveTag;
    }


    public void SetFilterBookmarkTag(String strTagBookmark) {
        m_strTagBookmark = strTagBookmark;
    }

    public void setFontSize(int nFontSize) {
        m_fFontSize = nFontSize;
        setRowHeight(nFontSize + 4);
    }

    public void setValueAt(Object aValue, int row, int column) {
        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
        if (column == LogFilterTableModel.COMUMN_BOOKMARK) {
            logInfo.setBookmark((String) aValue);
            m_LogFilterMain.setBookmark(logInfo.getLine() - 1, (String) aValue);
        }
    }

    boolean isInnerRect(Rectangle parent, Rectangle child) {
        return parent.y <= child.y && (parent.y + parent.height) >= (child.y + child.height);
    }

    public void showRow(int row) {
        if (row < 0) row = 0;
        if (row > getRowCount() - 1)
            row = getRowCount() - 1;

        Rectangle rList = getVisibleRect();
        Rectangle rCell = getCellRect(row, 0, true);
        if (rList != null) {
            Rectangle scrollToRect = new Rectangle((int) rList.getX(), (int) rCell.getY(), (int) (rList.getWidth()), (int) rCell.getHeight());
            scrollRectToVisible(scrollToRect);
        }
    }

    public void showRowCenterIfNotInRect(int row, boolean changeSelection) {
        if (changeSelection) {
            changeSelection(row, 0, false, false);
        }
        int nVisible = row;
        if (0 <= nVisible && nVisible < getRowCount()) {
            Rectangle parent = getVisibleRect();
            Rectangle child = getCellRect(row, 0, true);
            if (!isInnerRect(parent, child)) {
                if (child.y + child.height > parent.y + parent.height) {
                    nVisible = row + getVisibleRowCount() / 2;
                } else if (child.y < parent.y) {
                    nVisible = row - getVisibleRowCount() / 2;
                }
            }
        }
        showRow(nVisible);
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

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        m_bAltPressed = false;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        copySelectedRows();
    }

    public void copySelectedRows() {
        Utils.sendContentToClipboard(getFormatSelectedRows(LogFilterTableModel.COMUMN_LINE));
    }

    public String getFormatSelectedRows(int... exceptColumnIndex) {
        StringBuilder sbf = new StringBuilder();
        int numRows = getSelectedRowCount();
        int[] rowsSelected = getSelectedRows();

        BitSet checkBit = new BitSet();
        for (int i = 0; exceptColumnIndex != null && i < exceptColumnIndex.length; i++) {
            checkBit.set(exceptColumnIndex[i]);
        }
        int[] maxColWidth = new int[m_arbShow.length];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < m_arbShow.length; j++) {
                if (checkBit.get(j))
                    continue;
                String colStr = String.valueOf(getValueAt(rowsSelected[i], j)).trim();
                if (colStr.length() > maxColWidth[j]) {
                    maxColWidth[j] = colStr.length();
                }
            }
        }

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < m_arbShow.length; j++) {
                if (m_arbShow[j] && maxColWidth[j] != 0) {
                    StringBuilder strTemp = new StringBuilder(String.valueOf(getValueAt(rowsSelected[i], j)).trim());
                    if (j != LogFilterTableModel.COMUMN_MESSAGE) {
                        int len = strTemp.length();
                        for (int k = 0; k < maxColWidth[j] - len; k++)
                            strTemp.append(" ");
                        strTemp.append("  ");
                    }
                    sbf.append(strTemp);
                }
            }
            sbf.append("\n");
        }
        sbf.deleteCharAt(sbf.length() - 1); // remove last \n
        return sbf.toString();
    }
}

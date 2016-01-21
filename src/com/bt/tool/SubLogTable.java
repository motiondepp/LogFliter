package com.bt.tool;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.BitSet;
import java.util.StringTokenizer;

public class SubLogTable extends JTable implements FocusListener, ActionListener {
    private static final long serialVersionUID = 1L;

    LogFilterMain m_LogFilterMain;
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

    public SubLogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel);
        m_LogFilterMain = filterMain;
        m_strSearchHighlight = "";
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

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        changeSelection(rowIndex, columnIndex, toggle, extend, true);
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend, boolean bMove) {
        if (rowIndex < 0) rowIndex = 0;
        if (rowIndex > getRowCount() - 1) rowIndex = getRowCount() - 1;
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
        if (bMove)
            showRow(rowIndex);
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
            getColumnModel().getColumn(iIndex).setCellRenderer(new LogCellRenderer());
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
                for (int selectedRow : selectedRows) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(selectedRow);
                    logInfo.setMarked(!logInfo.isMarked());
                    m_LogFilterMain.bookmarkItem(selectedRow, logInfo.getLine() - 1, logInfo.isMarked());
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

    String GetFilterShowTag() {
        return m_strTagShow;
    }

    String GetHighlight() {
        return m_strHighlight;
    }

    String GetSearchHighlight() {
        return m_strSearchHighlight;
    }

    String GetFilterRemoveTag() {
        return m_strTagRemove;
    }

    String GetFilterBookmarkTag() {
        return m_strTagBookmark;
    }

    int getVisibleRowCount() {
        return getVisibleRect().height / getRowHeight();
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

    public float getFontSize() {
        return m_fFontSize;
    }

    public int getColumnWidth(int nColumn) {
        return getColumnModel().getColumn(nColumn).getWidth();
    }

    public void showColumn(int nColumn, boolean bShow) {
        m_arbShow[nColumn] = bShow;
        if (bShow) {
            getColumnModel().getColumn(nColumn).setResizable(true);
            getColumnModel().getColumn(nColumn).setMaxWidth(LogFilterTableModel.ColWidth[nColumn] * 1000);
            getColumnModel().getColumn(nColumn).setMinWidth(1);
            getColumnModel().getColumn(nColumn).setWidth(LogFilterTableModel.ColWidth[nColumn]);
            getColumnModel().getColumn(nColumn).setPreferredWidth(LogFilterTableModel.ColWidth[nColumn]);
        } else
            hideColumn(nColumn);
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

    public class LogCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        boolean m_bChanged;

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
            if (value != null) {
                value = remakeData(column, value.toString());
            }
            Component c = super.getTableCellRendererComponent(table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
            c.setFont(getFont().deriveFont(m_fFontSize));
            c.setForeground(logInfo.getTextColor());
            if (isSelected) {
                if (logInfo.isMarked())
                    c.setBackground(new Color(LogColor.COLOR_BOOKMARK2));
            } else
                c.setBackground(Color.WHITE);

            return c;
        }

        String remakeData(int nIndex, String strText) {

            m_bChanged = false;

            strText = strText.replace(" ", "\u00A0");
            if (LogColor.COLOR_HIGHLIGHT != null && LogColor.COLOR_HIGHLIGHT.length > 0) {
                strText = remakeFind(strText, GetHighlight(), LogColor.COLOR_HIGHLIGHT, true);
            } else {
                strText = remakeFind(strText, GetHighlight(), "#00FF00", true);
            }

            if (nIndex == LogFilterTableModel.COMUMN_TAG) {
                strText = remakeFind(strText, GetFilterShowTag(), "#FF0000", false);
            }

            strText = remakeFind(strText, GetSearchHighlight(), "#FFFF00", true);
            if (m_bChanged)
                strText = "<html><nobr>" + strText + "</nobr></html>";

            return strText.replace("\t", "    ");
        }

        String remakeFind(String strText, String strFind, String[] arColor, boolean bUseSpan) {
            if (strFind == null || strFind.length() <= 0) return strText;

            strFind = strFind.replace(" ", "\u00A0");
            StringTokenizer stk = new StringTokenizer(strFind, "|");
            String newText;
            String strToken;
            int nIndex = 0;

            while (stk.hasMoreElements()) {
                if (nIndex >= arColor.length)
                    nIndex = 0;
                strToken = stk.nextToken();

                int idx = strText.toLowerCase().indexOf(strToken.toLowerCase());
                if (idx != -1) {
                    String prefix = strText.substring(0, idx);
                    String suffix = strText.substring(idx + strToken.length());
                    String target = strText.substring(idx, idx + strToken.length());

                    if (bUseSpan)
                        newText = "<span style=\"background-color:#" + arColor[nIndex] + "\"><b>";
                    else
                        newText = "<font color=#" + arColor[nIndex] + "><b>";
                    newText += target;
                    if (bUseSpan)
                        newText += "</b></span>";
                    else
                        newText += "</b></font>";
                    strText = prefix + newText + suffix;
                    m_bChanged = true;
                    nIndex++;
                }
            }
            return strText;
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

    public void showRow(int row) {
        if (row < 0) row = 0;
        if (row > getRowCount() - 1) row = getRowCount() - 1;

        Rectangle rList = getVisibleRect();
        Rectangle rCell = getCellRect(row, 0, true);
        if (rList != null) {
            Rectangle scrollToRect = new Rectangle((int) rList.getX(), (int) rCell.getY(), (int) (rList.getWidth()), (int) rCell.getHeight());
            scrollRectToVisible(scrollToRect);
        }
    }

    public void showRow(int row, boolean bCenter) {
        int nLastSelectedIndex = getSelectedRow();

        changeSelection(row, 0, false, false);
        int nVisible = row;
        if (nLastSelectedIndex <= row || nLastSelectedIndex == -1)
            nVisible = row + getVisibleRowCount() / 2;
        else
            nVisible = row - getVisibleRowCount() / 2;
        if (nVisible < 0) nVisible = 0;
        else if (nVisible > getRowCount() - 1) nVisible = getRowCount() - 1;
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
                    if (j == LogFilterTableModel.COMUMN_BOOKMARK) {
                        strTemp.insert(0, "<").append(">");
                    }
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

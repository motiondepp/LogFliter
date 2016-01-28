package com.bt.tool;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xinyu.he on 2016/1/28.
 */
public abstract class BaseLogTable extends JTable implements FocusListener, ActionListener, ILogRenderResolver {

    LogFilterMain m_LogFilterMain;
    String m_strSearchHighlight;
    String m_strHighlight;
    String m_strPidShow;
    String m_strTidShow;
    String m_strTagShow;
    String m_strTagRemove;
    String m_strTagBookmark;
    String m_strFilterRemove;
    String m_strFilterFind;
    float m_fFontSize;
    boolean[] m_arbShow;
    boolean m_bAltPressed;

    private long mFilterToTime = -1;
    private long mFilterFromTime = -1;

    public int mMaxShownCol = 0;
    public int mMinShownCol = LogFilterTableModel.COMUMN_MAX - 1;
    protected int mMaxSelectedRow;
    protected int mMinSelectedRow;
    private LogInfo m_latestSelectLogInfo;
    private int mLastSelectedRow;
    private LogInfoHistory curHistoryInfo = new LogInfoHistory();

    public BaseLogTable(TableModel model, LogFilterMain filterMain) {
        super(model);
        m_LogFilterMain = filterMain;
        m_strSearchHighlight = "";
        m_strHighlight = "";
        m_strPidShow = "";
        m_strTidShow = "";
        m_strTagShow = "";
        m_strTagRemove = "";
        m_strTagBookmark = "";
        m_strFilterRemove = "";
        m_strFilterFind = "";
        m_arbShow = new boolean[LogFilterTableModel.COMUMN_MAX];
        init();
        setColumnWidth();
    }

    protected void init() {
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
    }


    public void changeSelection(LogInfo target, boolean toggle, boolean extend) {
        for (int nIndex = 0; nIndex < getRowCount(); nIndex++) {
            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.getLine() != null && target.getLine() == logInfo.getLine()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        changeSelection(rowIndex, columnIndex, toggle, extend, true);
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend, boolean bMove) {
        changeSelection(rowIndex, columnIndex, toggle, extend, bMove, true);
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend, boolean bMove, boolean addHistory) {
        if (rowIndex < 0) rowIndex = 0;
        if (rowIndex > getRowCount() - 1) rowIndex = getRowCount() - 1;
        super.changeSelection(rowIndex, columnIndex, toggle, extend);

        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(rowIndex);
        if (logInfo != m_latestSelectLogInfo) {
            m_LogFilterMain.onSelectedRowChanged(mLastSelectedRow, rowIndex, logInfo);
            if (addHistory)
                handleHistorySelectionChanged(logInfo);
        }
        m_latestSelectLogInfo = logInfo;
        mLastSelectedRow = rowIndex;
        if (bMove)
            showRow(rowIndex);
    }

    public LogInfo getLatestSelectedLogInfo() {
        return m_latestSelectLogInfo;
    }

    private void handleHistorySelectionChanged(LogInfo logInfo) {
        if (curHistoryInfo.next == null || curHistoryInfo.next.value != logInfo) {
            LogInfoHistory newHistory = new LogInfoHistory();
            newHistory.value = logInfo;
            newHistory.prev = curHistoryInfo;
            newHistory.next = null;
            curHistoryInfo.next = newHistory;
            curHistoryInfo = newHistory;
        }
    }

    public void historyBack() {
        if (curHistoryInfo.prev == null || curHistoryInfo.prev.value == null) {
            return;
        }
        LogInfo preInfo = curHistoryInfo.prev.value;
        for (int nIndex = 0; nIndex < getRowCount(); nIndex++) {
            LogInfo info = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (info == preInfo) {
                curHistoryInfo = curHistoryInfo.prev;
                changeSelection(nIndex, -1, false, false, true, false);
                return;
            }
        }
    }

    public void historyForward() {
        if (curHistoryInfo.next == null) {
            return;
        }
        LogInfo preInfo = curHistoryInfo.next.value;
        for (int nIndex = 0; nIndex < getRowCount(); nIndex++) {
            LogInfo info = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (info == preInfo) {
                curHistoryInfo = curHistoryInfo.next;
                changeSelection(nIndex, -1, false, false, true, false);
                return;
            }
        }
    }

    public int getColumnWidth(int nColumn) {
        return getColumnModel().getColumn(nColumn).getWidth();
    }

    public void setColumnWidth() {
        for (int iIndex = 0; iIndex < getColumnCount(); iIndex++) {
            showColumn(iIndex, true);
        }
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

    public void hideColumn(int nColumn) {
        getColumnModel().getColumn(nColumn).setWidth(0);
        getColumnModel().getColumn(nColumn).setMinWidth(0);
        getColumnModel().getColumn(nColumn).setMaxWidth(0);
        getColumnModel().getColumn(nColumn).setPreferredWidth(0);
        getColumnModel().getColumn(nColumn).setResizable(false);
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

    int getVisibleRowCount() {
        return getVisibleRect().height / getRowHeight();
    }

    boolean isInnerRect(Rectangle parent, Rectangle child) {
        return parent.y <= child.y && (parent.y + parent.height) >= (child.y + child.height);
    }

    public void showRowCenterIfNotInRect(int row, boolean changeSelection) {
        if (changeSelection) {
            changeSelection(row, 0, false, false, false);
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

    @Override
    public void actionPerformed(ActionEvent arg0) {
        copySelectedRows();
    }

    public void copySelectedColumn(int[] column) {
        Set<Integer> target = new HashSet<>();
        for (int j = 0; j < m_arbShow.length; j++) {
            target.add(j);
        }
        for (int col : column)
            target.remove(col);
        target.add(LogFilterTableModel.COMUMN_LINE);

        int[] result = new int[target.size()];
        int i = 0;
        for (int col : target) {
            result[i++] = col;
        }
        Utils.sendContentToClipboard(getFormatSelectedRows(result));
    }

    public void copySelectedRows() {
        Utils.sendContentToClipboard(getFormatSelectedRows(new int[]{LogFilterTableModel.COMUMN_LINE}));
    }

    public String getFormatSelectedRows(int[] exceptColumnIndex) {
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


    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        m_bAltPressed = false;
    }

    void setFilterFind(String strFind) {
        m_strFilterFind = strFind;
    }

    void SetFilterRemove(String strRemove) {
        m_strFilterRemove = strRemove;
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

    public boolean isCellEditable(int row, int column) {
        return false;
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

    public String GetFilterFind() {
        return m_strFilterFind;
    }

    public String GetFilterRemove() {
        return m_strFilterRemove;
    }

    public String GetFilterShowPid() {
        return m_strPidShow;
    }

    public String GetFilterShowTid() {
        return m_strTidShow;
    }

    public String GetFilterShowTag() {
        return m_strTagShow;
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


    public void SetFilterFromTime(String fromTime) {
        if (fromTime == null || fromTime.length() == 0) {
            mFilterFromTime = -1;
            return;
        }

        fromTime = getVaildTimeString(fromTime);
        try {
            mFilterFromTime = LogCatParser.TIMESTAMP_FORMAT.parse(fromTime).getTime();
        } catch (ParseException e) {
            mFilterFromTime = -1;
        }
    }

    public void SetFilterToTime(String toTime) {
        if (toTime == null || toTime.length() == 0) {
            mFilterToTime = -1;
            return;
        }

        toTime = getVaildTimeString(toTime);
        try {
            mFilterToTime = LogCatParser.TIMESTAMP_FORMAT.parse(toTime).getTime();
        } catch (ParseException e) {
            mFilterToTime = -1;
        }
    }

    private String getVaildTimeString(String srcTime) {
        String[] tParts = srcTime.split("[:\\.]");
        if (tParts.length > 4)
            return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String part = "0";
            if (i < tParts.length) {
                part = tParts[i];
            }
            sb.append(String.format("%-2s", part).replace(' ', '0'));
            sb.append(":");
        }
        sb.deleteCharAt(sb.length() - 1);

        if (tParts.length == 4) {
            sb.append(".").append(tParts[3]);
        }

        if (tParts.length == 3) {
            sb.append(".000");
        } else if (tParts[3].length() < 3) {
            srcTime = sb.append(String.format("%-3s", tParts[3]).replace(' ', '0')).toString();
        }
        return srcTime;
    }

    public long GetFilterFromTime() {
        return mFilterFromTime;
    }

    public long GetFilterToTime() {
        return mFilterToTime;
    }

    void gotoNextBookmark() {
        int nSeletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        for (int nIndex = nSeletectRow + 1; nIndex < getRowCount(); nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.isMarked()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }

        for (int nIndex = 0; nIndex < nSeletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.isMarked()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }
    }

    void gotoPreBookmark() {
        int nSeletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        for (int nIndex = nSeletectRow - 1; nIndex >= 0; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.isMarked()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }

        for (int nIndex = getRowCount() - 1; nIndex > nSeletectRow; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.isMarked()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }
    }

    void gotoNextSearchResult() {
        if (m_strSearchHighlight == null || m_strSearchHighlight.length() == 0)
            return;

        int nSeletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        for (int nIndex = nSeletectRow + 1; nIndex < getRowCount(); nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                return;
            }
        }

        for (int nIndex = 0; nIndex < nSeletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                return;
            }
        }
    }

    void gotoPreSearchResult() {
        if (m_strSearchHighlight == null || m_strSearchHighlight.length() == 0)
            return;
        int nSeletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        for (int nIndex = nSeletectRow - 1; nIndex >= 0; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                return;
            }
        }

        for (int nIndex = getRowCount() - 1; nIndex > nSeletectRow; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                return;
            }
        }
    }

    public void searchSimilarForward(String cmd) {
        int seletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        int minWeight = Integer.MAX_VALUE;
        int minIdx = seletectRow;
        for (int nIndex = seletectRow + 1; nIndex < getRowCount(); nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.getMessage(), 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("next minWeight = " + minWeight);
            showRowCenterIfNotInRect(minIdx, true);
            return;
        }

        for (int nIndex = 0; nIndex < seletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.getMessage(), 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("pre minWeight = " + minWeight);
            showRowCenterIfNotInRect(minIdx, true);
            return;
        }
    }

    public void searchSimilarBackward(String cmd) {
        int seletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        int minWeight = Integer.MAX_VALUE;
        int minIdx = seletectRow;
        for (int nIndex = 0; nIndex < seletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.getMessage(), 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("pre minWeight = " + minWeight);
            showRowCenterIfNotInRect(minIdx, true);
            return;
        }

        for (int nIndex = seletectRow + 1; nIndex < getRowCount(); nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.getMessage(), 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("next minWeight = " + minWeight);
            showRowCenterIfNotInRect(minIdx, true);
            return;
        }
    }
}

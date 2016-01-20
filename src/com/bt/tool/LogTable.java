package com.bt.tool;

import com.bt.tool.diff.DiffService;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.BitSet;
import java.util.StringTokenizer;

public class LogTable extends JTable implements FocusListener, ActionListener {
    private static final long serialVersionUID = 1L;

    LogFilterMain m_LogFilterMain;
    ILogParser m_iLogParser;
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
    boolean m_bAltPressed;
    boolean[] m_arbShow;

    private LogInfo m_latestSelectLogInfo;
    private int mLastSelectedRow;
    private LogInfoHistory curHistoryInfo = new LogInfoHistory();
    private long mFilterToTime = -1;
    private long mFilterFromTime = -1;

    public LogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel);
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

    private void init() {
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK, false);
        registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);

        addFocusListener(this);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getTableHeader().setReorderingAllowed(false);
        m_fFontSize = 12;
        setOpaque(false);
        setAutoscrolls(false);
//        setRequestFocusEnabled(false);

//        setGridColor(TABLE_GRID_COLOR);
        setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually in order to paint
        // grid lines over the entire viewport.
        setShowGrid(false);

        for (int iIndex = 0; iIndex < getColumnCount(); iIndex++) {
            getColumnModel().getColumn(iIndex).setCellRenderer(new LogCellRenderer());
        }

        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                if (SwingUtilities.isLeftMouseButton(e)) {
//                    if (e.getClickCount() == 1){
//                        m_latestSelectLogInfo = ((LogFilterTableModel)getModel()).getRow(row);
////                        T.d("cur sel: " + m_latestSelectLogInfo.m_intLine);
//                     }
                    if (e.getClickCount() == 2) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        logInfo.m_bMarked = !logInfo.m_bMarked;
                        m_LogFilterMain.bookmarkItem(row, logInfo.m_intLine - 1, logInfo.m_bMarked);
                    } else if (m_bAltPressed) {
                        int colum = columnAtPoint(p);
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        if (colum == LogFilterTableModel.COMUMN_TAG) {
                            if (m_strTagShow.contains("|" + logInfo.getData(colum)))
                                m_strTagShow = m_strTagShow.replace("|" + logInfo.getData(colum), "");
                            else if (m_strTagShow.contains((String) logInfo.getData(colum)))
                                m_strTagShow = m_strTagShow.replace((String) logInfo.getData(colum), "");
                            else
                                m_strTagShow += "|" + logInfo.getData(colum);
                            m_LogFilterMain.notiEvent(new INotiEvent.EventParam(INotiEvent.EVENT_CHANGE_FILTER_SHOW_TAG));
                        } else if (colum == LogFilterTableModel.COMUMN_TIME) {
                            m_LogFilterMain.notiEvent(
                                    new INotiEvent.EventParam(INotiEvent.EVENT_CHANGE_FILTER_FROM_TIME, logInfo.m_strTime)
                            );
                        }
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
                        if (colum == LogFilterTableModel.COMUMN_TAG) {
                            m_strTagRemove += "|" + logInfo.getData(colum);
                            m_LogFilterMain.notiEvent(new INotiEvent.EventParam(INotiEvent.EVENT_CHANGE_FILTER_REMOVE_TAG));
                        } else if (colum == LogFilterTableModel.COMUMN_TIME) {
                            m_LogFilterMain.notiEvent(
                                    new INotiEvent.EventParam(INotiEvent.EVENT_CHANGE_FILTER_TO_TIME, logInfo.m_strTime)
                            );
                        }
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
        getColumnModel().addColumnModelListener(mTableColumnWidthListener);
    }

    private JPopupMenu createRightClickPopUp() {

        JPopupMenu menuPopup = new JPopupMenu();
        JMenuItem copyToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });
        JMenuItem markMenuItem = new JMenuItem(new AbstractAction("mark/unmark") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRows();
                for (int selectedRow : selectedRows) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(selectedRow);
                    logInfo.m_bMarked = !logInfo.m_bMarked;
                    m_LogFilterMain.bookmarkItem(selectedRow, logInfo.m_intLine - 1, logInfo.m_bMarked);
                }
            }
        });
        JMenuItem findInDiffMenuItem = new JMenuItem(new AbstractAction("find in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getData(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    m_LogFilterMain.mDiffService.writeDiffCommand(
                            DiffService.DiffServiceCmdType.FIND,
                            target
                    );
                }
            }
        });

        JMenuItem findSimilarInDiffMenuItem = new JMenuItem(new AbstractAction("find similar in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getData(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    m_LogFilterMain.mDiffService.writeDiffCommand(
                            DiffService.DiffServiceCmdType.FIND_SIMILAR,
                            target
                    );
                }
            }
        });

        JMenuItem compareMenuItem = new JMenuItem(new AbstractAction("compare with selected in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String target = getFormatSelectedRows(LogFilterTableModel.COMUMN_LINE, LogFilterTableModel.COMUMN_DATE);
                if (target != null && target.length() != 0) {
                    m_LogFilterMain.mDiffService.writeDiffCommand(
                            DiffService.DiffServiceCmdType.COMPARE,
                            target
                    );
                }
            }
        });

        menuPopup.add(markMenuItem);
        menuPopup.add(copyToClipboard);
        if (m_LogFilterMain.mDiffService.isDiffConnected()) {
            if (getSelectedRowCount() == 1) {
                menuPopup.add(findInDiffMenuItem);
                menuPopup.add(findSimilarInDiffMenuItem);
            } else {
                menuPopup.add(compareMenuItem);
            }
        }

        return menuPopup;
    }

    public boolean isCellEditable(int row, int column) {
        return column == LogFilterTableModel.COMUMN_BOOKMARK;
    }

    boolean isInnerRect(Rectangle parent, Rectangle child) {
        return parent.y <= child.y && (parent.y + parent.height) >= (child.y + child.height);
    }

    String GetFilterFind() {
        return m_strFilterFind;
    }

    String GetFilterRemove() {
        return m_strFilterRemove;
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


    public void SetFilterFromTime(String fromTime) {
        if (fromTime == null || fromTime.length() == 0) {
            mFilterFromTime = -1;
            return;
        }

        String[] tParts = fromTime.split("[:\\.]");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String part = "0";
            if (i < tParts.length) {
                part = tParts[i];
            }
            if (part.length() > 2)
                break;
            sb.append(String.format("%-2s", part).replace(' ', '0'));
            sb.append(":");
        }
        fromTime = sb.deleteCharAt(sb.length() - 1).toString();

        int idx = fromTime.indexOf(".");
        if (idx == -1) {
            fromTime += ".000";
        } else if (fromTime.length() - idx < 4) {
            String sss = fromTime.substring(idx + 1);
            fromTime = fromTime.substring(0, idx + 1) + String.format("%-3s", sss).replace(' ', '0');
        }

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

        String[] tParts = toTime.split("[:\\.]");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String part = "0";
            if (i < tParts.length) {
                part = tParts[i];
            }
            if (part.length() > 2)
                break;
            sb.append(String.format("%-2s", part).replace(' ', '0'));
            sb.append(":");
        }
        toTime = sb.deleteCharAt(sb.length() - 1).toString();

        int idx = toTime.indexOf(".");
        if (idx == -1) {
            toTime += ".000";
        } else if (toTime.length() - idx < 4) {
            String sss = toTime.substring(idx + 1);
            toTime = toTime.substring(0, idx + 1) + String.format("%-3s", sss).replace(' ', '0');
        }
        try {
            mFilterToTime = LogCatParser.TIMESTAMP_FORMAT.parse(toTime).getTime();
        } catch (ParseException e) {
            mFilterToTime = -1;
        }
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
            if (logInfo.m_bMarked) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex + getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }

        for (int nIndex = 0; nIndex < nSeletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.m_bMarked) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex - getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }
    }

    int getVisibleRowCount() {
        return getVisibleRect().height / getRowHeight();
    }

    void gotoPreBookmark() {
        int nSeletectRow = getSelectedRow();
        Rectangle parent = getVisibleRect();

        LogInfo logInfo;
        for (int nIndex = nSeletectRow - 1; nIndex >= 0; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.m_bMarked) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex - getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }

        for (int nIndex = getRowCount() - 1; nIndex > nSeletectRow; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.m_bMarked) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex + getVisibleRowCount() / 2;
                showRow(nVisible);
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
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex + getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }

        for (int nIndex = 0; nIndex < nSeletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex - getVisibleRowCount() / 2;
                showRow(nVisible);
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
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex - getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }

        for (int nIndex = getRowCount() - 1; nIndex > nSeletectRow; nIndex--) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.containString(m_strSearchHighlight)) {
                changeSelection(nIndex, 0, false, false);
                int nVisible = nIndex;
                if (!isInnerRect(parent, getCellRect(nIndex, 0, true)))
                    nVisible = nIndex + getVisibleRowCount() / 2;
                showRow(nVisible);
                return;
            }
        }
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

        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = getTableHeader().getDefaultRenderer();
        }
        Component comp;
//        Component comp = renderer.getTableCellRendererComponent(
//            this, col.getHeaderValue(), false, false, 0, 0);
//        width = comp.getPreferredSize().width;

        JViewport viewport = m_LogFilterMain.m_scrollVBar.getViewport();
        Rectangle viewRect = viewport.getViewRect();
        int nFirst = m_LogFilterMain.getLogTable().rowAtPoint(new Point(0, viewRect.y));
        int nLast = m_LogFilterMain.getLogTable().rowAtPoint(new Point(0, viewRect.height - 1));

        if (nLast < 0)
            nLast = m_LogFilterMain.getLogTable().getRowCount();
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


    private TableColumnModelListener mTableColumnWidthListener = new TableColumnModelListener() {

        @Override
        public void columnMarginChanged(ChangeEvent e) {
            TableColumn tableColumn = getTableHeader().getResizingColumn();
            if (tableColumn != null) {
                int colIdx = tableColumn.getModelIndex();
                int width = tableColumn.getWidth();
                LogFilterTableModel.ColWidth[colIdx] = width;
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {

        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {

        }
    };

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

    public void setLogParser(ILogParser iLogParser) {
        m_iLogParser = iLogParser;
    }

    public void setValueAt(Object aValue, int row, int column) {
        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
        if (column == LogFilterTableModel.COMUMN_BOOKMARK) {
            logInfo.m_strBookmark = (String) aValue;
            m_LogFilterMain.setBookmark(logInfo.m_intLine - 1, (String) aValue);
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
            int weight = Utils.sift4(cmd, logInfo.m_strMessage, 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("next minWeight = " + minWeight);
            changeSelection(minIdx, 0, false, false);
            int nVisible = minIdx;
            if (!isInnerRect(parent, getCellRect(minIdx, 0, true)))
                nVisible = minIdx + getVisibleRowCount() / 2;
            showRow(nVisible);
            return;
        }

        for (int nIndex = 0; nIndex < seletectRow; nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.m_strMessage, 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("pre minWeight = " + minWeight);
            changeSelection(minIdx, 0, false, false);
            int nVisible = minIdx;
            if (!isInnerRect(parent, getCellRect(minIdx, 0, true)))
                nVisible = minIdx + getVisibleRowCount() / 2;
            showRow(nVisible);
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
            int weight = Utils.sift4(cmd, logInfo.m_strMessage, 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("pre minWeight = " + minWeight);
            changeSelection(minIdx, 0, false, false);
            int nVisible = minIdx;
            if (!isInnerRect(parent, getCellRect(minIdx, 0, true)))
                nVisible = minIdx + getVisibleRowCount() / 2;
            showRow(nVisible);
            return;
        }

        for (int nIndex = seletectRow + 1; nIndex < getRowCount(); nIndex++) {
            logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            int weight = Utils.sift4(cmd, logInfo.m_strMessage, 10);
            if (weight < minWeight) {
                minWeight = weight;
                minIdx = nIndex;
            }
        }
        if (minWeight != Integer.MAX_VALUE) {
            T.d("next minWeight = " + minWeight);
            changeSelection(minIdx, 0, false, false);
            int nVisible = minIdx;
            if (!isInnerRect(parent, getCellRect(minIdx, 0, true)))
                nVisible = minIdx + getVisibleRowCount() / 2;
            showRow(nVisible);
            return;
        }
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
            c.setForeground(logInfo.m_TextColor);
            if (isSelected) {
                if (logInfo.m_bMarked)
                    c.setBackground(new Color(LogColor.COLOR_BOOKMARK2));
            } else if (logInfo.m_bMarked)
                c.setBackground(new Color(LogColor.COLOR_BOOKMARK));
            else
                c.setBackground(Color.WHITE);

            return c;
        }

        String remakeData(int nIndex, String strText) {
//            if(nIndex != LogFilterTableModel.COMUMN_MESSAGE
//                    && nIndex != LogFilterTableModel.COMUMN_TAG)
//                return strText;

            m_bChanged = false;

            strText = strText.replace(" ", "\u00A0");
            if (LogColor.COLOR_HIGHLIGHT != null && LogColor.COLOR_HIGHLIGHT.length > 0) {
                strText = remakeFind(strText, GetHighlight(), LogColor.COLOR_HIGHLIGHT, true);
            } else {
                strText = remakeFind(strText, GetHighlight(), "#00FF00", true);
            }

            if (nIndex == LogFilterTableModel.COMUMN_MESSAGE
                    || nIndex == LogFilterTableModel.COMUMN_TAG) {
                String strFind = nIndex == LogFilterTableModel.COMUMN_MESSAGE ? GetFilterFind() : GetFilterShowTag();
                strText = remakeFind(strText, strFind, "#FF0000", false);
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

    public LogInfo getLatestSelectedLogInfo() {
        return m_latestSelectLogInfo;
    }
}

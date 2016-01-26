package com.bt.tool;

import com.bt.tool.diff.DiffService;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.BitSet;

public class LogTable extends JTable implements FocusListener, ActionListener, ILogRenderResolver {
    private static final long serialVersionUID = 1L;

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
    boolean m_bAltPressed;
    boolean[] m_arbShow;

    private LogInfo m_latestSelectLogInfo;
    private int mLastSelectedRow;
    private LogInfoHistory curHistoryInfo = new LogInfoHistory();
    private long mFilterToTime = -1;
    private long mFilterFromTime = -1;
    public int mMaxShownCol = 0;
    public int mMinShownCol = LogFilterTableModel.COMUMN_MAX - 1;
    private int mMaxSelectedRow;
    private int mMinSelectedRow;

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

    public void changeSelection(LogInfo target, boolean toggle, boolean extend) {
        for (int nIndex = 0; nIndex < getRowCount(); nIndex++) {
            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(nIndex);
            if (logInfo.getLine() != null && target.getLine() == logInfo.getLine()) {
                showRowCenterIfNotInRect(nIndex, true);
                return;
            }
        }
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

        initListener();
    }

    private void initListener() {
        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                int column = columnAtPoint(p);
                if (row < 0 || row > getRowCount()) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        logInfo.setMarked(!logInfo.isMarked());
                        m_LogFilterMain.markLogInfo(row, logInfo.getLine() - 1, logInfo.isMarked());
                    } else if (m_bAltPressed) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        if (column == LogFilterTableModel.COMUMN_TAG) {
                            if (m_strTagShow.contains("|" + logInfo.getData(column))) {
                                m_strTagShow = m_strTagShow.replace("|" + logInfo.getData(column), "");
                            } else if (m_strTagShow.contains((String) logInfo.getData(column))) {
                                m_strTagShow = m_strTagShow.replace((String) logInfo.getData(column), "");
                            } else {
                                m_strTagShow += "|" + logInfo.getData(column);
                            }
                            m_LogFilterMain.notiEvent(new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_SHOW_TAG));
                        } else if (column == LogFilterTableModel.COMUMN_TIME) {
                            m_LogFilterMain.notiEvent(
                                    new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_FROM_TIME, logInfo.getTime())
                            );
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {

                    boolean hasSelected = false;
                    for (int sRow : getSelectedRows()) {
                        if (sRow == row) {
                            hasSelected = true;
                            break;
                        }
                    }
                    if (!hasSelected) {
                        setRowSelectionInterval(row, row);
                        setColumnSelectionInterval(column, column);
                    }

                    T.d("m_bAltPressed = " + m_bAltPressed);
                    if (m_bAltPressed) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        if (column == LogFilterTableModel.COMUMN_TAG) {
                            m_strTagRemove += "|" + logInfo.getData(column);
                            m_LogFilterMain.notiEvent(new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG));
                        } else if (column == LogFilterTableModel.COMUMN_TIME) {
                            m_LogFilterMain.notiEvent(
                                    new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_TO_TIME, logInfo.getTime())
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
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                mMaxSelectedRow = lsm.getMaxSelectionIndex();
                mMinSelectedRow = lsm.getMinSelectionIndex();
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
                    logInfo.setMarked(!logInfo.isMarked());
                    m_LogFilterMain.markLogInfo(selectedRow, logInfo.getLine() - 1, logInfo.isMarked());
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

    int getVisibleRowCount() {
        return getVisibleRect().height / getRowHeight();
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

        JViewport viewport = m_LogFilterMain.m_logScrollVPane.getViewport();
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

    public int getColumnWidth(int nColumn) {
        return getColumnModel().getColumn(nColumn).getWidth();
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

    public void setValueAt(Object aValue, int row, int column) {
        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
        if (column == LogFilterTableModel.COMUMN_BOOKMARK) {
            logInfo.setBookmark((String) aValue);
            m_LogFilterMain.setBookmark(logInfo.getLine() - 1, (String) aValue);
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

    public LogInfo getLatestSelectedLogInfo() {
        return m_latestSelectLogInfo;
    }
}

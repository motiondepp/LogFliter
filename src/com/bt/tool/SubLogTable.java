package com.bt.tool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SubLogTable extends BaseLogTable {
    private static final long serialVersionUID = 1L;

    public SubLogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel, filterMain);
        initListener();
    }

    private void initListener() {
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

        JMenuItem copycolumnToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selColumns = getSelectedColumns();
                if (selColumns.length != 0) {
                    copySelectedColumn(selColumns);
                }
            }
        });
        menuPopup.add(copycolumnToClipboard);
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy row to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });
        menuPopup.add(copyRowToClipboard);

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
}

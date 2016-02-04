package com.bt.tool;

import com.bt.tool.diff.DiffService;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LogTable extends BaseLogTable {
    private static final long serialVersionUID = 1L;
    private DiffService mDiffService;

    public LogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel, filterMain);
        initListener();
    }

    protected void initListener() {
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
                        if (column != LogFilterTableModel.COMUMN_BOOKMARK) {
                            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                            logInfo.setMarked(!logInfo.isMarked());
                            mBaseLogTableListener.markLogInfo(row, logInfo.getLine() - 1, logInfo.isMarked());
                        }
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
                            mBaseLogTableListener.notiEvent(new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_SHOW_TAG));
                        } else if (column == LogFilterTableModel.COMUMN_TIME) {
                            mBaseLogTableListener.notiEvent(
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
                            mBaseLogTableListener.notiEvent(new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG));
                        } else if (column == LogFilterTableModel.COMUMN_TIME) {
                            mBaseLogTableListener.notiEvent(
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

    private JPopupMenu createRightClickPopUp() {

        JPopupMenu menuPopup = new JPopupMenu();
        JMenuItem copycolumnToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selColumns = getSelectedColumns();
                if (selColumns.length != 0) {
                    copySelectedColumn(selColumns);
                }
            }
        });
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy row to clipboard") {
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
                    mBaseLogTableListener.markLogInfo(selectedRow, logInfo.getLine() - 1, logInfo.isMarked());
                }
            }
        });
        JMenuItem findInDiffMenuItem = new JMenuItem(new AbstractAction("find in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getData(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.FIND,
                                target
                        );
                    }
                }
            }
        });

        JMenuItem findSimilarInDiffMenuItem = new JMenuItem(new AbstractAction("find similar in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getData(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.FIND_SIMILAR,
                                target
                        );
                    }
                }
            }
        });

        JMenuItem compareMenuItem = new JMenuItem(new AbstractAction("compare with selected in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String target = getFormatSelectedRows(new int[]{LogFilterTableModel.COMUMN_LINE, LogFilterTableModel.COMUMN_DATE});
                if (target != null && target.length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.COMPARE,
                                target
                        );
                    }
                }
            }
        });

        menuPopup.add(markMenuItem);
        menuPopup.add(copyRowToClipboard);
        menuPopup.add(copycolumnToClipboard);
        if (mDiffService != null && mDiffService.isDiffConnected()) {
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

    public DiffService getDiffService() {
        return mDiffService;
    }

    public void setDiffService(DiffService mDiffService) {
        this.mDiffService = mDiffService;
    }
}

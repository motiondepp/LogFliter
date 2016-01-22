package com.bt.tool;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * Created by xinyu.he on 2016/1/22.
 */

public class LogCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private Border SELECTED_BORDER_TOP_LEFT;
    private Border SELECTED_BORDER_TOP_RIGHT;
    private Border SELECTED_BORDER_TOP;
    private Border SELECTED_BORDER_BOTTOM_LEFT;
    private Border SELECTED_BORDER_BOTTOM_RIGHT;
    private Border SELECTED_BORDER_BOTTOM;
    private Border SELECTED_BORDER_LEFT;
    private Border SELECTED_BORDER_RIGHT;
    private Border SELECTED_BORDER_TOTAL;
    private Border SELECTED_BORDER_LEFT_RIGHT;
    private Border SELECTED_BORDER_LEFT_RIGHT_TOP;
    private Border SELECTED_BORDER_LEFT_RIGHT_BOTTOM;
    private Border SELECTED_BORDER_TOP_BOTTOM;
    private Border SELECTED_BORDER_TOP_BOTTOM_LEFT;
    private Border SELECTED_BORDER_TOP_BOTTOM_RIGHT;
    private Border SELECTED_BORDER_NONE;

    boolean m_bChanged;
    private final int BRORDER_WIDTG = 1;
    private final Color BORDER_COLOR = new Color(100, 100, 100);
    private JTable mTable;
    private ILogRenderResolver mResolver;

    public LogCellRenderer(JTable table, ILogRenderResolver resolver) {
        super();
        SELECTED_BORDER_TOP = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(BRORDER_WIDTG, 0, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_BOTTOM = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, BRORDER_WIDTG, 0, BORDER_COLOR));
        SELECTED_BORDER_LEFT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, BRORDER_WIDTG, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_RIGHT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, 0, BRORDER_WIDTG, BORDER_COLOR));

        SELECTED_BORDER_TOP_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_TOP_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_RIGHT);
        SELECTED_BORDER_BOTTOM_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_BOTTOM, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_BOTTOM_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_BOTTOM, SELECTED_BORDER_RIGHT);

        SELECTED_BORDER_LEFT_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT, SELECTED_BORDER_RIGHT);
        SELECTED_BORDER_LEFT_RIGHT_TOP = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT_RIGHT, SELECTED_BORDER_TOP);
        SELECTED_BORDER_LEFT_RIGHT_BOTTOM = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT_RIGHT, SELECTED_BORDER_BOTTOM);

        SELECTED_BORDER_TOP_BOTTOM = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_BOTTOM);
        SELECTED_BORDER_TOP_BOTTOM_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_BOTTOM, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_TOP_BOTTOM_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_BOTTOM, SELECTED_BORDER_RIGHT);

        SELECTED_BORDER_TOTAL = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_LEFT, SELECTED_BORDER_BOTTOM_RIGHT);
        SELECTED_BORDER_NONE = null;

        this.mTable = table;
        this.mResolver = resolver;
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        if (!mResolver.isColumnShown(column)) {
            return null;
        }
        LogInfo logInfo = ((LogFilterTableModel) mTable.getModel()).getRow(row);
        if (value != null) {
            value = remakeData(column, value.toString());
        }
        Component c = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);
        c.setForeground(logInfo.getTextColor());
        c.setFont(getFont().deriveFont(mResolver.getFontSize()));
        if (isSelected) {
            c.setFont(getFont().deriveFont(mResolver.getFontSize() + 1));
            if (logInfo.isMarked()) {
                c.setBackground(new Color(LogColor.COLOR_BOOKMARK2));
            }
        } else if (logInfo.isMarked()) {
            c.setBackground(new Color(LogColor.COLOR_BOOKMARK));
        } else {
            c.setBackground(Color.WHITE);
        }

        if (c instanceof JComponent) {
            JComponent cc = (JComponent) c;
            if (isSelected) {
                int[] rows = mTable.getSelectedRows();
                if (rows.length == 1) {
                    if (mResolver.getMinShownColumn() == column) {
                        cc.setBorder(SELECTED_BORDER_TOP_BOTTOM_LEFT);
                    } else if (mResolver.getMaxShownColumn() == column) {
                        cc.setBorder(SELECTED_BORDER_TOP_BOTTOM_RIGHT);
                    } else {
                        cc.setBorder(SELECTED_BORDER_TOP_BOTTOM);
                    }
                } else {
                    if (rows[0] == row) {
                        if (mResolver.getMinShownColumn() == column) {
                            cc.setBorder(SELECTED_BORDER_TOP_LEFT);
                        } else if (mResolver.getMaxShownColumn() == column) {
                            cc.setBorder(SELECTED_BORDER_TOP_RIGHT);
                        } else {
                            cc.setBorder(SELECTED_BORDER_TOP);
                        }
                    } else if (rows[rows.length - 1] == row) {
                        if (mResolver.getMinShownColumn() == column) {
                            cc.setBorder(SELECTED_BORDER_BOTTOM_LEFT);
                        } else if (mResolver.getMaxShownColumn() == column) {
                            cc.setBorder(SELECTED_BORDER_BOTTOM_RIGHT);
                        } else {
                            cc.setBorder(SELECTED_BORDER_BOTTOM);
                        }
                    } else if (mResolver.getMinShownColumn() == column) {
                        cc.setBorder(SELECTED_BORDER_LEFT);
                    } else if (mResolver.getMaxShownColumn() == column) {
                        cc.setBorder(SELECTED_BORDER_RIGHT);
                    } else {
                        cc.setBorder(SELECTED_BORDER_NONE);
                    }
                }
            } else {
                cc.setBorder(SELECTED_BORDER_NONE);
            }
        }

        return c;
    }

    String remakeData(int nIndex, String strText) {

        m_bChanged = false;

        strText = strText.replace(" ", "\u00A0");
        if (LogColor.COLOR_HIGHLIGHT != null && LogColor.COLOR_HIGHLIGHT.length > 0) {
            strText = remakeFind(strText, mResolver.GetHighlight(), LogColor.COLOR_HIGHLIGHT, true);
        } else {
            strText = remakeFind(strText, mResolver.GetHighlight(), "#00FF00", true);
        }

        if (nIndex == LogFilterTableModel.COMUMN_MESSAGE
                || nIndex == LogFilterTableModel.COMUMN_TAG) {
            String strFind = nIndex == LogFilterTableModel.COMUMN_MESSAGE ? mResolver.GetFilterFind() : mResolver.GetFilterShowTag();
            strText = remakeFind(strText, strFind, "#FF0000", false);
        }

        strText = remakeFind(strText, mResolver.GetSearchHighlight(), "#FFFF00", true);
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

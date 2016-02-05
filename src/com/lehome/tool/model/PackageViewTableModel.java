package com.lehome.tool.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class PackageViewTableModel extends AbstractTableModel {
    private String[] columnNames = {"pid", "package name"};
    private ArrayList<PackageInfo> data = new ArrayList<>();

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        if (row > data.size())
            return null;

        PackageInfo packageInfo = data.get(row);
        if (packageInfo != null) {
            return packageInfo.getValue(col);
        }
        return null;
    }

    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public ArrayList<PackageInfo> getData() {
        return data;
    }

    public void setData(ArrayList<PackageInfo> data) {
        this.data = data;
    }

    public static class PackageInfo {
        public String pid;
        public String name;

        public String getValue(int idx) {
            switch (idx) {
                case 0:
                    return pid;
                case 1:
                    return name;
            }
            return null;
        }
    }
}

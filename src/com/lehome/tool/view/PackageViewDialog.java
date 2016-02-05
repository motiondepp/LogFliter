package com.lehome.tool.view;

import com.lehome.tool.T;
import com.lehome.tool.model.PackageViewTableModel;
import com.lehome.tool.presenter.PackageViewPresenter;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class PackageViewDialog extends JDialog {
    private final PackageViewDialogListener mListener;
    private PackageViewPresenter mPackageViewPresenter;
    private PackageViewTableModel mPackageViewTableModel;
    private JTable mMainTable;
    private JButton mRefreshButton;
    private JProgressBar mProgressBar;

    public PackageViewDialog(JFrame frame, String title, String deviceId, PackageViewDialogListener listener) {
        super(frame, title, true);

        mPackageViewPresenter = new PackageViewPresenter(this, deviceId);
        this.mListener = listener;

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        initMainTable(c);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        mProgressBar = new JProgressBar();
        mProgressBar.setVisible(false);
        buttonPanel.add(mProgressBar);

        mRefreshButton = new JButton("Refresh");
        mRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mPackageViewPresenter.onRefreshButtonPressed();
            }
        });
        buttonPanel.add(mRefreshButton);
        c.add(buttonPanel, BorderLayout.SOUTH);

        this.pack();

        mPackageViewPresenter.onStart();
    }

    private void initMainTable(Container c) {
        mPackageViewTableModel = new PackageViewTableModel();
        mMainTable = new JTable(mPackageViewTableModel);
        mMainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mMainTable.getTableHeader().setReorderingAllowed(false);
        mMainTable.setOpaque(false);
        mMainTable.setAutoscrolls(false);
//        mMainTable.setIntercellSpacing(new Dimension(0, 0));

        mMainTable.getColumnModel().getColumn(0).setResizable(true);
        mMainTable.getColumnModel().getColumn(0).setMinWidth(80);
        mMainTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        mMainTable.getColumnModel().getColumn(1).setResizable(true);
        mMainTable.getColumnModel().getColumn(1).setMinWidth(60);
        mMainTable.getColumnModel().getColumn(1).setPreferredWidth(320);

        mMainTable.getTableHeader().addMouseListener(new ColumnHeaderListener());
        mMainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = mMainTable.rowAtPoint(p);
                int column = mMainTable.columnAtPoint(p);
                if (row < 0 || row > mMainTable.getRowCount()) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        String value = (String) mMainTable.getModel().getValueAt(row, 0);
                        if (mListener != null) {
                            mListener.onFliterPidSelected(value);
                        }
                    }
                }
            }
        });


        JScrollPane scrollPane = new JScrollPane(mMainTable);
        c.add(scrollPane, BorderLayout.CENTER);
    }

    public JTable getMainTable() {
        return mMainTable;
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
                mPackageViewPresenter.onColumnHeaderClicked(vColIndex);
            }
        }
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

    public interface PackageViewDialogListener {

        void onFliterPidSelected(String value);
    }
}
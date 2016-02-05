package com.lehome.tool.presenter;

import com.lehome.tool.OSUtil;
import com.lehome.tool.T;
import com.lehome.tool.model.PackageViewTableModel;
import com.lehome.tool.view.PackageViewDialog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Comparator;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class PackageViewPresenter {
    private final PackageViewDialog mPackageViewDialog;
    private final String mDeviceId;

    public PackageViewPresenter(PackageViewDialog packageViewDialog, String deviceID) {
        this.mPackageViewDialog = packageViewDialog;
        this.mDeviceId = deviceID;
    }

    public void onStart() {
        refreshPackageData();
    }

    public void onRefreshButtonPressed() {
        refreshPackageData();
    }

    private void refreshPackageData() {
        new Thread() {
            @Override
            public void run() {
                mPackageViewDialog.onStartLoadingPackages();

                PackageViewTableModel model = (PackageViewTableModel) mPackageViewDialog.getMainTable().getModel();
                model.getData().clear();
                model.fireTableDataChanged();

                T.d("getting zygoteID");
                String[] getPPIDCmd = getADBValidCmd("ps | grep zygote");
                String PPIDResult = processCmd(getPPIDCmd);
                if (PPIDResult != null) {
                    try {
                        String zygoteID = PPIDResult.split("\\s+")[1];
                        T.d("got zygoteID: " + zygoteID);
                        String[] getPackageCmd = getADBValidCmd("ps | grep " + "\\\" " + zygoteID + " \\\"");
                        String packageResult = processCmd(getPackageCmd);
                        if (packageResult != null) {
                            for (String item : packageResult.split("\n")) {
                                T.d("got package: " + item);
                                String[] infos = item.split("\\s+");
                                PackageViewTableModel.PackageInfo newInfo = new PackageViewTableModel.PackageInfo();
                                newInfo.pid = infos[1];
                                newInfo.name = infos[8];
                                model.getData().add(newInfo);
                            }
                        }
                        model.fireTableDataChanged();
                    } catch (Exception e) {
                        T.d(e);
                    }
                }

                mPackageViewDialog.onStopLoadingPackages();
            }
        }.start();
    }

    private String[] getADBValidCmd(String customCmd) {
        if (mDeviceId != null && mDeviceId.length() != 0) {
            customCmd = "adb -s " + mDeviceId + " shell \"" + customCmd + "\"";
        }else {
            customCmd = "adb shell \"" + customCmd + "\"";
        }
        String[] cmd;
        if (OSUtil.isWindows()) {
            cmd = new String[]{"cmd.exe", "/C", customCmd};
        } else {
            cmd = new String[]{"/bin/bash", "-l", "-c", customCmd};
        }
        return cmd;
    }

    private String processCmd(String[] cmd) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            Process oProcess = processBuilder.start();

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                    oProcess.getInputStream()));

            String s;
            StringBuffer sb = new StringBuffer();
            while ((s = stdOut.readLine()) != null) {
                if (s.trim().length() != 0) {
                    sb.append(s.trim());
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            T.e("e = " + e);
            return null;
        }
    }

    public void onColumnHeaderClicked(final int vColIndex) {
        PackageViewTableModel model = (PackageViewTableModel) this.mPackageViewDialog.getMainTable().getModel();
        boolean ascend = true;
        if (model.getData().size() > 1) {
            if (model.getData().get(0).getValue(vColIndex).compareTo(
                            model.getData().get(model.getData().size() - 1).getValue(vColIndex)
                    ) < 0 ) {
                ascend = false;
            }
        }

        final boolean finalAscend = ascend;
        model.getData().sort(new Comparator<PackageViewTableModel.PackageInfo>() {
            @Override
            public int compare(PackageViewTableModel.PackageInfo o1, PackageViewTableModel.PackageInfo o2) {
                if (finalAscend)
                    return o1.getValue(vColIndex).compareTo(o2.getValue(vColIndex));
                else
                    return o2.getValue(vColIndex).compareTo(o1.getValue(vColIndex));
            }
        });
        model.fireTableDataChanged();
    }
}

package com.lehome.tool.diff;

import com.lehome.tool.LogFilterMain;
import com.lehome.tool.T;
import com.lehome.tool.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by xinyu.he on 2016/1/12.
 */
public class DiffService {

    private DiffServer mDiffServer;
    private DiffClient mDiffClient;
    private LogFilterMain mLogFilterMain;

    private DiffServiceType mDiffServiceType;
    private boolean mIsDiffConnected;

    public DiffService(LogFilterMain mainPanel, int serverPort) {
        mLogFilterMain = mainPanel;
        setupDiffServer(serverPort);
    }

    DiffClient.DiffClientListener mDiffClientListener = new DiffClient.DiffClientListener() {
        @Override
        public String onReceiveString(String input) {
            return handleReceiveDiffCmd(input);
        }

        @Override
        public void onConnected() {
            mDiffServiceType = DiffServiceType.AS_CLIENT;
            mIsDiffConnected = true;

            mLogFilterMain.refreshUIWithDiffState();
            mLogFilterMain.refreshDiffMenuBar();
        }

        @Override
        public void onDisconnected() {
            mDiffServiceType = null;
            mIsDiffConnected = false;

            mLogFilterMain.refreshUIWithDiffState();
            mLogFilterMain.refreshDiffMenuBar();
        }
    };


    DiffServer.DiffServerListener mDiffServerListener = new DiffServer.DiffServerListener() {
        @Override
        public String onReceiveString(String input) {
            return handleReceiveDiffCmd(input);
        }

        @Override
        public void onClientConnected(Socket clientSocket) {
            mDiffServiceType = DiffServiceType.AS_SERVER;
            mIsDiffConnected = true;
            mLogFilterMain.refreshUIWithDiffState();
        }

        @Override
        public void onClientDisconnected(Socket clientSocket) {
            mDiffServiceType = null;
            mIsDiffConnected = false;
            mLogFilterMain.refreshUIWithDiffState();
        }
    };

    public boolean setupDiffClient(String serverPort) {
        try {
            mDiffClient = new DiffClient(Integer.valueOf(serverPort), mDiffClientListener);
            mDiffClient.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void disconnectDiffClient() {
        mDiffClient.cleanup();
    }

    private void setupDiffServer(int port) {
        try {
            mDiffServer = new DiffServer(port, mDiffServerListener);
            T.d("bind port:" + port);
        } catch (IOException e) {
            e.printStackTrace();

            mDiffServer.cleanup();
            mDiffServer = null;
        }
        if (mDiffServer != null)
            mDiffServer.start();
    }

    public boolean isDiffConnected() {
        return mIsDiffConnected;
    }

    private String handleReceiveDiffCmd(String input) {
        JSONObject responseJson = new JSONObject(input);
        String type = responseJson.getString("type");
        String cmd = responseJson.getString("cmd");
        T.d("receive type:" + type + " cmd:" + cmd);
        if (type.equals(DiffServiceCmdType.SYNC_SCROLL_V.toString())) {
            mLogFilterMain.handleScrollVSyncEvent(cmd);
        } else if (type.equals(DiffServiceCmdType.FIND.toString())) {
            mLogFilterMain.searchKeyword(cmd);
        } else if (type.equals(DiffServiceCmdType.FIND_SIMILAR.toString())) {
            mLogFilterMain.searchSimilar(cmd);
        } else if (type.equals(DiffServiceCmdType.COMPARE.toString())) {
            mLogFilterMain.compareWithSelectedRows(cmd);
        } else if (type.equals(DiffServiceCmdType.SYNC_SELECTED_FORWARD.toString())) {
            mLogFilterMain.handleSelectedForwardSyncEvent(cmd);
        } else if (type.equals(DiffServiceCmdType.SYNC_SELECTED_BACKWARD.toString())) {
            mLogFilterMain.handleSelectedBackwardSyncEvent(cmd);
        }
        return null;
    }

    public void writeDiffCommand(DiffServiceCmdType cmdType, String cmd) {
        if (!mIsDiffConnected) {
            T.d("service disconnected.");
            return;
        }
        JSONObject jsonReq = new JSONObject();
        jsonReq.put("type", cmdType);
        jsonReq.put("cmd", cmd);
        String req = jsonReq.toString();
        T.d("send req to target: " + req);

        switch (getDiffServiceType()) {
            case AS_CLIENT:
                mDiffClient.writeStringToServer(req);
                break;
            case AS_SERVER:
                mDiffServer.writeStringToClient(req);
                break;
            default:
                break;
        }
    }

    public DiffServiceType getDiffServiceType() {
        return mDiffServiceType;
    }

    public enum DiffServiceType {
        AS_CLIENT, AS_SERVER
    }

    public enum DiffServiceCmdType {
        FIND, FIND_SIMILAR, COMPARE, SYNC_SELECTED_FORWARD, SYNC_SELECTED_BACKWARD, SYNC_SCROLL_V
    }
}

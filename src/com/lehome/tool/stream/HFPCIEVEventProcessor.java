package com.lehome.tool.stream;

import com.lehome.tool.LogInfo;

/**
 * Created by xinyu.he on 2016/1/22.
 */
public class HFPCIEVEventProcessor extends MessagePostProcessor {

    private static final String[][] DESC_MAP = {
            {},
            {"no calls in progress", "at least one call is in progress"},
            {"not currently in call set up", "an incoming call process ongoing", "an outgoing call set up is ongoing", "remote party being alerted in an outgoing call"},
            {"No calls held", "Call is placed on hold or active/held calls swapped (The AG has both an active AND a held call)", "Call on hold, no active call"}
    };

    @Override
    protected LogInfo process(LogInfo srcInfo) {
        String result = srcInfo.getMessage();
        if (result.startsWith("SEND AT CMD : +CIEV")) {
            String inds = result.substring(result.length() - 3);
            int cievType = (int) inds.charAt(0) - '0';
            int cievMsg = (int) inds.charAt(2) - '0';
            result += " (" + DESC_MAP[cievType][cievMsg] + ")";
        }
        srcInfo.setMessage(result);
        return srcInfo;
    }

    @Override
    public boolean shouldProcess(LogInfo info) {
        return info != null && info.getTag().equals("bt-btif:");
    }
}

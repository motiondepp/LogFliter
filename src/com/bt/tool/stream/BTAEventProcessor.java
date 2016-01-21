package com.bt.tool.stream;

import com.bt.tool.LogInfo;
import com.bt.tool.T;
import com.bt.tool.Utils;
import com.bt.tool.json.JSONArray;
import com.bt.tool.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xinyu.he on 2016/1/6.
 */
public class BTAEventProcessor extends MessagePostProcessor {

    String[] mSubSysList = null;
    Map<String, String[]> mEventMap = new HashMap<>();

    public BTAEventProcessor(String confPath) {
        super(true);

        String confContent = "";
        try {
            confContent = Utils.readFileAsString(confPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        initJson(confContent);
    }

    private void initJson(String confContent) {
        JSONObject jsonObject = new JSONObject(confContent);
        JSONArray subSyss = jsonObject.getJSONArray("sub_sys_list");
        mSubSysList = new String[subSyss.length()];
        for (int i = 0; i < mSubSysList.length; i++) {
            mSubSysList[i] = subSyss.get(i).toString();
        }

        JSONObject eventMap = jsonObject.getJSONObject("event_map");
        for (String subSys : mSubSysList) {
            JSONArray events = eventMap.getJSONArray(subSys);
            if (events != null) {
                String[] eventArray = new String[events.length()];
                for (int i = 0; i < eventArray.length; i++) {
                    eventArray[i] = events.get(i).toString();
                }
                mEventMap.put(subSys, eventArray);
            }
        }

        T.d("init finish: mSubSysList " + mSubSysList.length + " | mEventMap " + mEventMap.size());
    }

    @Override
    protected LogInfo process(LogInfo srcInfo) {
        String src = srcInfo.getMessage();
        if (mSubSysList != null && src.startsWith("BTA got event ")) {
            try {
                String eventName = "known";
                String eventSrc = src.substring("BTA got event ".length() + 2);
                // T.d("process event code: " + src);

                eventSrc = ("0000" + eventSrc).substring(eventSrc.length());
                byte[] eventByte = DatatypeConverter.parseHexBinary(eventSrc);

                int sysCode = eventByte[0];
                if (sysCode <= mSubSysList.length - 1) {
                    String sysName = mSubSysList[sysCode];

                    int eventCode = eventByte[1];
                    if (eventCode <= mEventMap.get(sysName).length - 1)
                        eventName = mEventMap.get(sysName)[eventCode];
                }
                src = src + " (" + eventName + ")";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        srcInfo.setMessage(src);
        return srcInfo;
    }

    @Override
    public boolean shouldProcess(LogInfo info) {
        return info != null && info.getTag().equals("bt-btif:");
    }
}

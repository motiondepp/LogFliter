package com.bt.tool.stream;

import com.bt.tool.LogInfo;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xinyu.he on 2016/1/22.
 */
public class HeadsetStateProcessor extends MessagePostProcessor {

    public static final int STATE_AUDIO_DISCONNECTED = 10;
    public static final int STATE_AUDIO_CONNECTING = 11;
    public static final int STATE_AUDIO_CONNECTED = 12;

    static final int CONNECT = 1;
    static final int DISCONNECT = 2;
    static final int CONNECT_AUDIO = 3;
    static final int DISCONNECT_AUDIO = 4;
    static final int VOICE_RECOGNITION_START = 5;
    static final int VOICE_RECOGNITION_STOP = 6;
    static final int INTENT_SCO_VOLUME_CHANGED = 7;
    static final int SET_MIC_VOLUME = 8;
    static final int CALL_STATE_CHANGED = 9;
    static final int INTENT_BATTERY_CHANGED = 10;
    static final int DEVICE_STATE_CHANGED = 11;
    static final int SEND_CLCC_RESPONSE = 12;
    static final int SEND_VENDOR_SPECIFIC_RESULT_CODE = 13;
    static final int VIRTUAL_CALL_START = 14;
    static final int VIRTUAL_CALL_STOP = 15;
    static final int ENABLE_WBS = 16;
    static final int DISABLE_WBS = 17;
    static final int DEVICE_DATA_STATE_CHANGED = 18;
    static final int ROAM_CHANGED = 21;
    static final int SET_SCO_PATH = 22;
    static final int SEND_VENDOR_SPECIFIC_COMMAND = 50; // vender specific command
    static final int SEND_MESSAGE_TO_HEADSET = 60;      // vender specific command
    static final int REGISTER_MESSAGE_LISTENER = 70;    // vender specific command
    static final int UNREGISTER_MESSAGE_LISTENER = 80;  // vender specific command
    static final int LIMIT_ATCMD = 90;                  // for wearable device
    private static final int STACK_EVENT = 101;
    private static final int DIALING_OUT_TIMEOUT = 102;
    private static final int START_VR_TIMEOUT = 103;
    private static final int CLCC_RSP_TIMEOUT = 104;
    private static final int CONNECT_AUDIO_TIMEOUT = 105;
    private static final int MSG_DISCONNECT_HFP_DELAYED = 106;
    private static final int CONNECT_TIMEOUT = 201;
    private static final int NEXT_CONNECT_TIMEOUT = 202;
    private static final int DELAY_ACTION = 203;


    public static final HashMap<Integer, String> STATE_MAP = new HashMap<>();

    static {
        STATE_MAP.put(STATE_AUDIO_DISCONNECTED, "STATE_AUDIO_DISCONNECTED");
        STATE_MAP.put(STATE_AUDIO_CONNECTING, "STATE_AUDIO_CONNECTING");
        STATE_MAP.put(STATE_AUDIO_CONNECTED, "STATE_AUDIO_CONNECTED");
    }

    public static final HashMap<Integer, String> MSG_MAP = new HashMap<>();

    static {
        MSG_MAP.put(CONNECT, "CONNECT");
        MSG_MAP.put(DISCONNECT, "DISCONNECT");
        MSG_MAP.put(CONNECT_AUDIO, "CONNECT_AUDIO");
        MSG_MAP.put(DISCONNECT_AUDIO, "DISCONNECT_AUDIO");
        MSG_MAP.put(VOICE_RECOGNITION_START, "VOICE_RECOGNITION_START");
        MSG_MAP.put(VOICE_RECOGNITION_STOP, "VOICE_RECOGNITION_STOP");
        MSG_MAP.put(INTENT_SCO_VOLUME_CHANGED, "INTENT_SCO_VOLUME_CHANGED");
        MSG_MAP.put(SET_MIC_VOLUME, "SET_MIC_VOLUME");
        MSG_MAP.put(CALL_STATE_CHANGED, "CALL_STATE_CHANGED");
        MSG_MAP.put(INTENT_BATTERY_CHANGED, "INTENT_BATTERY_CHANGED");
        MSG_MAP.put(DEVICE_STATE_CHANGED, "DEVICE_STATE_CHANGED");
        MSG_MAP.put(SEND_CLCC_RESPONSE, "SEND_CLCC_RESPONSE");
        MSG_MAP.put(SEND_VENDOR_SPECIFIC_RESULT_CODE, "SEND_VENDOR_SPECIFIC_RESULT_CODE");
        MSG_MAP.put(VIRTUAL_CALL_START, "VIRTUAL_CALL_START");
        MSG_MAP.put(VIRTUAL_CALL_STOP, "VIRTUAL_CALL_STOP");
        MSG_MAP.put(ENABLE_WBS, "ENABLE_WBS");
        MSG_MAP.put(DISABLE_WBS, "DISABLE_WBS");
        MSG_MAP.put(DEVICE_DATA_STATE_CHANGED, "DEVICE_DATA_STATE_CHANGED");
        MSG_MAP.put(ROAM_CHANGED, "ROAM_CHANGED");
        MSG_MAP.put(SET_SCO_PATH, "SET_SCO_PATH");
        MSG_MAP.put(SEND_VENDOR_SPECIFIC_COMMAND, "SEND_VENDOR_SPECIFIC_COMMAND");
        MSG_MAP.put(SEND_MESSAGE_TO_HEADSET, "SEND_MESSAGE_TO_HEADSET");
        MSG_MAP.put(REGISTER_MESSAGE_LISTENER, "REGISTER_MESSAGE_LISTENER");
        MSG_MAP.put(UNREGISTER_MESSAGE_LISTENER, "UNREGISTER_MESSAGE_LISTENER");
        MSG_MAP.put(LIMIT_ATCMD, "LIMIT_ATCMD");
        MSG_MAP.put(STACK_EVENT, "STACK_EVENT");
        MSG_MAP.put(DIALING_OUT_TIMEOUT, "DIALING_OUT_TIMEOUT");
        MSG_MAP.put(START_VR_TIMEOUT, "START_VR_TIMEOUT");
        MSG_MAP.put(CLCC_RSP_TIMEOUT, "CLCC_RSP_TIMEOUT");
        MSG_MAP.put(CONNECT_AUDIO_TIMEOUT, "CONNECT_AUDIO_TIMEOUT");
    }

    public static final String STATE_SEP_STR = " -> ";

    private static final Pattern PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\w+ process message: ");

    @Override
    protected LogInfo process(LogInfo src) {
        String result = src.getMessage();
        if (result.startsWith("Audio state ")) {
            Matcher matcher = PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                Integer state = Integer.valueOf(matcher.group(1));
                if (STATE_MAP.containsKey(state)) {
                    sb.append(STATE_MAP.get(state));
                    sb.append(STATE_SEP_STR);
                }
            }
            sb.insert(0, " ( ").delete(sb.length() - STATE_SEP_STR.length(), sb.length()).append(" )");
            result += sb.toString();
        } else if (MESSAGE_PATTERN.matcher(result).find()) {
            int idx = result.indexOf(": ");
            if (idx != -1) {
                Matcher matcher = PATTERN.matcher(result);
                if (matcher.find()) {
                    String msgValue = matcher.group(1);
                    int msgIdx = Integer.valueOf(msgValue);
                    if (MSG_MAP.containsKey(msgIdx)) {
                        StringBuffer sb = new StringBuffer();
                        sb.insert(0, " ( ").append(MSG_MAP.get(msgIdx)).append(" )");
                        result += sb.toString();
                    }
                }
            }
        }
        src.setMessage(result);
        return src;
    }

    @Override
    public boolean shouldProcess(LogInfo info) {
        return info != null && info.getTag().equals("HeadsetStateMachine:");
    }
}

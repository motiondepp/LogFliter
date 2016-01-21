package com.bt.tool.stream;

import com.bt.tool.LogInfo;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xinyu.he on 2016/1/20.
 */
public class BluetoothAdapterStateProcessor extends MessagePostProcessor {

    public static final int STATE_OFF = 10;
    public static final int STATE_TURNING_ON = 11;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;
    public static final int STATE_RADIO_ON = 14;
    public static final int STATE_RADIO_OFF = 15;
    public static final HashMap<Integer, String> STATE_MAP = new HashMap<>();

    static {
        STATE_MAP.put(STATE_OFF, "STATE_OFF");
        STATE_MAP.put(STATE_TURNING_ON, "STATE_TURNING_ON");
        STATE_MAP.put(STATE_ON, "STATE_ON");
        STATE_MAP.put(STATE_TURNING_OFF, "STATE_TURNING_OFF");
        STATE_MAP.put(STATE_RADIO_ON, "STATE_RADIO_ON");
        STATE_MAP.put(STATE_RADIO_OFF, "STATE_RADIO_OFF");
    }

    public static final String STATE_SEP_STR = " -> ";

    private static final Pattern PATTERN = Pattern.compile("(\\d+)");

    @Override
    protected LogInfo process(LogInfo src) {
        String result = src.getMessage();
        if (result.startsWith("Bluetooth adapter state changed")) {
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
        }
        src.setMessage(result);
        return src;
    }

    @Override
    public boolean shouldProcess(LogInfo info) {
        return info != null && info.getTag().equals("BluetoothAdapterState:");
    }
}

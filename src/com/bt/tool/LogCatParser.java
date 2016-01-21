package com.bt.tool;

import com.bt.tool.stream.BTAEventProcessor;
import com.bt.tool.stream.BluetoothAdapterStateProcessor;
import com.bt.tool.stream.MessagePostProcessor;

import java.awt.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

public class LogCatParser implements ILogParser {
    final String TOKEN_KERNEL = "<>[]";
    final String TOKEN_SPACE = " ";
    final String TOKEN_SLASH = "/";
    final String TOKEN = "/()";
    final String TOKEN_PID = "/() ";
    final String TOKEN_MESSAGE = "'";

    public static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public MessagePostProcessor mMessagePostProcessor;

    public LogCatParser() {
        mMessagePostProcessor = new BTAEventProcessor("BTA_event_conf.json");
        mMessagePostProcessor.setNextProcessor(new BluetoothAdapterStateProcessor());
    }

    public Color getColor(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return Color.BLACK;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return new Color(LogColor.COLOR_FATAL);
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E") || logInfo.getLogLV().equals("3"))
            return new Color(LogColor.COLOR_ERROR);
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W") || logInfo.getLogLV().equals("4"))
            return new Color(LogColor.COLOR_WARN);
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I") || logInfo.getLogLV().equals("6"))
            return new Color(LogColor.COLOR_INFO);
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D") || logInfo.getLogLV().equals("7"))
            return new Color(LogColor.COLOR_DEBUG);
        else if (logInfo.getLogLV().equals("0"))
            return new Color(LogColor.COLOR_0);
        else if (logInfo.getLogLV().equals("1"))
            return new Color(LogColor.COLOR_1);
        else if (logInfo.getLogLV().equals("2"))
            return new Color(LogColor.COLOR_2);
        else if (logInfo.getLogLV().equals("5"))
            return new Color(LogColor.COLOR_5);
        else
            return Color.BLACK;
    }

    public int getLogLV(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return LogInfo.LOG_LV_VERBOSE;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return LogInfo.LOG_LV_FATAL;
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E"))
            return LogInfo.LOG_LV_ERROR;
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W"))
            return LogInfo.LOG_LV_WARN;
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I"))
            return LogInfo.LOG_LV_INFO;
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D"))
            return LogInfo.LOG_LV_DEBUG;
        else
            return LogInfo.LOG_LV_VERBOSE;
    }

    //04-17 09:01:18.910 D/LightsService(  139): BKL : 106
    public boolean isNormal(String strText) {
        if (strText.length() < 22) return false;

        String strLevel = strText.substring(19, 21);
        if (strLevel.equals("D/")
                || strLevel.equals("V/")
                || strLevel.equals("I/")
                || strLevel.equals("W/")
                || strLevel.equals("E/")
                || strLevel.equals("F/")
                )
            return true;
        return false;
    }

    //04-20 12:06:02.125   146   179 D BatteryService: update start
    public boolean isThreadTime(String strText) {
        if (strText.length() < 34) return false;

        String strLevel = strText.substring(31, 33);
        if (strLevel.equals("D ")
                || strLevel.equals("V ")
                || strLevel.equals("I ")
                || strLevel.equals("W ")
                || strLevel.equals("E ")
                || strLevel.equals("F ")
                )
            return true;
        return false;
    }

    //    <4>[19553.494855] [DEBUG] USB_SEL(1) HIGH set USB mode
    public boolean isKernel(String strText) {
        if (strText.length() < 18) return false;

        String strLevel = strText.substring(1, 2);
        if (strLevel.equals("0")
                || strLevel.equals("1")
                || strLevel.equals("2")
                || strLevel.equals("3")
                || strLevel.equals("4")
                || strLevel.equals("5")
                || strLevel.equals("6")
                || strLevel.equals("7")
                )
            return true;
        return false;
    }

    public LogInfo getNormal(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_PID, false);
        if (stk.hasMoreElements()) {
            logInfo.setDate(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTag(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setPid(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_MESSAGE));
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + stk.nextToken(TOKEN_MESSAGE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("\\): ", ""));
            logInfo = mMessagePostProcessor.postProcess(logInfo);
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }

    public LogInfo getThreadTime(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_SPACE, false);
        if (stk.hasMoreElements()) {
            logInfo.setDate(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setPid(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setThread(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTag(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_MESSAGE).trim());
            if (logInfo.getMessage().length() != 0 && logInfo.getMessage().charAt(0) == ':') {
                logInfo.setTag(logInfo.getTag() + ":");
                logInfo.setMessage(logInfo.getMessage().substring(1).trim());
            }
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + stk.nextToken(TOKEN_MESSAGE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("\\): ", ""));
            logInfo = mMessagePostProcessor.postProcess(logInfo);
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }

    public LogInfo getKernel(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_KERNEL, false);
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_KERNEL));
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + " " + stk.nextToken(TOKEN_SPACE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("  ", ""));
            logInfo = mMessagePostProcessor.postProcess(logInfo);
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }

    public LogInfo parseLog(String strText) {
        if (isNormal(strText))
            return getNormal(strText);
        else if (isThreadTime(strText))
            return getThreadTime(strText);
        else if (isKernel(strText))
            return getKernel(strText);
        else {
            LogInfo logInfo = new LogInfo();
            logInfo.setMessage(strText);
            logInfo = mMessagePostProcessor.postProcess(logInfo);
            return logInfo;
        }
    }
}

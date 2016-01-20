package com.bt.tool;

import java.awt.*;

public class LogInfo {
    public static final int LOG_LV_VERBOSE = 1;
    public static final int LOG_LV_DEBUG = LOG_LV_VERBOSE << 1;
    public static final int LOG_LV_INFO = LOG_LV_DEBUG << 1;
    public static final int LOG_LV_WARN = LOG_LV_INFO << 1;
    public static final int LOG_LV_ERROR = LOG_LV_WARN << 1;
    public static final int LOG_LV_FATAL = LOG_LV_ERROR << 1;
    public static final int LOG_LV_ALL = LOG_LV_VERBOSE | LOG_LV_DEBUG | LOG_LV_INFO
            | LOG_LV_WARN | LOG_LV_ERROR | LOG_LV_FATAL;

    boolean m_bMarked;
    String m_strBookmark = "";
    String m_strDate = "";
    Integer m_intLine = -1;
    String m_strTime = "";
    String m_strLogLV = "";
    String m_strPid = "";
    String m_strThread = "";
    String m_strTag = "";
    String m_strMessage = "";
    long m_timestamp = -1l;
    Color m_TextColor;

    TYPE mType = TYPE.SYSTEM;

    public void display() {
        T.d("=============================================");
        T.d("m_bMarked      = " + m_bMarked);
        T.d("m_strBookmark  = " + m_strBookmark);
        T.d("m_strDate      = " + m_strDate);
        T.d("m_intLine      = " + m_intLine);
        T.d("m_strTime      = " + m_strTime);
        T.d("m_strLogLV     = " + m_strLogLV);
        T.d("m_strPid       = " + m_strPid);
        T.d("m_strThread    = " + m_strThread);
        T.d("m_strTag       = " + m_strTag);
        T.d("m_strMessage   = " + m_strMessage);
        T.d("m_timestamp   = " + m_timestamp);
        T.d("=============================================");
    }

    public Object getData(int nColumn) {
        switch (nColumn) {
            case LogFilterTableModel.COMUMN_LINE:
                return m_intLine;
            case LogFilterTableModel.COMUMN_DATE:
                return m_strDate;
            case LogFilterTableModel.COMUMN_TIME:
                return m_strTime;
            case LogFilterTableModel.COMUMN_LOGLV:
                return m_strLogLV;
            case LogFilterTableModel.COMUMN_PID:
                return m_strPid;
            case LogFilterTableModel.COMUMN_THREAD:
                return m_strThread;
            case LogFilterTableModel.COMUMN_TAG:
                return m_strTag;
            case LogFilterTableModel.COMUMN_BOOKMARK:
                return m_strBookmark;
            case LogFilterTableModel.COMUMN_MESSAGE:
                return m_strMessage;
        }
        return null;
    }

    public boolean containString(String src) {
        if (m_strMessage.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strTag.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strBookmark.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strDate.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strTime.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strLogLV.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strPid.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (m_strThread.toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        return false;
    }

    public enum TYPE {
        SYSTEM, DUMP_OF_SERVICE
    }
}

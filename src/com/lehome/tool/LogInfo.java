package com.lehome.tool;

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

    private boolean m_bMarked;
    private String m_strBookmark = "";
    private String m_strDate = "";
    private Integer m_intLine = -1;
    private String m_strTime = "";
    private String m_strLogLV = "";
    private String m_strPid = "";
    private String m_strThread = "";
    private String m_strTag = "";
    private String m_strMessage = "";
    private long m_timestamp = -1l;
    private Color m_TextColor;
    private TYPE mType = TYPE.SYSTEM;

    public void display() {
        T.d("=============================================");
        T.d("m_bMarked      = " + isMarked());
        T.d("m_strBookmark  = " + getBookmark());
        T.d("m_strDate      = " + getDate());
        T.d("m_intLine      = " + getLine());
        T.d("m_strTime      = " + getTime());
        T.d("m_strLogLV     = " + getLogLV());
        T.d("m_strPid       = " + getPid());
        T.d("m_strThread    = " + getThread());
        T.d("m_strTag       = " + getTag());
        T.d("m_strMessage   = " + getMessage());
        T.d("m_timestamp   = " + getTimestamp());
        T.d("=============================================");
    }

    public Object getData(int nColumn) {
        switch (nColumn) {
            case LogFilterTableModel.COMUMN_LINE:
                return getLine();
            case LogFilterTableModel.COMUMN_DATE:
                return getDate();
            case LogFilterTableModel.COMUMN_TIME:
                return getTime();
            case LogFilterTableModel.COMUMN_LOGLV:
                return getLogLV();
            case LogFilterTableModel.COMUMN_PID:
                return getPid();
            case LogFilterTableModel.COMUMN_THREAD:
                return getThread();
            case LogFilterTableModel.COMUMN_TAG:
                return getTag();
            case LogFilterTableModel.COMUMN_BOOKMARK:
                return getBookmark();
            case LogFilterTableModel.COMUMN_MESSAGE:
                return getMessage();
        }
        return null;
    }

    public boolean containString(String src) {
        if (getMessage().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getTag().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getBookmark().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getDate().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getTime().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getLogLV().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getPid().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        if (getThread().toLowerCase().contains(src.toLowerCase())) {
            return true;
        }
        return false;
    }

    public boolean isMarked() {
        return m_bMarked;
    }

    public void setMarked(boolean m_bMarked) {
        this.m_bMarked = m_bMarked;
    }

    public String getBookmark() {
        return m_strBookmark;
    }

    public void setBookmark(String m_strBookmark) {
        this.m_strBookmark = m_strBookmark;
    }

    public String getDate() {
        return m_strDate;
    }

    public void setDate(String m_strDate) {
        this.m_strDate = m_strDate;
    }

    public Integer getLine() {
        return m_intLine;
    }

    public void setLine(Integer m_intLine) {
        this.m_intLine = m_intLine;
    }

    public String getTime() {
        return m_strTime;
    }

    public void setTime(String m_strTime) {
        this.m_strTime = m_strTime;
    }

    public String getLogLV() {
        return m_strLogLV;
    }

    public void setLogLV(String m_strLogLV) {
        this.m_strLogLV = m_strLogLV;
    }

    public String getPid() {
        return m_strPid;
    }

    public void setPid(String m_strPid) {
        this.m_strPid = m_strPid;
    }

    public String getThread() {
        return m_strThread;
    }

    public void setThread(String m_strThread) {
        this.m_strThread = m_strThread;
    }

    public String getTag() {
        return m_strTag;
    }

    public void setTag(String m_strTag) {
        this.m_strTag = m_strTag;
    }

    public String getMessage() {
        return m_strMessage;
    }

    public void setMessage(String m_strMessage) {
        this.m_strMessage = m_strMessage;
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(long m_timestamp) {
        this.m_timestamp = m_timestamp;
    }

    public Color getTextColor() {
        return m_TextColor;
    }

    public void setTextColor(Color m_TextColor) {
        this.m_TextColor = m_TextColor;
    }

    public TYPE getType() {
        return mType;
    }

    public void setType(TYPE mType) {
        this.mType = mType;
    }

    public enum TYPE {
        SYSTEM, DUMP_OF_SERVICE
    }
}

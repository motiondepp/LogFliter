package com.bt.tool;

/**
 * Created by xinyu.he on 2016/1/22.
 */
public interface ILogRenderResolver {
    boolean isColumnShown(int idx);

    int getMinShownColumn();

    int getMaxShownColumn();

    float getFontSize();

    String GetHighlight();

    String GetFilterFind();

    String GetFilterShowTag();

    String GetSearchHighlight();
}

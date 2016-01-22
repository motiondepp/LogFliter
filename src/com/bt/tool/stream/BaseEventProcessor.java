package com.bt.tool.stream;

import com.bt.tool.LogInfo;

/**
 * Created by xinyu.he on 2016/1/22.
 */
public class BaseEventProcessor extends MessagePostProcessor {
    @Override
    protected LogInfo process(LogInfo srcInfo) {
        return srcInfo;
    }
}

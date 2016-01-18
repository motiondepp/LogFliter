package com.bt.tool;

/**
 * Created by xinyu.he on 2016/1/6.
 */
public interface IPostProcessor<T> {
    T postProcess(LogInfo info);

    boolean shouldProcess(LogInfo info);
}

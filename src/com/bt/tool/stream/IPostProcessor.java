package com.bt.tool.stream;

/**
 * Created by xinyu.he on 2016/1/6.
 */
public interface IPostProcessor<T> {
    T postProcess(T info);

    boolean shouldProcess(T info);
}

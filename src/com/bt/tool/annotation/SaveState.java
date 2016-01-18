package com.bt.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xinyu.he on 2016/1/14.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SaveState {
    String getter();
    String setter();
    Type type();

    enum Type {
        BOOL, STRING, INTEGER
    }
}

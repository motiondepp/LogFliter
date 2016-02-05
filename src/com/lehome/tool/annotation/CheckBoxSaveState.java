package com.lehome.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xinyu.he on 2016/1/14.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface CheckBoxSaveState {
    String getter() default "isSelected";
    String setter() default "setSelected";
    SaveState.Type type() default SaveState.Type.BOOL;
}

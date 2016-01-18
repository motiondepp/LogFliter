package com.bt.tool.annotation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by xinyu.he on 2016/1/14.
 */
public class StateSaver {

    private Class[] mSupportedSaveState = new Class[]{
            SaveState.class,
            CheckBoxSaveState.class,
            TextFieldSaveState.class,
            FieldSaveState.class
    };

    private ArrayList<Field> mFields = new ArrayList<>();
    private Object mContext;
    private String mSavePath;

    public StateSaver(Object target, String filePath) {
        register(target, filePath);
    }

    private void register(Object context, String filePath) {
        if (context == null) {
            throw new NullPointerException();
        }

        Class targetClass = context.getClass();
        Field[] fields = targetClass.getDeclaredFields();
        for (Field field : fields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    mFields.add(field);
                    break;
                }
            }
        }
        mContext = context;
        mSavePath = filePath;
    }

    public void save() {
        Properties p = new Properties();
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processSave(field, p, stateClass);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        try {
            p.store(new FileOutputStream(mSavePath), "create by StateSaver");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(mSavePath));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processLoad(field, p, stateClass);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    protected void processSave(Field field, Properties p, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object obj = field.get(mContext);
        String name = field.getName();

        if (stateClass == FieldSaveState.class) {
            p.setProperty(name, String.valueOf(obj));
            return;
        }

        Annotation annotation = field.getAnnotation(stateClass);
        Method annoGetter = annotation.annotationType().getMethod("getter");
        Method setMethod = obj.getClass().getMethod((String) annoGetter.invoke(annotation));
        Object value = setMethod.invoke(obj);
        p.setProperty(name, value.toString());
    }

    protected void processLoad(Field field, Properties p, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String value = p.getProperty(field.getName());
        if (value == null) {
            return;
        }
        if (stateClass == FieldSaveState.class) {
            if (isWrapperType(field.getType())) {
                field.set(mContext, field.getType().getMethod("valueOf").invoke(field, value));
            } else if (field.getType() == int.class) {
                field.setInt(mContext, Integer.valueOf(value));
            } else if (field.getType() == long.class) {
                field.setLong(mContext, Long.valueOf(value));
            } else if (field.getType() == boolean.class) {
                field.setBoolean(mContext, Boolean.valueOf(value));
            } else if (field.getType() == short.class) {
                field.setShort(mContext, Short.valueOf(value));
            } else if (field.getType() == byte.class) {
                field.setByte(mContext, Byte.valueOf(value));
            } else if (field.getType() == char.class) {
                field.setChar(mContext, value.charAt(0));
            } else if (field.getType() == double.class) {
                field.setDouble(mContext, Double.valueOf(value));
            } else if (field.getType() == float.class) {
                field.setFloat(mContext, Float.valueOf(value));
            }
            return;
        }

        Object obj = field.get(mContext);
        Annotation annotation = field.getAnnotation(stateClass);
        Method typper = annotation.annotationType().getMethod("type");
        SaveState.Type type = (SaveState.Type) typper.invoke(annotation);

        Method annoSetter = annotation.annotationType().getMethod("setter");
        Method setter;
        if (type == SaveState.Type.BOOL) {
            setter = obj.getClass().getMethod((String) annoSetter.invoke(annotation), boolean.class);
            setter.invoke(obj, Boolean.valueOf(value.toString()));
        } else if (type == SaveState.Type.STRING) {
            setter = obj.getClass().getMethod((String) annoSetter.invoke(annotation), String.class);
            setter.invoke(obj, value.toString());
        }
    }

    // ----------------------------------------------------------------------

    private static final Set<Class<?>> WRAPPER_TYPES;

    static {
        WRAPPER_TYPES = new HashSet<>();
        WRAPPER_TYPES.add(Boolean.class);
        WRAPPER_TYPES.add(Character.class);
        WRAPPER_TYPES.add(Byte.class);
        WRAPPER_TYPES.add(Short.class);
        WRAPPER_TYPES.add(Integer.class);
        WRAPPER_TYPES.add(Long.class);
        WRAPPER_TYPES.add(Float.class);
        WRAPPER_TYPES.add(Double.class);
        WRAPPER_TYPES.add(Void.class);
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }
}

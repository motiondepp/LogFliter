package com.lehome.tool.annotation;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private HashMap<String, Serializable> mSerializableMap = new HashMap<>();
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
        save(mSavePath);
    }

    public void save(String path) {
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processSave(field, stateClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        serialize(path);
    }

    public void load() {
        load(mSavePath);
    }

    public void load(String path) {
        if (!deserialize(path) || mSerializableMap.size() == 0) {
            return;
        }
        for (Field field : mFields) {
            for (Class stateClass : mSupportedSaveState) {
                if (field.isAnnotationPresent(stateClass)) {
                    try {
                        field.setAccessible(true);
                        processLoad(field, stateClass);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    protected void processSave(Field field, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object obj = field.get(mContext);
        String name = field.getName();

        if (stateClass == FieldSaveState.class) {
            putSerializable(name, obj);
            return;
        }

        Annotation annotation = field.getAnnotation(stateClass);
        Method annoGetter = annotation.annotationType().getMethod("getter");
        Method setMethod = obj.getClass().getMethod((String) annoGetter.invoke(annotation));
        Object value = setMethod.invoke(obj);
        putSerializable(name, value);
    }

    protected void processLoad(Field field, Class stateClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object value = mSerializableMap.get(field.getName());
        if (value == null) {
            return;
        }
        if (stateClass == FieldSaveState.class) {
            if (isWrapperType(field.getType())) {
                field.set(mContext, value);
            } else if (field.getType() == Object.class || field.getType() == Object[].class) {
                field.set(mContext, value);
            } else if (field.getType() == int.class) {
                field.setInt(mContext, (Integer) value);
            } else if (field.getType() == long.class) {
                field.setLong(mContext, (Long) value);
            } else if (field.getType() == boolean.class) {
                field.setBoolean(mContext, (Boolean) value);
            } else if (field.getType() == short.class) {
                field.setShort(mContext, (Short) value);
            } else if (field.getType() == byte.class) {
                field.setByte(mContext, (Byte) value);
            } else if (field.getType() == char.class) {
                field.setChar(mContext, (Character) value);
            } else if (field.getType() == double.class) {
                field.setDouble(mContext, (Double) value);
            } else if (field.getType() == float.class) {
                field.setFloat(mContext, (Float) value);
            } else {
                field.set(mContext, value);
            }
            return;
        }

        Object obj = field.get(mContext);
        Annotation annotation = field.getAnnotation(stateClass);
        Method annoSetter = annotation.annotationType().getMethod("setter");
        String setterName = (String) annoSetter.invoke(annotation);
        Method setter = null;
        for (Method method : obj.getClass().getMethods()) {
            if (method.getParameterCount() == 1 && method.getName().equals(setterName)) {
                setter = method;
                break;
            }
        }
        if (setter != null) {
            setter.invoke(obj, value);
        }
    }

    // ----------------------------------------------------------------------

    private void putSerializable(String key, Object obj) {
        if (obj instanceof Serializable && mSerializableMap != null) {
            mSerializableMap.put(key, (Serializable) obj);
        }
    }

    private boolean serialize(String path) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(path);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(mSerializableMap);
            oos.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean deserialize(String path) {
        FileInputStream fos = null;
        ObjectInputStream oos = null;
        try {
            fos = new FileInputStream(path);
            oos = new ObjectInputStream(fos);
            mSerializableMap = (HashMap<String, Serializable>) oos.readObject();
            oos.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
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

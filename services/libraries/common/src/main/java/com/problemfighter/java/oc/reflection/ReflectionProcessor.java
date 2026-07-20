package com.problemfighter.java.oc.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class ReflectionProcessor {
    private Class<?> wrapPrimitive(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return type;
        }

        if (type == Boolean.TYPE) return Boolean.class;
        if (type == Byte.TYPE) return Byte.class;
        if (type == Short.TYPE) return Short.class;
        if (type == Character.TYPE) return Character.class;
        if (type == Integer.TYPE) return Integer.class;
        if (type == Long.TYPE) return Long.class;
        if (type == Float.TYPE) return Float.class;
        if (type == Double.TYPE) return Double.class;
        if (type == Void.TYPE) return Void.class;
        return type;
    }

    private boolean isParameterCompatible(Class<?> declaredParameter, Class<?> actualParameter) {
        if (actualParameter == null) {
            return !declaredParameter.isPrimitive();
        }

        Class<?> declaredType = this.wrapPrimitive(declaredParameter);
        Class<?> actualType = this.wrapPrimitive(actualParameter);
        return declaredType.isAssignableFrom(actualType);
    }

    private boolean isExactParameterMatch(Class<?> declaredParameter, Class<?> actualParameter) {
        if (actualParameter == null) {
            return false;
        }

        return this.wrapPrimitive(declaredParameter) == this.wrapPrimitive(actualParameter);
    }

    public Method getCompatibleMethod(Class<?> c, String name, Class<?>... parameterTypes) {
        if (c == null || name == null) {
            return null;
        }

        Method assignableMatch = null;
        for (Class<?> current = c; current != null; current = current.getSuperclass()) {
            Method[] declaredMethods = current.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.isBridge() || method.isSynthetic()) {
                    continue;
                }

                if (!method.getName().equals(name)) {
                    continue;
                }

                Class<?>[] declaredParameters = method.getParameterTypes();
                if (declaredParameters.length != parameterTypes.length) {
                    continue;
                }

                boolean exactMatch = true;
                boolean assignable = true;
                for (int i = 0; i < declaredParameters.length; i++) {
                    if (!this.isExactParameterMatch(declaredParameters[i], parameterTypes[i])) {
                        exactMatch = false;
                    }
                    if (!this.isParameterCompatible(declaredParameters[i], parameterTypes[i])) {
                        assignable = false;
                        break;
                    }
                }

                if (exactMatch) {
                    method.setAccessible(true);
                    return method;
                }

                if (assignable && assignableMatch == null) {
                    assignableMatch = method;
                }
            }
        }

        if (assignableMatch != null) {
            assignableMatch.setAccessible(true);
        }

        return assignableMatch;
    }

    public List<Class<?>> getAllSuperClass(Class<?> klass) {
        List<Class<?>> classes = new ArrayList<>();
        if (klass != null) {
            for (Class<?> superclass = klass.getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
                classes.add(superclass);
            }
        }

        return classes;
    }

    public List<Class<?>> getAllClass(Class<?> klass) {
        if (klass == null) {
            return new ArrayList<>();
        } else {
            List<Class<?>> classes = this.getAllSuperClass(klass);
            classes.add(klass);
            return classes;
        }
    }

    public List<Field> getAllField(Class<?> klass) {
        List<Field> fields = new ArrayList<>();
        if (klass != null) {
            for (Class<?> pClass : this.getAllClass(klass)) {
                fields.addAll(Arrays.asList(pClass.getDeclaredFields()));
            }
        }

        return fields;
    }

    private Field getDeclaredField(Object object, String fieldName) {
        return this.getDeclaredField(object.getClass(), fieldName);
    }

    private Field getDeclaredField(Class<?> klass, String fieldName) {
        try {
            return klass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException var4) {
            return null;
        }
    }

    private Field getField(Object object, String fieldName) {
        try {
            Class<?> klass = object instanceof Class<?> ? (Class<?>) object : object.getClass();
            return klass.getField(fieldName);
        } catch (NoSuchFieldException var4) {
            return null;
        }
    }

    public Field getFieldFromObject(Object object, String fieldName) {
        return this.getFieldFromObject(object.getClass(), fieldName);
    }

    public Field getFieldFromObject(Class<?> klass, String fieldName) {
        Field field = this.getDeclaredField(klass, fieldName);
        if (field == null) {
            field = this.getField(klass, fieldName);
        }

        if (field != null) {
            field.setAccessible(true);
        }

        return field;
    }

    public Field getAnyFieldFromObject(Object object, String fieldName) {
        return this.getAnyFieldFromKlass(object.getClass(), fieldName);
    }

    public Field getAnyFieldFromKlass(Class<?> klass, String fieldName) {
        Field field = this.getFieldFromObject(klass, fieldName);
        if (field == null) {
            for (Class<?> superclass = klass.getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
                field = this.getDeclaredField(superclass, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }

        return field;
    }

    public <D> D newInstance(Class<D> klass) {
        try {
            Constructor<D> constructor = klass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException var3) {
            return null;
        }
    }

    public Boolean isPrimitive(Class<?> c) {
        return c.isPrimitive() ||
                c == String.class ||
                c == Boolean.class ||
                c == Byte.class ||
                c == Short.class ||
                c == Character.class ||
                c == Integer.class ||
                c == Float.class ||
                c == Double.class ||
                c == BigDecimal.class ||
                c == BigInteger.class ||
                c == LocalDate.class ||
                c == LocalDateTime.class ||
                c == Date.class ||
                c == OffsetDateTime.class ||
                c == Timestamp.class ||
                c == Long.class;
    }

    public Boolean isList(Class<?> c) {
        if (c == null) {
            return false;
        }

        if (List.class.isAssignableFrom(c)) {
            return true;
        }

        return c == Collection.class || (Collection.class.isAssignableFrom(c) && !Set.class.isAssignableFrom(c) && !Queue.class.isAssignableFrom(c));
    }

    public Boolean isSet(Class<?> c) {
        return c != null && Set.class.isAssignableFrom(c);
    }

    public Boolean isQueue(Class<?> c) {
        return c != null && Queue.class.isAssignableFrom(c);
    }

    public Boolean isMap(Class<?> c) {
        return c != null && Map.class.isAssignableFrom(c);
    }

    public Collection<?> instanceOfList(Class<?> c) {
        if (c == LinkedList.class) {
            return new LinkedList<>();
        } else if (c == Vector.class) {
            return new Vector<>();
        } else {
            return (Collection<?>) (c == Stack.class ? new Stack<>() : new ArrayList());
        }
    }

    public Queue<?> instanceOfQueue(Class<?> c) {
        return (Queue<?>) (c != ArrayDeque.class && c != Deque.class ? new PriorityQueue() : new ArrayDeque());
    }

    public Set<?> instanceOfSet(Class<?> c) {
        if (c != TreeSet.class && c != SortedSet.class) {
            return (Set<?>) (c == HashSet.class ? new HashSet() : new LinkedHashSet());
        } else {
            return new TreeSet();
        }
    }

    public Map<?, ?> instanceOfMap(Class<?> c) {
        if (c == HashMap.class) {
            return new HashMap();
        } else {
            return (Map<?, ?>) (c != TreeMap.class && c != SortedMap.class ? new LinkedHashMap() : new TreeMap());
        }
    }

    public Method getMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = this.getCompatibleMethod(c, name, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        return method;
    }

    public Boolean isMethodExist(Class<?> c, String name, Class<?>... parameterTypes) {
        return this.getCompatibleMethod(c, name, parameterTypes) != null;
    }

    public Object invokeMethod(Object object, String name, Object... parameterTypes) {
        try {
            int paramLength = parameterTypes.length;
            Class<?>[] classes = new Class[paramLength];

            for (int i = 0; i < paramLength; ++i) {
                classes[i] = parameterTypes[i] != null ? parameterTypes[i].getClass() : null;
            }

            Method method = this.getCompatibleMethod(object.getClass(), name, classes);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(object, parameterTypes);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }

            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
        }

        return null;
    }
}

package net.mlk.jmson.utils;

import net.mlk.jmson.Json;
import net.mlk.jmson.JsonList;
import net.mlk.jmson.annotations.JsonObject;
import net.mlk.jmson.annotations.JsonField;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class JsonConverter {

    /**
     * convert json to object & create new instance
     * @param json  json to convert
     * @param clazz class to create object
     * @param <T>   class  that extends JsonConvertible
     * @return new class instance
     */
    public static <T extends JsonConvertible> T convertToObject(Json json, Class<T> clazz) {
        try {
            return convertToObject(json, clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * convert json to object
     *
     * @param json json to convert
     * @param <T>  class that extends JsonConvertible
     * @return class instance
     */
    public static <T extends JsonConvertible> T convertToObject(Json json, T instance) {
        return convertToObject(json, instance, instance.getClass(), false);
    }

    /**
     * main convert method
     *
     * @param json     json to convert
     * @param instance instance of the object
     * @param clazz    current class
     * @param recurse  for JsonObject annotations check
     * @param <T>      class that extends JsonConvertible
     * @return class instance
     */
    private static <T extends JsonConvertible> T convertToObject(Json json, T instance, Class<?> clazz, boolean recurse) {
        // Check annotation and parse by keys if exist
        JsonObject jsonObject = clazz.getAnnotation(JsonObject.class);
        if (jsonObject != null && !recurse) {
            boolean check = jsonObject.checkExist();
            String[] keys = jsonObject.keyList();
            Iterator<String> iterator = Arrays.stream(keys).iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!key.isEmpty()) {
                    if (!json.containsKey(key)) {
                        if (check) {
                            throw new RuntimeException("Key " + key + " doesn't exists in json.");
                        }
                    } else {
                        convertToObject(json.getJson(key), instance, clazz, true);
                        return instance;
                    }
                }
            }
        }
        setFields(json, instance, clazz.getDeclaredFields());

        // Parse superclasses
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            if (isConvertible(superClass)) {
                convertToObject(json, instance, superClass, false);
            }
        }

        return instance;
    }

    /**
     * set values to fields
     * @param json     json with values
     * @param instance current class
     * @param fields   fields to set
     * @param <T>      class that extends JsonConvertible
     */
    private static <T extends JsonConvertible> void setFields(Json json, T instance, Field[] fields) {
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            boolean isConvertible = isConvertible(fieldType);
            JsonField fieldData = field.getAnnotation(JsonField.class);
            if (fieldData != null && !fieldData.key().isEmpty()) {
                fieldName = fieldData.key();
            }
            if (!json.containsKey(fieldName)) {
                continue;
            }

            Object value = json.get(fieldName);
            try {
                if (isConvertible && value instanceof Json) {
                    Object fieldInstance = field.get(instance);
                    if (fieldInstance == null) {
                        fieldInstance = fieldType.newInstance();
                    }
                    field.set(instance, convertToObject((Json) value, (JsonConvertible) fieldInstance));
                    return;
                }

                boolean isCollection = Collection.class.isAssignableFrom(fieldType);
                if ((isCollection || fieldType.isArray()) && value instanceof JsonList) {
                    JsonList list = (JsonList) value;
                    if (isCollection) {
                        if (fieldType == JsonList.class) {
                            if (fieldData != null) {
                                Class<?> t = fieldData.type();
                                if (t != JsonField.class) {
                                    value = castJsonList(t, list);
                                }
                            }
                        } else {
                            value = castCollection((ParameterizedType) field.getGenericType(), list);
                        }
                    } else {
                        value = castArray(fieldType, list);
                    }
                } else if (fieldData != null && value != null && fieldType == LocalDateTime.class) {
                    if (fieldData.dateFormat().isEmpty()) {
                        value = LocalDateTime.parse((CharSequence) value);
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fieldData.dateFormat());
                        value = LocalDateTime.parse((CharSequence) value, formatter);
                    }
                }

                field.set(instance, value);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * convert object to json
     * @param object instance of object to convert
     * @return json
     * @param <T> object must extends JsonConvertible
     */
    public static <T extends JsonConvertible> Json convertToJson(T object) {
        Json json = getFields(object.getClass().getDeclaredFields(), object.getClass(), object);
        Class<?> clazz = object.getClass();
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null && isConvertible(superClass)) {
            json.putAll(getFields(superClass.getDeclaredFields(), superClass, object));
            superClass = superClass.getSuperclass();
        }
        return json;
    }

    /**
     * get values from fields
     * @param fields fields to set
     * @param clazz current class
     * @param object instance
     * @param <T> class that extends JsonConvertible
     */
    private static <T extends JsonConvertible> Json getFields(Field[] fields, Class<?> clazz, T object) {
        Json json = new Json();
        JsonObject jsonObject = clazz.getAnnotation(JsonObject.class);

        for (Field field : fields) {
            try {
                JsonField fieldData = field.getAnnotation(JsonField.class);
                boolean ignoreNull = jsonObject != null && jsonObject.ignoreNull();
                Class<?> fieldType = field.getType();
                String fieldName = field.getName();
                Object value = field.get(object);
                if (fieldData != null) {
                    fieldName = fieldData.key().isEmpty() ? fieldName : fieldData.key();
                    ignoreNull = fieldData.ignoreNull();

                    if (fieldData.type() == String.class && value == null) {
                        value = "null";
                    }
                }

                if (value != null && (value.getClass().isArray() || Collection.class.isAssignableFrom(value.getClass()))) {
                    if (value.getClass().isArray()) {
                        value = new ArrayList<>(Arrays.asList((Object[]) value));
                        fieldType = fieldType.getComponentType();
                    } else {
                        if (!(value instanceof JsonList)) {
                            Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            fieldType = (Class<?>) type;
                        }
                    }
                    if (value instanceof JsonList) {
                        JsonList values = (JsonList) value;
                        JsonList objects = new JsonList();
                        for (Object o : values) {
                            if (!isConvertible(o.getClass())) {
                                objects.add(o);
                            } else {
                                objects.add(convertToJson((JsonConvertible) o));
                            }
                        }
                        value = objects;
                    } else if (isConvertible(fieldType)) {
                        ArrayList<?> values = (ArrayList<?>) value;
                        ArrayList<Object> objects = new ArrayList<>();
                        for (Object o : values) {
                            objects.add(convertToJson((JsonConvertible) o));
                        }
                        value = objects;
                    }
                }

                if (value != null || !ignoreNull) {
                    json.put(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return json;
    }


    /**
     * cast json list to type
     * @param type type to cast
     * @param list list to cast
     * @return jsonlist with values cast to type
     */
    private static JsonList castJsonList(Class<?> type, JsonList list) {
        if (isConvertible(type)) {
            JsonList newList = new JsonList();
            for (Object obj : list) {
                newList.add(convertToObject((Json) obj, type.asSubclass(JsonConvertible.class)));
            }
            return newList;
        }
        return list;
    }

    /**
     * cast list to type
     * @param type type to cast
     * @param list list to cast
     * @return list with values cast to type
     */
    private static Collection<?> castCollection(ParameterizedType type, JsonList list) {
        Class<?> collectionType = (Class<?>) type.getActualTypeArguments()[0];
        if (isConvertible(collectionType)) {
            JsonList newList = new JsonList();
            for (Object jsonObject : list) {
                newList.add(convertToObject((Json) jsonObject, collectionType.asSubclass(JsonConvertible.class)));
            }
            return newList.getListOfType(collectionType);
        }
        return list;
    }

    /**
     * cast array list to type
     * @param type type to cast
     * @param list list to cast
     * @return array with values cast to type
     */
    private static Object[] castArray(Class<?> type, JsonList list) {
        Class<?> arrayType = type.getComponentType();
        JsonList newList = new JsonList();
        if (isConvertible(arrayType)) {
            for (Object jsonObject : list) {
                newList.add(convertToObject((Json) jsonObject, arrayType.asSubclass(JsonConvertible.class)));
            }
        } else {
            for (Object jsonObject : list) {
                if (jsonObject instanceof Json || jsonObject instanceof JsonList) {
                    jsonObject = jsonObject.toString();
                }
                newList.add(arrayType.cast(jsonObject));
            }
        }
        Object[] newArray = (Object[]) Array.newInstance(arrayType, newList.size());
        System.arraycopy(newList.toArray(), 0, newArray, 0, newList.size());
        return newArray;
    }

    /**
     * check if class can be converted to json
     * @param clazz class to check
     * @return true if can
     */
    private static boolean isConvertible(Class<?> clazz) {
        return Arrays.asList(clazz.getInterfaces()).contains(JsonConvertible.class);
    }

    /**
     * cast object to type
     * @param object object to cast
     * @param type type for cast
     * @return cast object
     */
    public static Object castTo(Object object, Class<?> type) {
        if (object == null) {
            return null;
        }

        String value = object.toString();
        try {
            if (type == byte.class || type == Byte.class) {
                object = Byte.parseByte(value);
            } else if (type == short.class || type == Short.class) {
                object = Short.parseShort(value);
            } else if (type == int.class || type == Integer.class) {
                object = Integer.parseInt(value);
            } else if (type == long.class || type == Long.class) {
                object = Long.parseLong(value);
            } else if (type == float.class || type == Float.class) {
                object = Float.parseFloat(value);
            } else if (type == double.class || type == Double.class) {
                object = Double.parseDouble(value);
            } else if (type == boolean.class || type == Boolean.class) {
                object = Boolean.parseBoolean(value);
            } else if (type == String.class) {
                object = value;
            } else if (type == char.class || type == Character.class) {
                if (value.length() > 1) {
                    object = value.charAt(0);
                }
            } else {
                object = castObject(object, type);
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        return object;
    }

    /**
     * default cast
     * @param object object to cast
     * @param type type to cast
     * @return cast object
     */
    private static Object castObject(Object object, Class<?> type) {
        try {
            return type.cast(object);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

}

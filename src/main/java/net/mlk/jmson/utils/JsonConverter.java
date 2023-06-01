package net.mlk.jmson.utils;

import net.mlk.jmson.Json;
import net.mlk.jmson.JsonList;
import net.mlk.jmson.annotations.JsonField;
import net.mlk.jmson.annotations.JsonObject;

import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JsonConverter {

    /**
     * convert json to object & create new instance
     * @param json  json to convert
     * @param clazz class to create object
     * @param <T>   class  that extends JsonConvertible
     * @return new class instance
     */
    public static <T extends JsonConvertible> T convertToObject(Json json, Class<T> clazz) {
        return convertToObject(json, getDefaultConstructor(clazz));
    }

    /**
     * convert json to object
     *
     * @param json json to convert
     * @param <T>  class that extends JsonConvertible
     * @return class instance
     */
    public static <T extends JsonConvertible> T convertToObject(Json json, T instance) {
        return convertToObject(json, instance, instance.getClass());
    }

    /**
     * main convert method
     *
     * @param json     json to convert
     * @param instance instance of the object
     * @param clazz    current class
     * @param <T>      class that extends JsonConvertible
     * @return class instance
     */
    private static <T extends JsonConvertible> T convertToObject(Json json, T instance, Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != Object.class) {
            convertToObject(json, instance, superClass);
        }

        JsonObject jsonObject = clazz.getAnnotation(JsonObject.class);
        String globalDateFormat = null;
        boolean autoConvert = true;
        if (jsonObject != null) {
            json = getObjectValues(json, jsonObject);
            if (!jsonObject.dateFormat().isEmpty()) {
                globalDateFormat = jsonObject.dateFormat();
            }
            autoConvert = jsonObject.autoConvert();
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = getFieldName(field);
            Class<?> fieldType = field.getType();
            boolean needAutoConvert = true;

            if (json.containsKey(fieldName)) {
                try {
                    field.setAccessible(true);
                    JsonField jsonField = field.getAnnotation(JsonField.class);
                    Object value = json.get(fieldName);

                    if (value != null) {
                        if (isConvertible(fieldType)) {
                            value = convertToObject(json.getJson(fieldName), fieldType.asSubclass(JsonConvertible.class));
                        } else if (fieldType == LocalDateTime.class) {
                            try {
                                if (jsonField == null || jsonField.dateFormat().isEmpty()) {
                                    value = globalDateFormat != null ? getLocalDateTime(value.toString(), globalDateFormat) : null;
                                } else {
                                    value = getLocalDateTime(value.toString(), jsonField.dateFormat());
                                }
                            } catch (DateTimeParseException ex) {
                                throw new RuntimeException("Can't parse datetime \"" + value + "\" at " + field);
                            }
                        } else if (value instanceof JsonList && (fieldType.isArray() || Collection.class.isAssignableFrom(fieldType))) {
                            Class<?> defaultType = fieldType;
                            boolean allowArray = true;
                            if (jsonField != null) {
                                if (jsonField.type() != JsonField.class) {
                                    defaultType = jsonField.type();
                                }
                                for (Class<?> cl : jsonField.types()) {
                                    if (!isConvertible(cl)) {
                                        continue;
                                    }
                                    for (int i = 0; i < ((JsonList) value).size(); i++) {
                                        JsonObject jo = cl.getAnnotation(JsonObject.class);
                                        if (jo != null && !jo.methodName().isEmpty()) {
                                            try {
                                                Json obj = ((JsonList) value).getJson(i);
                                                if (obj == null) {
                                                    continue;
                                                }
                                                Method method = cl.getDeclaredMethod(jo.methodName(), Json.class);
                                                method.setAccessible(true);
                                                if ((boolean) method.invoke(getDefaultConstructor(cl.asSubclass(JsonConvertible.class)), obj)) {
                                                    ((JsonList) value).set(i, convertToObject(obj, cl.asSubclass(JsonConvertible.class)));
                                                }
                                            } catch (NoSuchMethodException | IllegalAccessException |
                                                     InvocationTargetException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    }
                                }
                                allowArray = jsonField.types().length == 0;
                            }

                            if (fieldType.isArray()) {
                                if (allowArray) {
                                    value = castToArray((JsonList) value, fieldType);
                                } else {
                                    throw new RuntimeException("Can't set multi types to array fields");
                                }
                            } else if (fieldType == JsonList.class) {
                                value = castToCollection((JsonList) value, defaultType);
                            } else {
                                if (defaultType == fieldType) {
                                    value = castToCollection((JsonList) value, (ParameterizedType) field.getGenericType());
                                } else {
                                    value = castToCollection((JsonList) value, defaultType);
                                }
                            }
                            needAutoConvert = false;
                        }
                    }

                    if (value != null && value.getClass() != fieldType && autoConvert && needAutoConvert) {
                        try {
                            value = castTo(value, fieldType);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("[Jmson AutoConvert] can't cast " + value + " to " + fieldType + " in " + field);
                        }
                    }
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return instance;
    }

    public static <T extends JsonConvertible> Json convertToJson(T instance) {
        return convertToJson(instance, instance.getClass());
    }

    private static <T extends JsonConvertible> Json convertToJson(Object instance, Class<T> clazz) {
        Json json = new Json();
        Json parentJson = new Json();
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != Object.class) {
            parentJson.putAll(convertToJson(instance, superClass.asSubclass(JsonConvertible.class)));
        }
        JsonObject jsonObject = clazz.getAnnotation(JsonObject.class);
        String key = null;

        if (jsonObject != null && !jsonObject.key().equals("JmsonKeyTemplate")) {
            key = jsonObject.key();
        }

        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String fieldName = getFieldName(field);
                Class<?> fieldType = field.getType();
                Object value = field.get(instance);
                boolean ignoreNull = jsonObject != null && jsonObject.ignoreNull();
                JsonField fieldData = field.getAnnotation(JsonField.class);
                if (fieldData != null) {
                    ignoreNull = fieldData.ignoreNull();

                    if (fieldData.type() == String.class && value == null) {
                        value = "null";
                    }
                }

                if (value != null) {
                    Class<?> valueType = value.getClass();
                    if (isConvertible(value.getClass())) {
                        value = convertToJson((JsonConvertible) value);
                    } else if ((value.getClass().isArray() || Collection.class.isAssignableFrom(value.getClass()))) {
                        if (value.getClass().isArray()) {
                            JsonList list = new JsonList();
                            list.addAll(Arrays.asList((Object[]) value));
                            value = list;
                            fieldType = fieldType.getComponentType();
                        } else {
                            if (!(value instanceof JsonList)) {
                                Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                                fieldType = (Class<?>) type;
                            }
                        }

                        if (value instanceof JsonList || isConvertible(fieldType)) {
                            List<Object> values = (List<Object>) value;
                            JsonList objects = new JsonList();
                            for (Object o : values) {
                                if (!isConvertible(o.getClass())) {
                                    objects.add(o);
                                } else {
                                    objects.add(convertToJson((JsonConvertible) o));
                                }
                            }
                            value = objects;
                        }
                    }
                }

                if (value != null || !ignoreNull) {
                    json.append(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (key == null) {
            parentJson.putAll(json);
            return parentJson;
        } else {
            Json newJson = new Json();
            newJson.putAll(parentJson);
            newJson.append(key, json);
            return newJson;
        }
    }

    /**
     * get private default constructor instance
     * @param clazz class of the constructor
     * @return instance os class
     */
    private static <T extends JsonConvertible> T getDefaultConstructor(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get field name with annotation
     * @param field field
     * @return field name
     */
    private static String getFieldName(Field field) {
        JsonField jsonField = field.getAnnotation(JsonField.class);
        if (jsonField != null) {
            if (!jsonField.key().equals("JmsonKeyTemplate")) {
                return jsonField.key();
            }
        }
        return field.getName();
    }

    /**
     * create json with new object values by keys from JsonObject annotation
     * @param json old json
     * @param jsonObject annotation with keys
     * @return new json with object values by keys
     */
    private static Json getObjectValues(Json json, JsonObject jsonObject) {
        Json newJson = json;
        if (!jsonObject.key().equals("JmsonKeyTemplate")) {
            newJson = new Json();
            if (json.containsKey(jsonObject.key())) {
                Object subJson = json.get(jsonObject.key());
                if (subJson instanceof Json) {
                    newJson.putAll((Json) subJson);
                }
            }
        }

        if (jsonObject.keys().length != 0) {
            if (newJson == json) {
                newJson = new Json();
            }
            for (String key : jsonObject.keys()) {
                if (json.containsKey(key)) {
                    Object subJson = json.get(key);
                    if (subJson instanceof Json) {
                        newJson.putAll((Json) subJson);
                    }
                }
            }
        }
        return newJson;
    }

    /**
     * @param date date
     * @param format date format
     * @return LocalDateTime
     */
    private static LocalDateTime getLocalDateTime(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(date, formatter);
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
     * cast array list to type
     * @param type type to cast
     * @param list list to cast
     * @return array with values cast to type
     */
    private static Object[] castToArray(JsonList list, Class<?> type) {
        Class<?> arrayType = type.getComponentType();
        JsonList newList = new JsonList();
        if (isConvertible(arrayType)) {
            for (Object jsonObject : list) {
                newList.add(convertToObject((Json) jsonObject, arrayType.asSubclass(JsonConvertible.class)));
            }
        } else {
            for (Object jsonObject : list) {
                if (arrayType == String.class && jsonObject instanceof net.mlk.jmson.JsonObject) {
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
     * cast list to type
     * @param type type to cast
     * @param list list to cast
     * @return list with values cast to type
     */
    private static Collection<?> castToCollection(JsonList list, ParameterizedType type) {
        Class<?> collectionType = (Class<?>) type.getActualTypeArguments()[0];
        return castToCollection(list, collectionType);
    }

    /**
     * cast list to type
     * @param type type to cast
     * @param list list to cast
     * @return list with values cast to type
     */
    private static Collection<?> castToCollection(JsonList list, Class<?> type) {
        if (isConvertible(type)) {
            JsonList newList = new JsonList();
            for (Object jsonObject : list) {
                if (jsonObject instanceof Json) {
                    newList.add(convertToObject((Json) jsonObject, type.asSubclass(JsonConvertible.class)));
                } else {
                    newList.add(jsonObject);
                }
            }
            return newList;
        }
        return list;
    }

    /**
     * auto parses values to their type
     * @param object object to parse
     * @return parsed object
     */
    public static Object autoParseToType(Object object) {
        if (object == null) {
            return null;
        }
        String value = object.toString();
        if (value.equalsIgnoreCase("true")) {
            object = true;
        } else if (value.equalsIgnoreCase("false")) {
            object = false;
        } else if (value.matches("[+-]?[0-9]+")) {
            try {
                object = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                object = Long.parseLong(value);
            }
        } else if (value.matches("[+-]?[0-9]*\\.[0-9]+")) {
            try {
                object = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                object = Double.parseDouble(value);
            }
        } else if (value.equals("null")) {
            object = null;
        }
        return object;
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
            object = type.cast(object);
        }
        return object;
    }
}

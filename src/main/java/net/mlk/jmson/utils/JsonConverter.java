package net.mlk.jmson.utils;

import net.mlk.jmson.Json;
import net.mlk.jmson.JsonList;
import net.mlk.jmson.annotations.JsonValue;
import net.mlk.jmson.annotations.SubJson;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonConverter {

    /**
     * Convert string json to Object
     * @param rawJsonString json string
     * @param object a class of object for example Test.class
     * @return new Instance of given object
     * @param <T> class that implements JsonConvertible
     */
    public static <T extends JsonConvertible> T convertToObject(String rawJsonString, Class<T> object) {
        try {
            return convertToObject(rawJsonString, object.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert string json to Object
     * @param rawJsonString json string
     * @param object instance of the object
     * @return object
     * @param <T> class that implements JsonConvertible
     */
    public static <T extends JsonConvertible> T convertToObject(String rawJsonString, T object) {
        return convertToObject(Json.parseFromString(rawJsonString), object);
    }

    /**
     * Convert json to Object
     * @param json json
     * @param object a class of object for example Test.class
     * @return new Instance of given object
     * @param <T> class that implements JsonConvertible
     */
    public static <T extends JsonConvertible> T convertToObject(Json json, Class<T> object) {
        try {
            return convertToObject(json, object.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends JsonConvertible> T convertToObject(Json json, T object) {
        return convertToObject(json, object, false);
    }

    /**
     * Convert json to Object
     * @param json json
     * @param object instance of the object
     * @param recurse if method use for recursion
     * @return object
     * @param <T> class that implements JsonConvertible
     */
    private static <T extends JsonConvertible> T convertToObject(Json json, T object, boolean recurse) {
        Class<?> clazz = object.getClass();
        Class<?> superClass = clazz.getSuperclass();
        List<Field> fields = new ArrayList<>();

        try {
            SubJson subJson = object.getClass().getAnnotation(SubJson.class);
            if (subJson != null && !recurse) {
                String name = subJson.key();
                boolean includeParent = subJson.includeParent();

                if (!name.isEmpty() && json.containsKey(name) || !subJson.checkKeyExist()) {
                    Json jsonPart = json.getJson(name);
                    if (jsonPart == null) {
                        throw new RuntimeException("Json object by key " + name + " doesn't exists.");
                    }
                    convertToObject(json.getJson(name), object, true);
                } else {
                    convertToObject(json, object, true);
                }
                if (includeParent) {
                    while (superClass != null &&
                            Arrays.asList(superClass.getInterfaces()).contains(JsonConvertible.class)) {
                        fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
                        superClass = superClass.getSuperclass();
                    }
                }
            } else {
                fields = Arrays.asList(clazz.getDeclaredFields());
            }

            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                boolean isConvertible = Arrays.asList(fieldType.getInterfaces()).contains(JsonConvertible.class);
                boolean isList = fieldType == JsonList.class;
                JsonValue jsonValue = field.getAnnotation(JsonValue.class);
                if (jsonValue != null) {
                    fieldName = jsonValue.key();
                }

                if (json.containsKey(fieldName)) {
                    Object value = json.get(fieldName);
                    if (jsonValue != null) {
                        if (jsonValue.autoConvert() && jsonValue.dateFormat().isEmpty()) {
                            if (value != null && fieldType != value.getClass()) {
                                value = castTo(value, fieldType);
                            }
                        } else if (fieldType == LocalDateTime.class && !jsonValue.dateFormat().isEmpty()) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(jsonValue.dateFormat());
                            value = LocalDateTime.parse((CharSequence) value, formatter);
                        }
                    }

                    if (isConvertible) {
                        Object instance = field.get(object);
                        if (instance == null) {
                            instance = fieldType.newInstance();
                            field.set(object, instance);
                        }
                        if (Json.isJson(json.getString(fieldName))) {
                            convertToObject(json.getJson(fieldName), (JsonConvertible) instance);
                            continue;
                        }
                    }
                    else if (isList && jsonValue != null) {
                        JsonList list = new JsonList();
                        for (Json obj : json.getListWithJsons(fieldName)) {
                            Class<?> objectType = jsonValue.type();

                            if (jsonValue.types().length != 0 && jsonValue.types()[0] != JsonValue.class) { // checking object class
                                for (Class<?> type : jsonValue.types()) {
                                    try {
                                        Method method = type.getDeclaredMethod("validateJson", Json.class);
                                        method.setAccessible(true);
                                        if ((boolean) method.invoke(type.newInstance(), obj)) {
                                            objectType = type;
                                        }
                                    } catch (NoSuchMethodException | InvocationTargetException ignored) {
                                    } // Method is optional
                                }
                            }

                            if (objectType != JsonValue.class) {
                                if (!Arrays.asList(objectType.getInterfaces()).contains(JsonConvertible.class)) {
                                    throw new RuntimeException("Object " + objectType + " doesn't implements JsonConvertible interface");
                                }
                                list.add(convertToObject(obj, (JsonConvertible) objectType.newInstance()));
                            }
                        }
                        value = list;
                    }
                    field.set(object, value);
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }

        return object;
    }

    /**
     * Method for cast object to given type if it meets the conditions
     * @param object object to cast
     * @param type type
     * @return an object reduced to the type
     */
    private static Object castTo(Object object, Class<?> type) {
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
        }
        return object;
    }

}

package net.mlk.jmson;

import net.mlk.jmson.json.JmsonObject;
import net.mlk.jmson.json.Json;
import java.lang.reflect.Field;

public class Jmson {
    private Json json;

    /**
     * Default constructor which save value of json
     * @param rawJson json string
     */
    public Jmson(String rawJson) {
        this.json = parseJson(rawJson);
    }

    /**
     * Constructor in which you can specify whether you want to disable type parsing in toString() method
     * @param rawJson json string
     * @param parseTypes specify type parsing
     */
    public Jmson(String rawJson, boolean parseTypes) {
        this.json = parseJson(rawJson, parseTypes);
    }

    /**
     * Constructor which parse json to specified object (Test function)
     * @param rawJson json string
     * @param object object with fields
     */
    public Jmson(String rawJson, JmsonObject object) {
        parseJson(rawJson, object);
    }

    /**
     * Default json parser method
     * @param rawJson json string
     * @return Json object
     */
    public static Json parseJson(String rawJson) {
        return parseJson(rawJson, true);
    }

    /**
     * Parser method in which you can specify whether you want to disable type parsing in toString() method
     * @param rawJson json string
     * @param parseTypes specify type parsing
     * @return Json object
     */
    public static Json parseJson(String rawJson, boolean parseTypes) {
        return Json.parseFromString(rawJson, parseTypes);
    }

    /**
     * Parser method which parse json to specified object (Test function)
     * @param rawJson json string
     * @param object object with fields
     */
    public static <T extends JmsonObject> T parseJson(String rawJson, T object) {
        Field[] fields = object.getClass().getFields();
        Json js = parseJson(rawJson);

        for (Field field : fields) {
            try {
                String name = field.getName();
                Class<?> type = field.getType();
                Object value;
                if (js.containsKey(name)) {
                    field.setAccessible(true);
                    if (type.equals(byte.class)) value = js.getByte(name);
                    else if (type.equals(short.class)) value = js.getShort(name);
                    else if (type.equals(int.class)) value = js.getInteger(name);
                    else if (type.equals(long.class)) value = js.getLong(name);
                    else if (type.equals(float.class)) value = js.getFloat(name);
                    else if (type.equals(double.class)) value = js.getDouble(name);
                    else if (type.equals(boolean.class)) value = js.getBoolean(name);
                    else value = js.get(name);

                    field.set(object, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return object;
    }

    /**
     * Parser method which parse json from specified object (Test function)
     * @param object object with fields
     */
    public static Json parseJsonFromObject(JmsonObject object) {
        return parseJsonFromObject(object, true);
    }

    /**
     * Parser method which parse json from specified object (Test function)
     * @param object object with fields
     * @param parseTypes specify type parsing
     */
    public static Json parseJsonFromObject(JmsonObject object, boolean parseTypes) {
        return parseJsonFromObject(object, false, parseTypes);
    }

    /**
     * Parser method which parse json from specified object (Test function)
     * @param object object with fields
     * @param ignoreNull if you want to ignore null fields
     * @param parseTypes specify type parsing
     */
    public static Json parseJsonFromObject(JmsonObject object, boolean ignoreNull, boolean parseTypes) {
        Field[] fields = object.getClass().getFields();
        Json json = new Json(parseTypes);

        for (Field field : fields) {
            try {
                String name = field.getName();
                Object value = field.get(object);
                if (ignoreNull && value == null) {
                    continue;
                }
                json.put(name, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return json;
    }

    /**
     * @return Json object
     */
    public Json getJson() {
        return this.json;
    }

}

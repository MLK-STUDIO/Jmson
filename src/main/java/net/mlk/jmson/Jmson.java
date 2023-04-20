package net.mlk.jmson;

import net.mlk.jmson.json.JmsonObject;
import net.mlk.jmson.json.Json;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class Jmson {
    private Json json;

    /**
     * Default constructor which save value of json
     * @param rawJson json string
     */
    public Jmson(String rawJson) {
        this.json = parse(rawJson);
    }

    /**
     * Constructor in which you can specify whether you want to disable type parsing in toString() method
     * @param rawJson json string
     * @param parseTypes specify type parsing
     */
    public Jmson(String rawJson, boolean parseTypes) {
        this.json = parse(rawJson, parseTypes);
    }

    /**
     * Constructor which parse json to specified object (Test function)
     * @param rawJson json string
     * @param object object with fields
     */
    public Jmson(String rawJson, JmsonObject object) {
        parse(rawJson, object);
    }

    /**
     * Default json parser method
     * @param rawJson json string
     * @return Json object
     */
    public static Json parse(String rawJson) {
        return parse(rawJson, true);
    }

    /**
     * Parser method in which you can specify whether you want to disable type parsing in toString() method
     * @param rawJson json string
     * @param parseTypes specify type parsing
     * @return Json object
     */
    public static Json parse(String rawJson, boolean parseTypes) {
        return Json.parseFromString(rawJson, parseTypes);
    }

    /**
     * Parser method which parse json to specified object (Test function)
     * @param rawJson json string
     * @param object object with fields
     */
    public static void parse(String rawJson, JmsonObject object) {
        Field[] fields = object.getClass().getFields();
        Json js = parse(rawJson);

        for (Field field : fields) {
            try {
                String name = field.getName();
                Class<?> type = field.getType();
                if (js.containsKey(name)) {
                    field.setAccessible(true);
                    field.set(object, js.getByType(name, type));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @return Json object
     */
    public Json getJson() {
        return this.json;
    }

}

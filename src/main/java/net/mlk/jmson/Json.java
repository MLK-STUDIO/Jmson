package net.mlk.jmson;

import net.mlk.jmson.annotations.JsonValue;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Json extends ConcurrentHashMap<String, Object> implements JsonObject {
    private boolean parseTypes = true;

    /**
     * Default json object
     */
    public Json() {

    }

    /**
     * Json object parsed from string
     * type parsing is true by default
     * @param rawJsonString string to parse
     */
    public Json(String rawJsonString) {
        this.parseFromString(this.validateString(Objects.requireNonNull(rawJsonString)));
    }

    /**
     * Json object parsed from string where you can specify if you don't want parse types
     * @param rawJsonString string to parse
     * @param parseTypes type parsing in json: with true - 1. With false - "1"
     */
    public Json(String rawJsonString, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.parseFromString(this.validateString(Objects.requireNonNull(rawJsonString)));
    }

    public String getString(String key) {
        return String.valueOf(super.get(key));
    }

    public char getCharacter(String key) {
        String value = this.getString(key);
        if (value.length() != 1) {
            return '\0';
        }
        return value.charAt(0);
    }

    public byte getByte(String key) {
        return (byte) this.getShort(key);
    }

    public short getShort(String key) {
        return (short) this.getInteger(key);
    }

    public int getInteger(String key) {
        return (int) this.getLong(key);
    }

    public long getLong(String key) {
        String value = this.getString(key);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    public float getFloat(String key) {
        return (float) this.getDouble(key);
    }

    public double getDouble(String key) {
        String value = this.getString(key);
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    public JsonList getList(String key) {
        return (JsonList) this.get(key);
    }

    public Json getJson(String key) {
        return (Json) this.get(key);
    }

    public List<Json> getListWithJsons(String key) {
        List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(key))) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    public List<JsonList> getListWithLists(String key) {
        List<JsonList> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(key))) {
            if (obj instanceof JsonList) {
                result.add((JsonList) obj);
            }
        }
        return result;
    }

    /**
     * Method to put json values into object by name
     * @param object object to put
     */
    public <T extends JsonObject> T parseToObject(T object) {
        return parseToObject(this, object);
    }

    /**
     * Method for put json values into object by name
     * @param rawJsonString json string to parse
     * @param object object to put
     */
    public static <T extends JsonObject> T parseToObject(String rawJsonString, T object) {
        return parseToObject(parseFromStrnig(rawJsonString), object);
    }

    /**
     * Method for put json values into object by name
     * @param json json to parse
     * @param object object to put
     */
    public static <T extends JsonObject> T parseToObject(Json json, T object) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> type = field.getType();
            boolean isJsonObject = Arrays.asList(type.getInterfaces()).contains(JsonObject.class) &&
                    type != Json.class && type != JsonList.class;
            boolean isList = type == JsonList.class;
            boolean isJson = type == Json.class;

            JsonValue jsonValue = field.getAnnotation(JsonValue.class);
            if (jsonValue != null) {
                fieldName = jsonValue.name();
            }

            if (json.containsKey(fieldName)) {
                Object value = json.get(fieldName);
                field.setAccessible(true);

                try {
                    if (isJsonObject) {
                        Object obj = field.get(object);
                        if (obj == null) {
                            obj = type.newInstance();
                            field.set(object, obj);
                        }
                        parseToObject(json.getJson(fieldName), (JsonObject) obj);
                        continue;
                    }
                    if (jsonValue != null) {
                        Class<?> toCast = jsonValue.type();
                        if (toCast != Class.class && isList) {
                            Object list = field.get(object);
                            if (list == null) {
                                list = JsonList.class.newInstance();
                            }
                            for (Json obj : json.getListWithJsons(fieldName)) {
                                Object toWrite = toCast.newInstance();
                                parseToObject(obj, (JsonObject) toWrite);
                                ((JsonList) list).add(toWrite);
                            }
                            field.set(object, list);
                            continue;
                        } else if (toCast != Class.class) {
                            field.set(object, castTo(json.get(jsonValue.name()), toCast));
                            continue;
                        }
                    }
                    if (type != value.getClass()) {
                        value = castTo(value, type);
                        field.set(object, value);
                        continue;
                    }
                    field.set(object, value);
                } catch (IllegalAccessException | InstantiationException ignored) {}
            }
        }
        return object;
    }

    /**
     * Check if string is look like json
     * @param rawJsonString string to check
     * @return true if json
     */
    public static boolean isJson(String rawJsonString) {
        return rawJsonString.startsWith("{") && rawJsonString.endsWith("}");
    }

    /**
     * enable/disable type parsing
     * @param parseTypes false if you don't want to parse types
     */
    public void parseTypes(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    @Override
    public String toString() {
        Pattern pattern = Pattern.compile("(true)?(false)?([0-9]+[.]?[0-9]?)*");
        StringBuilder builder = new StringBuilder(super.size() * 16);
        builder.append("{");
        Iterator<Entry<String, Object>> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            Object obj = entry.getValue();
            if (obj == null) {
                if (this.parseTypes) {
                    builder.append("\"").append(key).append("\":null");
                } else {
                    builder.append("\"").append(key).append("\":\"").append("null").append("\"");
                }
            } else {
                boolean matches = pattern.matcher(obj.toString()).matches();
                if (!obj.toString().isEmpty() && (obj instanceof Json || obj instanceof JsonList || (this.parseTypes && matches))) {
                    builder.append("\"").append(key).append("\":").append(obj);
                } else {
                    builder.append("\"").append(key).append("\":\"").append(obj).append("\"");
                }
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * parses values to their type
     * @param object object to parse
     * @return parsed object
     */
    static Object parseToType(Object object) {
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
            object = Double.parseDouble(value);
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
        } else {
            object = type.cast(object);
        }
        return object;
    }

    /**
     * parse Json from string
     * @param rawJsonString string to parse
     * @return Json object
     */
    public static Json parseFromStrnig(String rawJsonString) {
        return new Json(rawJsonString);
    }

    /**
     * parse Json from string
     * @param rawJsonString string to parse
     * @param parseTypes if you need to parse types
     * @return Json object
     */
    public static Json parseFromStrnig(String rawJsonString, boolean parseTypes) {
        return new Json(rawJsonString, parseTypes);
    }

    /**
     * Parse current json from string
     * @param rawJsonString string to parse
     */
    private void parseFromString(String rawJsonString) {
        if (rawJsonString.isEmpty()) {
            return;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(numThreads);

        int level = 0;
        int stringLength = rawJsonString.length();
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            char currentChar = rawJsonString.charAt(i);
            char prevChar = i == 0 ? '\0' : rawJsonString.charAt(i - 1);

            if (currentChar == '\"' && prevChar != '\\') {
                String key = null;
                String value = null;
                boolean isQuoted = true;
                i += 1;
                for ( ; i <= stringLength; i++) {
                    currentChar = i == stringLength ? '\0' : rawJsonString.charAt(i);
                    prevChar = rawJsonString.charAt(i - 1);
                    level += currentChar == '{' || currentChar == '[' ? 1 :
                            currentChar == '}' || currentChar == ']' ? -1 : 0;
                    if (level == 0 && currentChar == '\"' && prevChar != '\\') {
                        isQuoted = !isQuoted;
                        continue;
                    } else if ((!isQuoted && currentChar == ',' && level == 0) || i == stringLength) {
                        value = block.toString().trim();
                        block.setLength(0);
                        break;
                    } else if (!isQuoted && currentChar == ':' && level == 0) {
                        key = block.toString().trim();
                        block.setLength(0);
                        continue;
                    }
                    block.append(currentChar);
                }
                if (key == null || value == null) {
                    continue;
                }
                if (isJson(value)) {
                    String finalKey = key;
                    String finalValue = value;
                    service.execute(() -> super.put(finalKey, new Json(finalValue, this.parseTypes)));
                } else if (JsonList.isList(value)) {
                    String finalKey = key;
                    String finalValue = value;
                    service.execute(() -> super.put(finalKey, new JsonList(finalValue, this.parseTypes)));
                } else {
                    if (this.parseTypes) {
                        super.put(key, parseToType(value));
                    } else {
                        super.put(key, value);
                    }
                }
            }
        }
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            service.shutdownNow();
        }
    }

    /**
     * Validate string
     * @param rawJsonString string to check
     * @return string without brackets if success
     * @throws IllegalArgumentException if fail
     */
    private String validateString(String rawJsonString) {
        if (isJson(rawJsonString)) {
            return rawJsonString.substring(1, rawJsonString.length() - 1);
        }
        throw new IllegalArgumentException("The String doesn't look like json.");
    }

}
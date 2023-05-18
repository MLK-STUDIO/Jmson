package net.mlk.jmson;

import net.mlk.jmson.utils.JsonConverter;

import java.util.*;

public class Json extends LinkedHashMap<String, Object> implements JsonObject {
    private boolean parseTypes = true;

    /**
     * Default json object
     */
    public Json() {

    }

    /**
     * Default json object with parseTypes
     * @param parseTypes if you want to parseTypes
     */
    public Json(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    /**
     * Json object parsed from string
     * type parsing is true by default
     * @param rawJsonString string to parse
     */
    public Json(String rawJsonString) {
        this.parser(this.validateString(Objects.requireNonNull(rawJsonString)));
    }

    /**
     * Json object parsed from string where you can specify if you don't want parse types
     * @param rawJsonString string to parse
     * @param parseTypes type parsing in json: with true - 1. With false - "1"
     */
    public Json(String rawJsonString, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.parser(this.validateString(Objects.requireNonNull(rawJsonString)));
    }

    /**
     * @return copied json
     */
    public Json copy() {
        Json json = new Json();
        json.putAll(this);
        json.parseTypes(this.parseTypes);
        return json;
    }

    /**
     * Custom method for put values
     * @param key key of the value
     * @param value value to put
     * @return current Json
     */
    public Json add(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * @param key key of the value
     * @return string
     */
    public String getString(String key) {
        return String.valueOf(super.get(key));
    }

    /**
     * @param key key of the value
     * @return char
     */
    public char getCharacter(String key) {
        String value = this.getString(key);
        return value.charAt(0);
    }

    /**
     * @param key key of the value
     * @return byte
     */
    public byte getByte(String key) {
        return (byte) this.getShort(key);
    }

    /**
     * @param key key of the value
     * @return short
     */
    public short getShort(String key) {
        return (short) this.getInteger(key);
    }

    /**
     * @param key key of the value
     * @return int
     */
    public int getInteger(String key) {
        return (int) this.getLong(key);
    }

    /**
     * @param key key of the value
     * @return long
     */
    public long getLong(String key) {
        String value = this.getString(key);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    /**
     * @param key key of the value
     * @return float
     */
    public float getFloat(String key) {
        return (float) this.getDouble(key);
    }

    /**
     * @param key key of the value
     * @return double
     */
    public double getDouble(String key) {
        String value = this.getString(key);
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    /**
     * @param key key of the value
     * @return bool
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    /**
     * @param key key of the value
     * @return JsonList
     */
    public JsonList getList(String key) {
        Object obj = this.get(key);
        if (obj == null) {
            return null;
        } else if (!(obj instanceof JsonList) && JsonList.isList(obj.toString())) {
            return new JsonList(obj.toString());
        } else if (!(obj instanceof JsonList)) {
            return null;
        }
        return (JsonList) obj;
    }

    /**
     * @param key key of the value
     * @return Json
     */
    public Json getJson(String key) {
        Object obj = this.get(key);
        if (obj == null) {
            return null;
        } else if (!(obj instanceof Json) && Json.isJson(obj.toString())) {
            return new Json(obj.toString());
        } else if (!(obj instanceof Json)) {
            return null;
        }
        return (Json) obj;
    }

    /**
     * @param key key of the value
     * @return List with jsons
     */
    public List<Json> getListWithJsons(String key) {
        List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(key))) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    /**
     * @param key key of the value
     * @return list with lists
     */
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
        StringBuilder builder = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> iterator = entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            Object obj = entry.getValue();
            if ((!(obj instanceof String) && this.parseTypes) || obj instanceof JsonObject) {
                builder.append("\"").append(key).append("\":").append(obj);
            } else {
                builder.append("\"").append(key).append("\":\"").append(obj).append("\"");
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append("}").toString();
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
                object = JsonConverter.castTo(value, Integer.class);
            } catch (NumberFormatException ex) {
                object = JsonConverter.castTo(value, Long.class);
            }
        } else if (value.matches("[+-]?[0-9]*\\.[0-9]+")) {
            object = JsonConverter.castTo(value, Double.class);
        } else if (value.equals("null")) {
            object = null;
        }
        return object;
    }

    /**
     * parse Json from string
     * @param rawJsonString string to parse
     * @return Json object
     */
    public static Json parseFromString(String rawJsonString) {
        return new Json(rawJsonString);
    }

    /**
     * parse Json from string
     * @param rawJsonString string to parse
     * @param parseTypes if you need to parse types
     * @return Json object
     */
    public static Json parseFromString(String rawJsonString, boolean parseTypes) {
        return new Json(rawJsonString, parseTypes);
    }

    /**
     * Parse current json from string
     * @param rawJsonString string to parse
     */
    private void parser(String rawJsonString) {
        if (rawJsonString.isEmpty()) {
            return;
        }

        int level = 0;
        int quoteLevel = 0;
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
                quoteLevel += 1;
                for ( ; i <= stringLength; i++) {
                    currentChar = i == stringLength ? '\0' : rawJsonString.charAt(i);
                    prevChar = rawJsonString.charAt(i - 1);
                    level += !isQuoted && (currentChar == '{' || currentChar == '[') ? 1 :
                            !isQuoted && (currentChar == '}' || currentChar == ']') ? -1 : 0;
                    if (!isQuoted && quoteLevel != 0 && currentChar == ',' || quoteLevel != 0 && i == stringLength) {
                        throw new RuntimeException("Expected value, but it is not, at: " + i);
                    } else if (!isQuoted && currentChar == ',' && level != 0) {
                        throw new RuntimeException("Invalid structure at: " + i);
                    } else if (level == 0 && currentChar == '\"' && prevChar != '\\') {
                        isQuoted = !isQuoted;
                        quoteLevel += isQuoted ? 1 : -1;
                        continue;
                    } else if (!isQuoted && currentChar == ',' || i == stringLength) {
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
                    throw new RuntimeException("Invalid key-value structure, at: " + i);
                }
                if (isJson(value) && prevChar != '\"') {
                    super.put(key, new Json(value, this.parseTypes));
                } else if (JsonList.isList(value) && prevChar != '\"') {
                    super.put(key, new JsonList(value, this.parseTypes));
                } else {
                    if (this.parseTypes && prevChar != '\"') {
                        super.put(key, parseToType(value));
                    } else {
                        super.put(key, value);
                    }
                }
            }
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
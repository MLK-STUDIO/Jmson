package net.mlk.jmson;

import net.mlk.jmson.utils.JsonConverter;

import java.util.*;

/**
 * main json class
 */
public class Json extends LinkedHashMap<String, Object> implements JsonObject {
    private boolean parseTypes = true;

    public Json() {
    }

    public Json(String rawJson) {
        this(rawJson, true);
    }

    public Json(String rawJson, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.parseFromString(rawJson);
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
     * custom method to add values and
     * get instance of current object
     * @param key key
     * @param value value to put
     * @return this
     */
    public Json append(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * @param key key of the value
     * @return string if exists
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
        return value == null ? '\0' : value.charAt(0);
    }

    /**
     * @param key key of the value
     * @return byte
     * @throws IllegalStateException if not exists
     */
    public byte getByte(String key) {
        return (byte) this.getShort(key);
    }

    /**
     * @param key key of the value
     * @return short
     * @throws IllegalStateException if not exists
     */
    public short getShort(String key) {
        return (short) this.getInteger(key);
    }


    /**
     * @param key key of the value
     * @return int
     * @throws IllegalStateException if not exists
     */
    public int getInteger(String key) {
        return (int) this.getLong(key);
    }

    /**
     * @param key key of the value
     * @return long
     * @throws IllegalStateException if not exists
     */
    public long getLong(String key) {
        String value = this.getString(key);
        if (value == null) {
            throw new IllegalStateException("Element " + key + " doesn't exists in json.");
        }
        return Long.parseLong(value);
    }

    /**
     * @param key key of the value
     * @return float
     * @throws IllegalStateException if not exists
     */
    public float getFloat(String key) {
        return (float) this.getDouble(key);
    }


    /**
     * @param key key of the value
     * @return double
     * @throws IllegalStateException if not exists
     */
    public double getDouble(String key) {
        String value = this.getString(key);
        if (value == null) {
            throw new IllegalStateException("Element " + key + " doesn't exists in json.");
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
        }
        String value = obj.toString();
        if (!(obj instanceof JsonList) && JsonList.isList(value)) {
            return new JsonList(value);
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
        }

        String value = obj.toString();
        if (!(obj instanceof Json) && Json.isJson(value)) {
            return new Json(value);
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
     * parse current object to json
     * @param rawJson json string to parse
     * @return new Json
     */
    private Json parseFromString(String rawJson) {
        if (!isJson(rawJson)) {
            throw new RuntimeException("Not json object. " + rawJson);
        }
        if (rawJson.length() - 2 <= 0) {
            return this;
        }
        super.clear();

        boolean quoted = false;
        StringBuilder block = new StringBuilder();
        String[] pair = new String[2];
        int level = 0;
        for (int i = 1; i < rawJson.length(); i++) {
            char currentChar = rawJson.charAt(i);
            char prevChar = rawJson.charAt(i - 1);
            level += (!quoted && (currentChar == '{' || currentChar == '[')) ? 1 :
                    (!quoted && (currentChar == '}' || currentChar == ']')) ? -1 : 0;

            if (level == 0 || i + 1 == rawJson.length()) {
                if (!quoted && currentChar == ',' || i  == rawJson.length() - 1) {
                    if (pair[0] == null) {
                        throw new RuntimeException("Expected value, but It's not at " + i);
                    }
                    pair[1] = JsonConverter.decodeUnicode(block.toString());

                    if (Json.isJson(pair[1]) && prevChar != '\"') {
                        super.put(pair[0], new Json(pair[1].trim(), this.parseTypes));
                    } else if (JsonList.isList(pair[1]) && prevChar != '\"') {
                        super.put(pair[0], new JsonList(pair[1].trim(), this.parseTypes));
                    } else {
                        if (this.parseTypes && prevChar != '\"') {
                            super.put(pair[0], JsonConverter.autoParseToType(pair[1]));
                        } else {
                            super.put(pair[0], pair[1]);
                        }
                    }
                    pair = new String[2];
                    block.setLength(0);
                    continue;
                } else if (!quoted && currentChar == ':') {
                    pair[0] = block.toString().trim();
                    block.setLength(0);
                    continue;
                } else if (currentChar == '\"' && prevChar != '\\') {
                    quoted = !quoted;
                    continue;
                }
            }

            if (currentChar == ' ' && level != 0 || quoted || currentChar != ' ') {
                block.append(currentChar);
            }
        }

        return this;
    }

    /**
     * set parse types parameter
     * @param parseTypes if false integers become a string etc
     * @return this
     */
    public Json parseTypes(boolean parseTypes) {
        this.parseTypes = parseTypes;
        return this;
    }

    /**
     * change current json values
     * @param rawJson new json string
     * @return parsed json
     */
    public Json setJsonString(String rawJson) {
        return this.parseFromString(rawJson);
    }

    /**
     * @param rawJson json string
     * @return true if string can be parsed to json
     */
    public static boolean isJson(String rawJson) {
        return rawJson != null && rawJson.trim().startsWith("{") && rawJson.endsWith("}");
    }

    @Override
    public String toString() {
        StringBuilder json = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> entryIterator = super.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, Object> entry = entryIterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            if ((!(value instanceof String) && this.parseTypes) || value instanceof JsonObject) {
                json.append("\"").append(key).append("\":").append(value);
            } else {
                json.append("\"").append(key).append("\":\"").append(value).append("\"");
            }
            if (entryIterator.hasNext()) {
                json.append(", ");
            }
        }
        return json.append("}").toString();
    }

}

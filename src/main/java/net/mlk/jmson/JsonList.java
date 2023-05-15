package net.mlk.jmson;

import java.util.*;
import java.util.regex.Pattern;

public class JsonList extends ArrayList<Object> implements JsonObject {
    private boolean parseTypes = true;

    /**
     * Default list object
     */
    public JsonList() {

    }

    /**
     * Default list object with parseTypes
     * @param parseTypes if you want to parseTypes
     */
    public JsonList(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    /**
     * List object parsed from string
     * type parsing is true by default
     * @param rawListString string to parse
     */
    public JsonList(String rawListString) {
        this.parser(this.validateString(Objects.requireNonNull(rawListString)));
    }

    /**
     * List object parsed from string where you can specify if you don't want parse types
     * @param rawListString string to parse
     * @param parseTypes type parsing in json: with true - 1. With false - "1"
     */
    public JsonList(String rawListString, boolean parseTypes) {
        this(rawListString);
        this.parseTypes = parseTypes;
    }

    /**
     *
     * Custom method for add values
     * @param value value to put
     * @return current List
     */
    public JsonList append(Object value) {
        super.add(value);
        return this;
    }

    /**
     * @param index index of the value
     * @return string
     */
    public String getString(int index) {
        return String.valueOf(super.get(index));
    }

    /**
     * @param index index of the value
     * @return char
     */
    public char getCharacter(int index) {
        String value = this.getString(index);
        if (value.length() != 1) {
            return '\0';
        }
        return value.charAt(0);
    }

    /**
     * @param index index of the value
     * @return byte
     */
    public byte getByte(int index) {
        return (byte) this.getShort(index);
    }

    /**
     * @param index index of the value
     * @return short
     */
    public short getShort(int index) {
        return (short) this.getInteger(index);
    }

    /**
     * @param index index of the value
     * @return int
     */
    public int getInteger(int index) {
        return (int) this.getLong(index);
    }

    /**
     * @param index index of the value
     * @return long
     */
    public long getLong(int index) {
        String value = this.getString(index);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    /**
     * @param index index of the value
     * @return float
     */
    public float getFloat(int index) {
        return (float) this.getDouble(index);
    }

    /**
     * @param index index of the value
     * @return double
     */
    public double getDouble(int index) {
        String value = this.getString(index);
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    /**
     * @param index index of the value
     * @return boolean
     */
    public boolean getBoolean(int index) {
        return Boolean.parseBoolean(this.getString(index));
    }

    /**
     * @param index index of the value
     * @return JsonList
     */
    public JsonList getList(int index) {
        return (JsonList) super.get(index);
    }

    /**
     * @param index index of the value
     * @return Json
     */
    public Json getJson(int index) {
        return (Json) super.get(index);
    }

    /**
     * @param index index of the list
     * @return list with jsons
     */
    public List<Json> getListWithJsons(int index) {
        List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)super.get(index))) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    /**
     * @return list with jsons
     */
    public List<Json> getListWithJsons() {
        List<Json> result = new ArrayList<>();
        for (Object obj : this) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    /**
     * @param index index of the list
     * @return list with lists
     */
    public List<JsonList> getListWithLists(int index) {
        List<JsonList> result = new ArrayList<>();
        for (Object obj : ((JsonList)super.get(index))) {
            if (obj instanceof JsonList) {
                result.add((JsonList) obj);
            }
        }
        return result;
    }

    public <T> List<T> getListOfType(Class<T> object) {
        List<T> result = new ArrayList<>();
        for (Object obj : this) {
            if (object.isInstance(obj)) {
                result.add(object.cast(obj));
            }
        }
        return result;
    }

    /**
     * Check if string is look like list
     * @param rawListString string to check
     * @return true if list
     */
    public static boolean isList(String rawListString) {
        return rawListString.startsWith("[") && rawListString.endsWith("]");
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
        StringBuilder builder = new StringBuilder("[");
        Iterator<Object> iterator = super.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (!(obj instanceof String)) {
                builder.append(obj);
            } else {
                builder.append("\"").append(obj).append("\"");
            }
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        return builder.append("]").toString();
    }

    /**
     * parse List from string
     * @param rawListString string to parse
     * @return Json object
     */
    public static JsonList parseFromString(String rawListString) {
        return new JsonList(rawListString);
    }

    /**
     * parse List from string
     * @param rawListString string to parse
     * @param parseTypes if you need to parse types
     * @return Json object
     */
    public static JsonList parseFromString(String rawListString, boolean parseTypes) {
        return new JsonList(rawListString, parseTypes);
    }

    /**
     * Parse current list from string
     * @param rawListString string to parse
     */
    private void parser(String rawListString) {
        if (rawListString.isEmpty()) {
            return;
        }

        int level = 0;
        int stringLength = rawListString.length();
        StringBuilder block = new StringBuilder();
        for (int i = 0; i <= stringLength; i++) {
            char currentChar = i == stringLength ? '\0' : rawListString.charAt(i);

            level += currentChar == '{' || currentChar == '[' ? 1 :
                    currentChar == '}' || currentChar == ']' ? -1 : 0;

            if ((level == 0 && currentChar == ',') || i == stringLength) {
                String value = block.toString().trim();
                if (Json.isJson(value)) {
                    super.add(new Json(value, this.parseTypes));
                } else if (isList(value)) {
                    super.add(new JsonList(value, this.parseTypes));
                } else {
                    boolean quoted = false;
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        quoted = true;
                    }
                    if (this.parseTypes && !quoted) {
                        super.add(Json.parseToType(value));
                    } else {
                        super.add(value);
                    }
                }
                block.setLength(0);
                continue;
            }
            block.append(currentChar);
        }
    }

    /**
     * Validate string
     * @param rawListString string to check
     * @return string without brackets if success
     * @throws IllegalArgumentException if fail
     */
    private String validateString(String rawListString) {
        if (isList(rawListString)) {
            return rawListString.substring(1, rawListString.length() - 1);
        }
        throw new IllegalArgumentException("The String doesn't look like list.");
    }

}

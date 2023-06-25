package net.mlk.jmson;

import net.mlk.jmson.utils.JsonConverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * JsonList class
 */
public class JsonList extends ArrayList<Object> implements JsonObject {
    private boolean parseTypes = true;

    public JsonList() {
    }

    public JsonList(String rawList) {
        this(rawList, true);
    }

    public JsonList(String rawList, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.parseFromString(rawList);
    }

    /**
     * @return copied list
     */
    public JsonList copy() {
        JsonList json = new JsonList();
        json.addAll(this);
        json.parseTypes(this.parseTypes);
        return json;
    }

    /**
     * custom method to add values and
     * get instance of current object
     * @param value value to put
     * @return this
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
     * @return first char in string
     */
    public char getCharacter(int index) {
        String value = this.getString(index);
        return value == null ? '\0' : value.charAt(0);
    }

    /**
     * @param index index of the value
     * @return byte
     * @throws IllegalStateException if not exists
     */
    public byte getByte(int index) {
        return (byte) this.getShort(index);
    }

    /**
     * @param index index of the value
     * @return short
     * @throws IllegalStateException if not exists
     */
    public short getShort(int index) {
        return (short) this.getInteger(index);
    }

    /**
     * @param index index of the value
     * @return int
     * @throws IllegalStateException if not exists
     */
    public int getInteger(int index) {
        return (int) this.getLong(index);
    }

    /**
     * @param index index of the value
     * @return long
     * @throws IllegalStateException if not exists
     */
    public long getLong(int index) {
        String value = this.getString(index);
        if (value == null) {
            throw new IllegalStateException("Element at " + index + " doesn't exists in json.");
        }
        return Long.parseLong(value);
    }

    /**
     * @param index index of the value
     * @return float
     * @throws IllegalStateException if not exists
     */
    public float getFloat(int index) {
        return (float) this.getDouble(index);
    }

    /**
     * @param index index of the value
     * @return double
     * @throws IllegalStateException if not exists
     */
    public double getDouble(int index) {
        String value = this.getString(index);
        if (value == null) {
            throw new IllegalStateException("Element at " + index + " doesn't exists in json.");
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
        Object obj = this.get(index);
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
     * @param index index of the value
     * @return Json
     */
    public Json getJson(int index) {
        Object obj = this.get(index);
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
     * @return current list with jsons
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

    /**
     * @return current list with lists
     */
    public List<JsonList> getListWithLists() {
        List<JsonList> result = new ArrayList<>();
        for (Object obj : this) {
            if (obj instanceof JsonList) {
                result.add((JsonList) obj);
            }
        }
        return result;
    }

    /**
     * @param object class to cast
     * @return T list
     * @param <T> param to cast
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getListOfType(Class<T> object) {
        List<T> result = new ArrayList<>();
        for (Object obj : this) {
            try {
                result.add((T) JsonConverter.castTo(obj, object));
            } catch (Exception ignored) { } // ignore if can't parse
        }
        return result;
    }

    /**
     * parse current object to list
     * @param rawList list string to parse
     * @return new JsonList
     */
    private JsonList parseFromString(String rawList) {
        if (!isList(rawList)) {
            throw new RuntimeException("Not list object. " + rawList);
        }
        if (rawList.length() - 2 <= 0) {
            return this;
        }
        super.clear();

        int level = 0;
        boolean quoted = false;
        StringBuilder block = new StringBuilder();
        for (int i = 1; i < rawList.length(); i++) {
            char currentChar = rawList.charAt(i);
            char prevChar = rawList.charAt(i - 1);
            level += (!quoted && (currentChar == '{' || currentChar == '[')) ? 1 :
                    (!quoted && (currentChar == '}' || currentChar == ']')) ? -1 : 0;

            if (level == 0 || i == rawList.length() - 1) {
                if (currentChar == '\"' && prevChar != '\"') {
                    quoted = !quoted;
                    continue;
                } else if (!quoted && currentChar == ',' || i == rawList.length() - 1) {
                    String value = block.toString();
                    if (Json.isJson(value) && prevChar != '\"') {
                        super.add(new Json(value, this.parseTypes));
                    } else if (JsonList.isList(value) && prevChar != '\"') {
                        super.add(new JsonList(value, this.parseTypes));
                    } else {
                        if (this.parseTypes && prevChar != '\"') {
                            super.add(JsonConverter.autoParseToType(value));
                        } else {
                            super.add(value);
                        }
                    }
                    block.setLength(0);
                    continue;
                }
            }

            // Test condition
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
    public JsonList parseTypes(boolean parseTypes) {
        this.parseTypes = parseTypes;
        return this;
    }

    /**
     * change current list values
     * @param rawList new list string
     * @return parsed list
     */
    public JsonList setJsonString(String rawList) {
        return this.parseFromString(rawList);
    }

    /**
     * @param rawList list string
     * @return true if string can be parsed to list
     */
    public static boolean isList(String rawList) {
        return rawList != null && rawList.trim().startsWith("[") && rawList.endsWith("]");
    }

    @Override
    public String toString() {
        StringBuilder list = new StringBuilder("[");

        Iterator<Object> iterator = super.iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if ((!(value instanceof String) && this.parseTypes) || value instanceof JsonObject) {
                list.append(value);
            } else {
                list.append("\"").append(value).append("\"");
            }
            if (iterator.hasNext()) {
                list.append(", ");
            }
        }

        return list.append("]").toString();
    }

}

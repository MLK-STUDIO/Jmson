package net.mlk.jmson.json;

import net.mlk.jmson.Jmson;

import java.util.*;
import java.util.regex.Pattern;

public class Json implements JmsonObject{
    private static final Pattern pattern = Pattern.compile("(true)?(false)?([0-9]+[.]?[0-9]?)*");
    private final Map<String, Object> dict = new LinkedHashMap<>();
    private boolean parseTypes = true;

    public Json() { }

    public Json(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    public Json(String rawJson) {
        this.putAll(parseFromString(rawJson));
    }

    public Json(String rawJson, boolean parseTypes) {
        this.putAll(parseFromString(rawJson, parseTypes));
    }

    public Json(Map<String, Object> dict) {
        this.putAll(dict);
    }

    public Json(Map<String, Object> dict, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.putAll(dict);
    }

    public Json put(String key, Object value) {
        if (value == this) {
            this.dict.put(key, new Json(((Json) value).getAll()));
            return this;
        }

        this.dict.put(key, value);
        return this;
    }

    public Json putAll(Map<String, Object> dict) {
        dict.forEach(this::put);
        return this;
    }

    public Json putAll(Json dict) {
        dict.getAll().forEach(this::put);
        return this;
    }

    public Object get(String key) {
        return this.dict.get(key);
    }

    public Map<String, Object> getAll() {
        return this.dict;
    }

    public String getString(String key) {
        return String.valueOf(this.dict.get(key));
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

    public java.util.List<Json> getListWithJsons(String key) {
        java.util.List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(key)).getAll()) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    public java.util.List<JsonList> getListWithLists(String key) {
        java.util.List<JsonList> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(key)).getAll()) {
            if (obj instanceof JsonList) {
                result.add((JsonList) obj);
            }
        }
        return result;
    }

    public int size() {
        return this.dict.size();
    }

    public Set<String> keySet() {
        return this.dict.keySet();
    }

    public Collection<Object> values() {
        return this.dict.values();
    }

    public boolean containsKey(String key) {
        return this.dict.containsKey(key);
    }

    public boolean containsValue(String value) {
        return this.dict.containsKey(value);
    }

    public Json setParseTypes(boolean parseTypes) {
        this.parseTypes = parseTypes;
        return this;
    }

    public static Json parseFromString(String rawJson) {
        return parseFromString(rawJson, true);
    }

    public static Json parseFromString(String rawJson, boolean parseTypes) {
        Json json = new Json(parseTypes);
        if (rawJson == null) {
            return json;
        }
        StringBuilder builder = new StringBuilder();
        String toParse = rawJson.substring(1, rawJson.length() - 1);
        boolean quote = false;
        int level = 0;

        for (int i = 0; i < toParse.length(); i++) {
            char current_char = toParse.charAt(i);
            char prev_char = i == 0 ? '\0' : toParse.charAt(i - 1);
            quote = (current_char == '\"' && prev_char != '\\') != quote;

            level += !quote && (current_char == '[' || current_char == '{') ? 1 :
                    !quote && (current_char == ']' || current_char == '}') ? -1 : 0;
            builder.append(current_char);

            if (!quote && level == 0 && current_char == ',' || i == toParse.length() - 1) {
                String[] block = split(builder.toString());
                String key = fixElement(block[0]);
                String value = block.length == 2 ? fixElement(block[1]) : null;
                if (value != null) {
                    if (Jmson.isJson(value)) {
                        json.put(key, Json.parseFromString(value, parseTypes));
                    } else if (Jmson.isList(value)) {
                        json.put(key, JsonList.parseFromString(value, parseTypes));
                    } else {
                        json.put(key, value);
                    }
                }
                builder.setLength(0);
            }
        }
        return json;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");

        this.dict.forEach((k, v) -> {
            builder.append(String.format("\"%s\":" + checkType(v, this.parseTypes), k, v));
            boolean isLastKey = k == this.dict.keySet().toArray()[this.dict.size() - 1];
            if (!isLastKey) {
                builder.append(", ");
            }
        });
        return builder.append("}").toString();
    }

    static String fixElement(String element) {
        element = element.trim();
        if (element.startsWith("\"")) {
            element = element.substring(1);
        }
        if (element.endsWith(",")) {
            element = element.substring(0, element.length() - 1);
        }
        if (element.endsWith("\"")) {
            element = element.substring(0, element.length() - 1);
        }
        return element;
    }

    static String[] split(String str) {
        String[] result = { str };
        boolean quote = false;

        for (int i = 0; i < str.length(); i++) {
            char current_char = str.charAt(i);
            char prev_char = i == 0 ? '\0' : str.charAt(i - 1);
            quote = (current_char == '\"' && prev_char != '\\') != quote;

            if (!quote && current_char == ':') {
                result = new String[2];
                result[0] = str.substring(0, i);
                result[1] = str.substring(i + 1);
                break;
            }
        }
        return result;
    }

    static String checkType(Object element, boolean parse_types) {
        if (element == null) {
            return "\"%s\"";
        }

        boolean matches = pattern.matcher(element.toString()).matches();
        if (!element.toString().isEmpty() && (element instanceof Json || element instanceof JsonList || (parse_types && matches))) {
            return "%s";
        } else {
            return "\"%s\"";
        }
    }

}

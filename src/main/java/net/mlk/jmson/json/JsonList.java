package net.mlk.jmson.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

public class JsonList {
    private final java.util.List<Object> list = new ArrayList<>();
    private boolean parseTypes = true;

    public JsonList() { }

    public JsonList(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    public JsonList(java.util.List<Object> list) {
        this.addAll(list);
    }

    public JsonList add(Object value) {
        if (value == this) {
            this.list.add(new JsonList(((JsonList) value).getAll()));
            return this;
        }

        this.list.add(value);
        return this;
    }

    public JsonList addAll(java.util.List<Object> list) {
        list.forEach(this::add);
        return this;
    }

    public JsonList addAll(JsonList list) {
        this.addAll(list.getAll());
        return this;
    }

    public Object get(int index) {
        return this.list.get(index);
    }

    public <T> Object getByType(int index, Class<T> type) {
        if (type == byte.class) {
            return this.getByte(index);
        } else if (type == short.class) {
            return this.getShort(index);
        } else if (type == int.class) {
            return this.getInteger(index);
        } else if (type == long.class) {
            return this.getLong(index);
        } else if (type == char.class) {
            return this.getCharacter(index);
        }
        return type.cast(this.get(index));
    }

    public java.util.List<Object> getAll() {
        return this.list;
    }

    public String getString(int index) {
        return String.valueOf(this.list.get(index));
    }

    public char getCharacter(int index) {
        String value = this.getString(index);
        if (value.length() != 1) {
            return '\0';
        }
        return value.charAt(0);
    }

    public byte getByte(int index) {
        return (byte) this.getShort(index);
    }

    public short getShort(int index) {
        return (short) this.getInteger(index);
    }

    public int getInteger(int index) {
        return (int) this.getLong(index);
    }

    public long getLong(int index) {
        String value = this.getString(index);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    public float getFloat(int index) {
        return (float) this.getDouble(index);
    }

    public double getDouble(int index) {
        String value = this.getString(index);
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    public boolean getBoolean(int index) {
        return Boolean.parseBoolean(this.getString(index));
    }

    public JsonList getList(int index) {
        return (JsonList) this.get(index);
    }

    public Json getJson(int index) {
        return (Json) this.get(index);
    }

    public java.util.List<Json> getListWithJsons(int index) {
        java.util.List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(index)).getAll()) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    public java.util.List<Json> getListWithJsons() {
        java.util.List<Json> result = new ArrayList<>();
        for (Object obj : this.getAll()) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    public java.util.List<JsonList> getListWithLists(int index) {
        java.util.List<JsonList> result = new ArrayList<>();
        for (Object obj : ((JsonList)this.get(index)).getAll()) {
            if (obj instanceof JsonList) {
                result.add((JsonList) obj);
            }
        }
        return result;
    }

    public int size() {
        return this.list.size();
    }

    public boolean contains(Object value) {
        return this.list.contains(value);
    }

    public boolean containsAll(Collection<?> list) {
        return new HashSet<>(this.list).containsAll(list);
    }

    public static JsonList parseFromString(String rawList) {
        return parseFromString(rawList);
    }

    public static JsonList parseFromString(String rawList, boolean parseTypes) {
        JsonList list = new JsonList(parseTypes);
        StringBuilder builder = new StringBuilder();
        String toParse = rawList.substring(1, rawList.length() - 1);
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
                String value = Json.fixElement(builder.toString());
                if (Json.isDict(value)) {
                    list.add(Json.parseFromString(value, parseTypes));
                }
                else if (Json.isList(value)) {
                    list.add(JsonList.parseFromString(value, parseTypes));
                }
                else {
                    list.add(value);
                }
                builder.setLength(0);
            }
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");

        this.list.forEach((v) -> {
            builder.append(String.format(Json.checkType(v, this.parseTypes), v));
            boolean isLastValue = v == this.get(this.list.size() - 1);
            if (!isLastValue) {
                builder.append(", ");
            }
        });
        return builder.append("]").toString();
    }

}
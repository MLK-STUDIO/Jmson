package net.mlk.jmson;

import java.util.*;
import java.util.regex.Pattern;

public class Jmson {
    private final ArrayList<Object> list = new ArrayList<>();
    private final HashMap<Object, Object> dict = new LinkedHashMap<>();
    private boolean parseTypes = true;

    public Jmson() {

    }

    public Jmson(boolean parseTypes) {
        this.parseTypes = parseTypes;
    }

    public Jmson(String raw) {
        this(raw, true);
    }

    public Jmson(String raw, boolean parseTypes) {
        this.parseTypes = parseTypes;
        this.parser(raw);
    }

    private void parser(String raw) {
        StringBuilder builder = new StringBuilder();
        String toParse = raw.substring(1, raw.length() - 1);
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

                if (this.isDict(raw)) {
                    String[] block = this.split(builder.toString());
                    String key = this.fixElement(block[0]);
                    String value = block.length == 2 ? this.fixElement(block[1]) : null;

                    if (value != null) {
                        if (this.isDict(value) || this.isList(value)) {
                            this.dict.put(key, new Jmson(value, this.parseTypes));
                        } else {
                            this.dict.put(key, value);
                        }
                    }
                } else {
                    String value = this.fixElement(builder.toString());

                    if (this.isList(raw)) {
                        if (this.isDict(value) || this.isList(value)) {
                            this.list.add(new Jmson(value, this.parseTypes));
                        } else {
                            this.list.add(value);
                        }
                    }
                }

                builder.setLength(0);
            }
        }
    }

    public Object get(int index) {
        return this.list.get(index);
    }

    public Object get(Object key) {
        return this.dict.get(key);
    }

    public String getString(int index) {
        return String.valueOf(this.get(index));
    }

    public String getString(Object key) {
        return String.valueOf(this.get(key));
    }

    public byte getByte(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Byte.parseByte(result);
    }

    public byte getByte(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Byte.parseByte(result);
    }

    public short getShort(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Short.parseShort(result);
    }

    public short getShort(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Short.parseShort(result);
    }

    public int getInteger(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Integer.parseInt(result);
    }

    public int getInteger(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Integer.parseInt(result);
    }

    public long getLong(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Long.parseLong(result);
    }

    public long getLong(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Long.parseLong(result);
    }

    public float getFloat(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Float.parseFloat(result);
    }

    public float getFloat(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Float.parseFloat(result);
    }

    public double getDouble(int index) {
        String result = this.getString(index);
        if (result == null) {
            return 0;
        }
        return Double.parseDouble(result);
    }

    public double getDouble(Object key) {
        String result = this.getString(key);
        if (result == null) {
            return 0;
        }
        return Double.parseDouble(result);
    }

    public boolean getBoolean(int index) {
        return Boolean.parseBoolean(this.getString(index));
    }

    public boolean getBoolean(Object key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    public List<Object> getList() {
        return this.list;
    }

    public Map<Object, Object> getDict() {
        return this.dict;
    }

    public Jmson getJmsonPart(int index) {
        return (Jmson) this.list.get(index);
    }

    public Jmson getJmsonPart(Object key) {
        return (Jmson) this.dict.get(key);
    }

    public List<Jmson> getListOfDictionaries() {
        ArrayList<Jmson> result = new ArrayList<>();
        for (Object obj : this.list) {
            result.add((Jmson) obj);
        }
        return result;
    }

    public List<Jmson> getListOfDictionaries(Object key) {
        ArrayList<Jmson> result = new ArrayList<>();
        for (Object obj : ((Jmson) dict.get(key)).getList()) {
            result.add((Jmson) obj);
        }
        return result;
    }

    public Jmson put(Object value) {
        this.list.add(value);
        return this;
    }

    public Jmson putAll(List<Object> values) {
        this.list.addAll(values);
        return this;
    }

    public Jmson putAll(Jmson jmson) {
        this.list.addAll(jmson.getList());
        this.dict.putAll(jmson.getDict());
        return this;
    }

    public Jmson put(Object key, Object value) {
        this.dict.put(key, value);
        return this;
    }

    public Jmson putAll(Map<Object, Object> values) {
        this.dict.putAll(values);
        return this;
    }

    public Jmson remove(int index) {
        this.list.remove(index);
        return this;
    }

    public Jmson remove(Object key) {
        if (this.dict.size() == 0) {
            this.list.remove(key);
        } else {
            this.dict.remove(key);
        }
        return this;
    }

    public boolean containsKey(Object key) {
        return this.dict.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.dict.containsValue(value) || this.list.contains(value);
    }

    public int size() {
        if (this.dict.size() == 0) {
            return this.list.size();
        }
        else if (this.list.size() == 0) {
            return this.dict.size();
        }
        else {
            return 0;
        }
    }

    private boolean isList(String raw) {
        return raw.startsWith("[") && raw.endsWith("]");
    }

    private boolean isDict(String raw) {
        return raw.startsWith("{") && raw.endsWith("}");
    }

    private String[] split(String str) {
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
            }
        }
        return result;
    }

    private String fixElement(String element) {
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

    private String checkType(Object element, boolean parse_types) {
        String pattern = "(true)?(false)?([0-9]+[.]?[0-9]?)*";
        boolean matches = Pattern.matches(pattern, element.toString());
        if (!element.toString().equals("") && (element instanceof Jmson || (parse_types && matches))) {
            pattern = "%s";
        } else {
            pattern = "\"%s\"";
        }
        return pattern;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (this.dict.size() == 0 && this.list.size() == 0) {
            return "{}";
        }

        if (this.dict.size() == 0) {
            result.append("[");
            this.list.forEach(element -> {
                String pattern = this.checkType(element, this.parseTypes);
                result.append(String.format(pattern, element));
                if (element != this.list.get(this.list.size() - 1)) {
                    result.append(", ");
                }
            });
            result.append("]");
        }
        else {
            result.append("{");
            dict.forEach((key, value) -> {
                String pattern = this.checkType(key, this.parseTypes) + ":" + this.checkType(value, this.parseTypes);
                result.append(String.format(pattern, key, value));
                boolean isLastKey = key != this.dict.keySet().toArray()[this.dict.size() - 1];
                if (isLastKey) {
                    result.append(", ");
                }
            });
            result.append("}");
        }

        return result.toString();
    }

}

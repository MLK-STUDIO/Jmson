package net.mlk.jmson;

import java.util.HashMap;

public class JmsonDict extends JmsonObject {

    public JmsonDict() {
        super(true);
    }

    public JmsonDict(boolean parse_types) {
        super(parse_types);
    }

    public JmsonDict(String raw_json) {
        super(raw_json);
    }

    public JmsonDict(String raw_json, boolean parse_types) {
        super(raw_json, parse_types);
    }

    @Override
    protected void add_block(String block) {
        String[] values = block.split(":", 2);
        String key = fix_element(values[0]);
        String value = fix_element(values[1]);

        if (value.startsWith("{") && value.endsWith("}")) {
            this.dict.put(key, new JmsonDict(value, this.parse_types));
        }
        else if (value.startsWith("[") && value.endsWith("]")) {
            this.dict.put(key, new JmsonList(value, this.parse_types));
        }
        else {
            this.dict.put(key, value);
        }
    }
//t
    public JmsonDict remove(Object key) {
        this.dict.remove(key);
        return this;
    }

    public JmsonDict put(Object key, Object value) {
        if (value == this) {
            throw new RuntimeException("You can't put an element in itself.");
        }
        this.dict.put(key, value);
        return this;
    }

    public JmsonDict putAll(JmsonDict dict) {
        this.dict.putAll(dict.getAll());
        return this;
    }

    public HashMap<Object, Object> getAll() {
        return this.dict;
    }

    public Object get(Object key) {
        return this.dict.get(key);
    }

    public JmsonDict getDictionary(Object key) {
        return (JmsonDict) this.get(key);
    }

    public JmsonList getList(Object key) {
        return (JmsonList) this.get(key);
    }

    public String getString(Object key) {
        if (!dict.containsKey(key)) return null;
        return translate_unicode(String.valueOf(this.get(key)));
    }

    public boolean getBoolean(Object key) {
        return Boolean.parseBoolean(this.getString(key));
    }

    public int getInt(Object key) {
        return Integer.parseInt(this.getString(key));
    }

    public float getFloat(Object key) {
        return Float.parseFloat(this.getString(key));
    }

    public double getDouble(Object key) {
        return Double.parseDouble(this.getString(key));
    }

    public boolean containsKey(Object key) {
        return this.dict.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.dict.containsValue(value);
    }

    public int size() {
        return this.dict.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        dict.forEach((key, value) -> {
            String pattern = checkType(key, this.parse_types) + ":" + checkType(value, this.parse_types);
            builder.append(String.format(pattern, key, value));
            boolean isLastKey = key != this.dict.keySet().toArray()[this.dict.size() - 1];
            if (isLastKey) {
                builder.append(", ");
            }
        });
        return builder + "}";
    }
}


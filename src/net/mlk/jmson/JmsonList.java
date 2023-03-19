package net.mlk.jmson;

import java.util.ArrayList;
import java.util.List;

public class JmsonList extends JmsonObject {

    public JmsonList() {
        super(true);
    }

    public JmsonList(boolean parse_types) {
        super(parse_types);
    }

    public JmsonList(String raw_list) {
        super(raw_list);
    }

    public JmsonList(String raw_list, boolean parse_types) {
        super(raw_list, parse_types);
    }

    @Override
    protected void add_block(String raw) {
        String value = fix_element(raw);

        if (value.startsWith("{") && value.endsWith("}")) {
            this.list.add(new JmsonDict(value, this.parse_types));
        }
        else if (value.startsWith("[") && value.endsWith("]")) {
            this.list.add(new JmsonList(value, this.parse_types));
        }
        else {
            this.list.add(value);
        }
    }

    public JmsonList add(Object object) {
        if (object == this) {
            throw new RuntimeException("You can't add an element in itself.");
        }
        this.list.add(object);
        return this;
    }
    public JmsonList addAll(JmsonList list) {
        this.list.addAll(list.getAll());
        return this;
    }

    public Object get(int index) {
        return this.list.get(index);
    }

    public JmsonList remove(int index) {
        this.list.remove(index);
        return this;
    }

    public int getInt(int index) {
        return Integer.parseInt(this.getString(index));
    }

    public float getFloat(int index) {
        return Float.parseFloat(this.getString(index));
    }

    public Double getDouble(int index) {
        return Double.parseDouble(this.getString(index));
    }

    public String getString(int index) {
        return JmsonDict.translate_unicode((String) this.get(index));
    }

    public JmsonDict getJson(int index) {
        return (JmsonDict) this.get(index);
    }

    public JmsonList getList(int index) {
        return (JmsonList) this.get(index);
    }

    public List<Object> getAll() {
        return this.list;
    }

    public List<JmsonDict> getListOfDictionaries() {
        ArrayList<JmsonDict> result = new ArrayList<>();
        for (Object obj : this.list) {
            result.add((JmsonDict) obj);
        }
        return result;
    }

    public boolean contains(Object value) {
        return this.list.contains(value);
    }

    public int size() {
        return this.list.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        this.list.forEach(element -> {
            String pattern = checkType(element, this.parse_types);
            builder.append(String.format(pattern, element));
            if (element != this.list.get(this.list.size() - 1)) {
                builder.append(", ");
            }
        });
        return builder + "]";
    }
}

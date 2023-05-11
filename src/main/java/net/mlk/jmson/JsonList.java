package net.mlk.jmson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class JsonList extends CopyOnWriteArrayList<Object> implements JsonObject {
    private boolean parseTypes = true;

    /**
     * Default list object
     */
    public JsonList() {

    }

    /**
     * List object parsed from string
     * type parsing is true by default
     * @param rawListString string to parse
     */
    public JsonList(String rawListString) {
        this.parseFromString(this.validateString(Objects.requireNonNull(rawListString)));
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

    public String getString(int index) {
        return String.valueOf(super.get(index));
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
        return (JsonList) super.get(index);
    }

    public Json getJson(int index) {
        return (Json) super.get(index);
    }

    public List<Json> getListWithJsons(int index) {
        List<Json> result = new ArrayList<>();
        for (Object obj : ((JsonList)super.get(index))) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

    public List<Json> getListWithJsons() {
        List<Json> result = new ArrayList<>();
        for (Object obj : this) {
            if (obj instanceof Json) {
                result.add((Json) obj);
            }
        }
        return result;
    }

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
        Pattern pattern = Pattern.compile("(true)?(false)?([0-9]+[.]?[0-9]?)*");
        StringBuilder builder = new StringBuilder(super.size() * 16);
        builder.append("[");
        Iterator<Object> iterator = super.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj == null) {
                if (this.parseTypes) {
                    builder.append("null");
                } else {
                    builder.append("\"").append("null").append("\"");
                }
            } else {
                boolean matches = pattern.matcher(obj.toString()).matches();
                if (!obj.toString().isEmpty() && (obj instanceof Json || obj instanceof JsonList || (this.parseTypes && matches))) {
                    builder.append(obj);
                } else {
                    builder.append("\"").append(obj).append("\"");
                }
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Parse current list from string
     * @param rawListString string to parse
     */
    private void parseFromString(String rawListString) {
        if (rawListString.isEmpty()) {
            return;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(numThreads);

        int level = 0;
        int stringLength = rawListString.length();
        StringBuilder block = new StringBuilder();
        for (int i = 0; i <= stringLength; i++) {
            char currentChar = i == stringLength ? '\0' : rawListString.charAt(i);
            char prevChar = i == 0 ? '\0' : rawListString.charAt(i - 1);

            level += currentChar == '{' || currentChar == '[' ? 1 :
                    currentChar == '}' || currentChar == ']' ? -1 : 0;

            if ((level == 0 &&currentChar == ',') || i == stringLength) {
                String value = block.toString().trim();
                if (Json.isJson(value)) {
                    String finalValue1 = value;
                    service.execute(() -> super.add(new Json(finalValue1, this.parseTypes)));
                } else if (isList(value)) {
                    String finalValue = value;
                    service.execute(() -> super.add(new JsonList(finalValue, this.parseTypes)));
                } else {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (this.parseTypes) {
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

        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            service.shutdownNow();
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

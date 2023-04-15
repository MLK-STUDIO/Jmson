package net.mlk.jmson;
import java.util.regex.Pattern;

public abstract class JmsonObject {
    protected boolean parse_types = true;

    protected JmsonObject(boolean parse_types) {
        this.parse_types = parse_types;
    }

    protected JmsonObject(String raw) {
        raw = raw.substring(1, raw.length() - 1);
        this.parser(raw);
    }

    protected JmsonObject(String raw, boolean parse_types) {
        this.parse_types = parse_types;
        raw = raw.substring(1, raw.length() - 1);
        this.parser(raw);
    }

    private void parser(String raw) {
        StringBuilder builder = new StringBuilder();
        boolean quote = false;
        int level = 0;

        for (int i = 0; i < raw.length(); i++) {
            char current_char = raw.charAt(i);
            char prev_char = i == 0 ? ' ' : raw.charAt(i - 1);
            quote = (current_char == '\"' && prev_char != '\\') != quote;

            level += !quote && (current_char == '[' || current_char == '{') ? 1 :
                    !quote && (current_char == ']' || current_char == '}') ? -1 : 0;
            builder.append(current_char);

            if (!quote && level == 0 && current_char == ',' || i == raw.length() - 1) {
                this.add_block(builder.toString());
                builder.setLength(0);
            }
        }
    }

    protected void add_block(String block) {}

    protected static String fix_element(String element) {
        element = element.trim();
        if (element.startsWith("\"")) {
            element = element.substring(1);
        }
        if (element.endsWith(":")) {
            element = element.substring(0, element.length() - 1);
        }
        if (element.endsWith(",")) {
            element = element.substring(0, element.length() - 1);
        }
        if (element.endsWith("\"")) {
            element = element.substring(0, element.length() - 1);
        }
        return element;
    }

    protected static String checkType(Object element, boolean parse_types) {
        String pattern = "(true)?(false)?([0-9]+[.]?[0-9]?)*";
        boolean matches = Pattern.matches(pattern, element.toString());
        if (!element.toString().equals("") && (element instanceof JmsonList || element instanceof JmsonDict || (parse_types && matches))) {
            pattern = "%s";
        } else {
            pattern = "\"%s\"";
        }
        return pattern;
    }

    public static String translate_unicode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\') {
                c = s.charAt(i++);
                if (c == 'u') {
                    c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                    i += 4;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}

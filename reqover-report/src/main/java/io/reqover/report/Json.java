package io.reqover.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader and string escaper.
 *
 * <p>{@code reqover-report} deliberately has no dependency beyond
 * {@code reqover-core}, so that a report can be written and read back by the
 * CLI without dragging a JSON library onto the classpath of the application
 * being measured. This parser covers the whole JSON grammar but nothing more:
 * no comments, no trailing commas, no lenient modes.
 *
 * <p>Numbers become {@link Long} when they have no fraction or exponent and fit
 * in 64 bits, and {@link Double} otherwise.
 */
final class Json {
    private final String text;
    private int cursor;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Parses one JSON document.
     *
     * @throws IllegalArgumentException if the text is not a single well-formed
     * JSON value
     */
    static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("JSON text must not be null");
        }
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.cursor != text.length()) {
            throw parser.fail("trailing content after the top-level value");
        }
        return value;
    }

    private Object readValue() {
        if (cursor >= text.length()) {
            throw fail("unexpected end of input");
        }
        char c = text.charAt(cursor);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readKeyword("true", Boolean.TRUE);
            case 'f' -> readKeyword("false", Boolean.FALSE);
            case 'n' -> readKeyword("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> object = new LinkedHashMap<>();
        cursor++;
        skipWhitespace();
        if (peek() == '}') {
            cursor++;
            return object;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw fail("expected a field name");
            }
            String field = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw fail("expected ':' after field " + field);
            }
            cursor++;
            skipWhitespace();
            object.put(field, readValue());
            skipWhitespace();
            char next = peek();
            cursor++;
            if (next == '}') {
                return object;
            }
            if (next != ',') {
                throw fail("expected ',' or '}' in object");
            }
        }
    }

    private List<Object> readArray() {
        List<Object> array = new ArrayList<>();
        cursor++;
        skipWhitespace();
        if (peek() == ']') {
            cursor++;
            return array;
        }
        while (true) {
            skipWhitespace();
            array.add(readValue());
            skipWhitespace();
            char next = peek();
            cursor++;
            if (next == ']') {
                return array;
            }
            if (next != ',') {
                throw fail("expected ',' or ']' in array");
            }
        }
    }

    private String readString() {
        cursor++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (cursor >= text.length()) {
                throw fail("unterminated string");
            }
            char c = text.charAt(cursor++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                if (c < 0x20) {
                    throw fail("unescaped control character in string");
                }
                out.append(c);
                continue;
            }
            if (cursor >= text.length()) {
                throw fail("unterminated escape sequence");
            }
            char escape = text.charAt(cursor++);
            switch (escape) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (cursor + 4 > text.length()) {
                        throw fail("truncated \\u escape");
                    }
                    String hex = text.substring(cursor, cursor + 4);
                    try {
                        out.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException e) {
                        throw fail("invalid \\u escape: " + hex);
                    }
                    cursor += 4;
                }
                default -> throw fail("invalid escape character: \\" + escape);
            }
        }
    }

    private Object readNumber() {
        int start = cursor;
        if (peek() == '-') {
            cursor++;
        }
        boolean floating = false;
        while (cursor < text.length()) {
            char c = text.charAt(cursor);
            if (c >= '0' && c <= '9') {
                cursor++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                floating = true;
                cursor++;
            } else if ((c == '+' || c == '-') && isExponentMarker(text.charAt(cursor - 1))) {
                cursor++;
            } else {
                break;
            }
        }
        String literal = text.substring(start, cursor);
        if (literal.isEmpty() || "-".equals(literal)) {
            throw fail("expected a number");
        }
        try {
            return floating ? (Object) Double.valueOf(literal) : (Object) Long.valueOf(literal);
        } catch (NumberFormatException e) {
            try {
                return Double.valueOf(literal);
            } catch (NumberFormatException nested) {
                throw fail("invalid number: " + literal);
            }
        }
    }

    private static boolean isExponentMarker(char c) {
        return c == 'e' || c == 'E';
    }

    private Object readKeyword(String keyword, Object value) {
        if (!text.startsWith(keyword, cursor)) {
            throw fail("expected " + keyword);
        }
        cursor += keyword.length();
        return value;
    }

    private char peek() {
        if (cursor >= text.length()) {
            throw fail("unexpected end of input");
        }
        return text.charAt(cursor);
    }

    private void skipWhitespace() {
        while (cursor < text.length()) {
            char c = text.charAt(cursor);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                cursor++;
            } else {
                return;
            }
        }
    }

    private IllegalArgumentException fail(String message) {
        return new IllegalArgumentException("Malformed JSON at offset " + cursor + ": " + message);
    }

    // --- accessors used when mapping a parsed document onto records ---

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object node, String what) {
        if (node instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException(what + " must be a JSON object");
    }

    static List<Object> array(Object node, String what) {
        if (node instanceof List<?> list) {
            // Not List.copyOf: a stray JSON null would surface as a bare
            // NullPointerException instead of a message naming the field.
            return new ArrayList<>(list);
        }
        throw new IllegalArgumentException(what + " must be a JSON array");
    }

    /** An absent or null array field reads as empty, so older reports stay readable. */
    static List<Object> optionalArray(Map<String, Object> node, String field) {
        Object value = node.get(field);
        return value == null ? List.of() : array(value, field);
    }

    static String string(Map<String, Object> node, String field) {
        Object value = node.get(field);
        if (value instanceof String text) {
            return text;
        }
        throw new IllegalArgumentException("field '" + field + "' must be a JSON string");
    }

    static int integer(Map<String, Object> node, String field) {
        Object value = node.get(field);
        if (value instanceof Long number) {
            return Math.toIntExact(number);
        }
        if (value instanceof Double number && number == Math.floor(number)) {
            return (int) (double) number;
        }
        throw new IllegalArgumentException("field '" + field + "' must be a JSON integer");
    }

    static Integer nullableInteger(Map<String, Object> node, String field) {
        Object value = node.get(field);
        if (value == null) {
            return null;
        }
        return integer(node, field);
    }

    static List<String> strings(Map<String, Object> node, String field) {
        List<Object> values = optionalArray(node, field);
        List<String> out = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof String text)) {
                throw new IllegalArgumentException("field '" + field + "' must contain only JSON strings");
            }
            out.add(text);
        }
        return List.copyOf(out);
    }

    /** Appends {@code value} as a quoted, escaped JSON string. */
    static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}

package com.bank.util;

import java.util.*;

/**
 * Lightweight pure Core Java JSON Parser and Serializer.
 * Requires NO external dependencies (compatible with standard JDK).
 */
public class JsonUtil {

    // ==========================================
    // SERIALIZATION (Java Object -> JSON String)
    // ==========================================
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : arr) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==========================================
    // DESERIALIZATION (JSON String -> Java Object)
    // ==========================================
    public static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.isEmpty()) return null;
        JsonParser parser = new JsonParser(json);
        return parser.parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        Object parsed = parse(json);
        if (parsed instanceof List) {
            return (List<Object>) parsed;
        }
        return new ArrayList<>();
    }

    private static class JsonParser {
        private final String src;
        private int pos = 0;

        public JsonParser(String src) {
            this.src = src;
        }

        private void skipWhitespace() {
            while (pos < src.length() && (Character.isWhitespace(src.charAt(pos)) || src.charAt(pos) == '\0')) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        public Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;

            // Handle accidental escaped quotes (like \" from command-line bodies)
            if (pos + 1 < src.length() && src.charAt(pos) == '\\' && (src.charAt(pos + 1) == '"' || src.charAt(pos + 1) == '\'')) {
                pos++;
            }

            char c = src.charAt(pos);

            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"' || c == '\'') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();

            // Unquoted string fallback
            return parseUnquotedWord();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // consume '{'
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                if (peek() == '}') {
                    pos++;
                    break;
                }

                String key = parseKey();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') {
                    pos++; // consume ':'
                }
                Object value = parseValue();
                map.put(key, value);

                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++; // consume ','
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++; // consume '}'
                    break;
                }
            }
            return map;
        }

        private String parseKey() {
            skipWhitespace();
            if (pos < src.length() && (src.charAt(pos) == '"' || src.charAt(pos) == '\'')) {
                return parseString();
            }
            if (pos + 1 < src.length() && src.charAt(pos) == '\\' && (src.charAt(pos + 1) == '"' || src.charAt(pos + 1) == '\'')) {
                pos++;
                return parseString();
            }
            return parseUnquotedWord();
        }

        private String parseUnquotedWord() {
            skipWhitespace();
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ':' || c == ',' || c == '}' || c == ']' || Character.isWhitespace(c) || c == '\\') {
                    break;
                }
                pos++;
            }
            String s = src.substring(start, pos);
            if (pos < src.length() && src.charAt(pos) == '\\') pos++;
            return s;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // consume '['
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                skipWhitespace();
                if (peek() == ']') {
                    pos++;
                    break;
                }
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++; // consume ','
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++; // consume ']'
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            skipWhitespace();
            if (pos >= src.length()) return "";

            char quoteChar = src.charAt(pos);
            if (quoteChar != '"' && quoteChar != '\'') {
                return parseUnquotedWord();
            }
            pos++; // consume opening quote

            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == quoteChar) {
                    return sb.toString();
                }
                if (c == '\\' && pos < src.length()) {
                    char next = src.charAt(pos++);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\'': sb.append('\''); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default: sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            return null;
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
            }
            return null;
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == 'e' || src.charAt(pos) == 'E' || src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                if ((src.charAt(pos) == '+' || src.charAt(pos) == '-') && src.charAt(pos - 1) != 'e' && src.charAt(pos - 1) != 'E') {
                    break;
                }
                pos++;
            }
            String numStr = src.substring(start, pos);
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return Double.parseDouble(numStr);
            }
        }
    }

    public static void main(String[] args) {
        String test = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        Map<String, Object> map = parseObject(test);
        System.out.println("PARSED: " + map);
        System.out.println("Username: " + map.get("username"));
        System.out.println("Password: " + map.get("password"));
    }
}

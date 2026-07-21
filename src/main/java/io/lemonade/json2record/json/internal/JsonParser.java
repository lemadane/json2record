package io.lemonade.json2record.json.internal;

import io.lemonade.json2record.exceptions.JsonMappingException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clean internal JSON recursive-descent parser.
 */
public final class JsonParser {

    public sealed interface JsonNode {
        record JsonObjectNode(Map<String, JsonNode> members) implements JsonNode {}
        record JsonArrayNode(List<JsonNode> elements) implements JsonNode {}
        record JsonStringNode(String value) implements JsonNode {}
        record JsonNumberNode(String rawNumber, BigDecimal bigDecimalValue) implements JsonNode {}
        record JsonBooleanNode(boolean value) implements JsonNode {}
        record JsonNullNode() implements JsonNode {}
    }

    private final String json;
    private final int length;
    private int pos;

    private JsonParser(String json) {
        this.json = json;
        this.length = json.length();
        this.pos = 0;
    }

    public static JsonNode parse(String json) {
        if (json == null) {
            throw new NullPointerException("json must not be null");
        }
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        if (parser.pos >= parser.length) {
            throw new JsonMappingException("Empty JSON content");
        }
        JsonNode root = parser.parseValue("$");
        parser.skipWhitespace();
        if (parser.pos < parser.length) {
            throw new JsonMappingException("Trailing non-whitespace content after root JSON value at index " + parser.pos);
        }
        return root;
    }

    private JsonNode parseValue(String path) {
        skipWhitespace();
        if (pos >= length) {
            throw new JsonMappingException("Unexpected end of JSON input at " + path);
        }

        char c = json.charAt(pos);
        if (c == '{') {
            return parseObject(path);
        } else if (c == '[') {
            return parseArray(path);
        } else if (c == '"') {
            return parseString(path);
        } else if (c == 't' || c == 'f') {
            return parseBoolean(path);
        } else if (c == 'n') {
            return parseNull(path);
        } else if (c == '-' || (c >= '0' && c <= '9')) {
            return parseNumber(path);
        } else {
            throw new JsonMappingException("Unexpected character '" + c + "' at " + path);
        }
    }

    private JsonNode.JsonObjectNode parseObject(String path) {
        expect('{', path);
        skipWhitespace();
        Map<String, JsonNode> members = new LinkedHashMap<>();

        if (pos < length && json.charAt(pos) == '}') {
            pos++;
            return new JsonNode.JsonObjectNode(members);
        }

        while (pos < length) {
            skipWhitespace();
            if (pos >= length || json.charAt(pos) != '"') {
                throw new JsonMappingException("Expected JSON property key string at " + path);
            }

            String key = parseStringValue(path);
            String propPath = path + "." + key;

            if (members.containsKey(key)) {
                throw new JsonMappingException("Duplicate JSON key \"" + key + "\" encountered at " + propPath);
            }

            skipWhitespace();
            expect(':', propPath);
            JsonNode value = parseValue(propPath);
            members.put(key, value);

            skipWhitespace();
            if (pos < length && json.charAt(pos) == ',') {
                pos++;
            } else if (pos < length && json.charAt(pos) == '}') {
                pos++;
                return new JsonNode.JsonObjectNode(members);
            } else {
                throw new JsonMappingException("Expected ',' or '}' in object at " + path);
            }
        }

        throw new JsonMappingException("Unterminated JSON object at " + path);
    }

    private JsonNode.JsonArrayNode parseArray(String path) {
        expect('[', path);
        skipWhitespace();
        List<JsonNode> elements = new ArrayList<>();

        if (pos < length && json.charAt(pos) == ']') {
            pos++;
            return new JsonNode.JsonArrayNode(elements);
        }

        int index = 0;
        while (pos < length) {
            String elemPath = path + "[" + index + "]";
            JsonNode element = parseValue(elemPath);
            elements.add(element);
            index++;

            skipWhitespace();
            if (pos < length && json.charAt(pos) == ',') {
                pos++;
            } else if (pos < length && json.charAt(pos) == ']') {
                pos++;
                return new JsonNode.JsonArrayNode(elements);
            } else {
                throw new JsonMappingException("Expected ',' or ']' in array at " + path);
            }
        }

        throw new JsonMappingException("Unterminated JSON array at " + path);
    }

    private JsonNode.JsonStringNode parseString(String path) {
        return new JsonNode.JsonStringNode(parseStringValue(path));
    }

    private String parseStringValue(String path) {
        expect('"', path);
        StringBuilder sb = new StringBuilder();

        while (pos < length) {
            char c = json.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            } else if (c == '\\') {
                if (pos >= length) {
                    throw new JsonMappingException("Unterminated escape sequence in string at " + path);
                }
                char esc = json.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        int codePoint = parseHex4(path);
                        if (Character.isHighSurrogate((char) codePoint)) {
                            // Check for trailing low surrogate
                            if (pos + 5 < length && json.charAt(pos) == '\\' && json.charAt(pos + 1) == 'u') {
                                pos += 2;
                                int low = parseHex4(path);
                                if (Character.isLowSurrogate((char) low)) {
                                    sb.appendCodePoint(Character.toCodePoint((char) codePoint, (char) low));
                                } else {
                                    sb.append((char) codePoint);
                                    sb.append((char) low);
                                }
                            } else {
                                sb.append((char) codePoint);
                            }
                        } else {
                            sb.append((char) codePoint);
                        }
                    }
                    default -> throw new JsonMappingException("Invalid escape character '\\" + esc + "' at " + path);
                }
            } else if (c < 0x20) {
                throw new JsonMappingException("Unescaped control character 0x" + Integer.toHexString(c) + " in string at " + path);
            } else {
                sb.append(c);
            }
        }

        throw new JsonMappingException("Unterminated string at " + path);
    }

    private int parseHex4(String path) {
        if (pos + 4 > length) {
            throw new JsonMappingException("Invalid Unicode escape sequence at " + path);
        }
        String hex = json.substring(pos, pos + 4);
        pos += 4;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new JsonMappingException("Invalid hex in Unicode escape \\u" + hex + " at " + path, e);
        }
    }

    private JsonNode.JsonBooleanNode parseBoolean(String path) {
        if (json.startsWith("true", pos)) {
            pos += 4;
            return new JsonNode.JsonBooleanNode(true);
        } else if (json.startsWith("false", pos)) {
            pos += 5;
            return new JsonNode.JsonBooleanNode(false);
        }
        throw new JsonMappingException("Invalid boolean literal at " + path);
    }

    private JsonNode.JsonNullNode parseNull(String path) {
        if (json.startsWith("null", pos)) {
            pos += 4;
            return new JsonNode.JsonNullNode();
        }
        throw new JsonMappingException("Invalid null literal at " + path);
    }

    private JsonNode.JsonNumberNode parseNumber(String path) {
        int start = pos;
        if (json.charAt(pos) == '-') {
            pos++;
        }
        if (pos >= length) {
            throw new JsonMappingException("Invalid number at " + path);
        }
        if (json.charAt(pos) == '0') {
            pos++;
        } else if (json.charAt(pos) >= '1' && json.charAt(pos) <= '9') {
            while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
        } else {
            throw new JsonMappingException("Invalid number format at " + path);
        }

        if (pos < length && json.charAt(pos) == '.') {
            pos++;
            int fracStart = pos;
            while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
            if (pos == fracStart) {
                throw new JsonMappingException("Expected digits after decimal point at " + path);
            }
        }

        if (pos < length && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
            pos++;
            if (pos < length && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                pos++;
            }
            int expStart = pos;
            while (pos < length && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
            if (pos == expStart) {
                throw new JsonMappingException("Expected digits after exponent at " + path);
            }
        }

        String raw = json.substring(start, pos);
        try {
            BigDecimal bd = new BigDecimal(raw);
            return new JsonNode.JsonNumberNode(raw, bd);
        } catch (NumberFormatException e) {
            throw new JsonMappingException("Invalid numeric value \"" + raw + "\" at " + path, e);
        }
    }

    private void expect(char expected, String path) {
        if (pos >= length || json.charAt(pos) != expected) {
            throw new JsonMappingException("Expected '" + expected + "' at " + path);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < length) {
            char c = json.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }
}

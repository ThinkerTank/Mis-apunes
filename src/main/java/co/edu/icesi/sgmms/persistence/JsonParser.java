package co.edu.icesi.sgmms.persistence;

import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/**
 * Recursive-descent parser for JSON (RFC 8259) that produces {@link JsonValue} trees. Errors are
 * reported with the line and column where the text stopped being valid.
 *
 * <p>Complexity — time: O(n) for a text of n characters (each character is read once);
 * space: O(n) for the tree plus O(d) recursion for a nesting depth d.
 */
public final class JsonParser {

    private final String text;
    private int index;
    private int line = 1;
    private int column = 1;

    private JsonParser(String text) {
        this.text = text;
    }

    /**
     * Parses a complete JSON document.
     *
     * <p>Complexity — time: O(n); space: O(n).
     *
     * @throws ConfigurationException if the text is not valid JSON
     */
    public static JsonValue parse(String text) throws ConfigurationException {
        if (text == null) {
            throw new ConfigurationException("El texto JSON está vacío");
        }
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.index < text.length()) {
            throw parser.error("Hay contenido inesperado después del valor JSON");
        }
        return value;
    }

    private JsonValue parseValue() throws ConfigurationException {
        if (index >= text.length()) {
            throw error("El texto terminó antes de lo esperado");
        }
        char c = text.charAt(index);
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return JsonValue.string(parseString());
            case 't':
                expectWord("true");
                return JsonValue.bool(true);
            case 'f':
                expectWord("false");
                return JsonValue.bool(false);
            case 'n':
                expectWord("null");
                return JsonValue.nullValue();
            default:
                if (c == '-' || Character.isDigit(c)) {
                    return JsonValue.number(parseNumber());
                }
                throw error("Carácter inesperado '" + c + "'");
        }
    }

    private JsonValue parseObject() throws ConfigurationException {
        HashTable<String, JsonValue> members = new HashTable<>();
        LinkedList<String> order = new LinkedList<>();
        advance();
        skipWhitespace();
        if (peek() == '}') {
            advance();
            return JsonValue.object(members, order);
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Se esperaba el nombre de un campo entre comillas");
            }
            String key = parseString();
            if (members.containsKey(key)) {
                throw error("Campo repetido \"" + key + "\"");
            }
            skipWhitespace();
            expect(':');
            skipWhitespace();
            members.put(key, parseValue());
            order.addLast(key);
            skipWhitespace();
            if (peek() == ',') {
                advance();
            } else if (peek() == '}') {
                advance();
                return JsonValue.object(members, order);
            } else {
                throw error("Se esperaba ',' o '}' en el objeto");
            }
        }
    }

    private JsonValue parseArray() throws ConfigurationException {
        LinkedList<JsonValue> elements = new LinkedList<>();
        advance();
        skipWhitespace();
        if (peek() == ']') {
            advance();
            return JsonValue.array(elements);
        }
        while (true) {
            skipWhitespace();
            elements.addLast(parseValue());
            skipWhitespace();
            if (peek() == ',') {
                advance();
            } else if (peek() == ']') {
                advance();
                return JsonValue.array(elements);
            } else {
                throw error("Se esperaba ',' o ']' en el arreglo");
            }
        }
    }

    private String parseString() throws ConfigurationException {
        advance();
        StringBuilder builder = new StringBuilder();
        while (true) {
            if (index >= text.length()) {
                throw error("Texto sin cerrar");
            }
            char c = text.charAt(index);
            if (c == '"') {
                advance();
                return builder.toString();
            }
            if (c < 0x20) {
                throw error("Los caracteres de control deben escaparse dentro de un texto");
            }
            if (c == '\\') {
                advance();
                if (index >= text.length()) {
                    throw error("Secuencia de escape incompleta");
                }
                char escaped = text.charAt(index);
                switch (escaped) {
                    case '"': builder.append('"'); break;
                    case '\\': builder.append('\\'); break;
                    case '/': builder.append('/'); break;
                    case 'b': builder.append('\b'); break;
                    case 'f': builder.append('\f'); break;
                    case 'n': builder.append('\n'); break;
                    case 'r': builder.append('\r'); break;
                    case 't': builder.append('\t'); break;
                    case 'u':
                        if (index + 4 >= text.length()) {
                            throw error("Escape \\u incompleto");
                        }
                        String hex = text.substring(index + 1, index + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw error("Escape \\u inválido: " + hex);
                        }
                        for (int i = 0; i < 4; i++) {
                            advance();
                        }
                        break;
                    default:
                        throw error("Secuencia de escape inválida \\" + escaped);
                }
                advance();
            } else {
                builder.append(c);
                advance();
            }
        }
    }

    private double parseNumber() throws ConfigurationException {
        int start = index;
        if (peek() == '-') {
            advance();
        }
        if (!Character.isDigit(peek())) {
            throw error("Se esperaba un dígito");
        }
        while (Character.isDigit(peek())) {
            advance();
        }
        if (peek() == '.') {
            advance();
            if (!Character.isDigit(peek())) {
                throw error("Se esperaba un dígito después del punto decimal");
            }
            while (Character.isDigit(peek())) {
                advance();
            }
        }
        if (peek() == 'e' || peek() == 'E') {
            advance();
            if (peek() == '+' || peek() == '-') {
                advance();
            }
            if (!Character.isDigit(peek())) {
                throw error("Se esperaba un dígito en el exponente");
            }
            while (Character.isDigit(peek())) {
                advance();
            }
        }
        return Double.parseDouble(text.substring(start, index));
    }

    private void expectWord(String word) throws ConfigurationException {
        if (!text.startsWith(word, index)) {
            throw error("Se esperaba '" + word + "'");
        }
        for (int i = 0; i < word.length(); i++) {
            advance();
        }
    }

    private void expect(char c) throws ConfigurationException {
        if (peek() != c) {
            throw error("Se esperaba '" + c + "'");
        }
        advance();
    }

    private char peek() {
        return index < text.length() ? text.charAt(index) : '\0';
    }

    private void advance() {
        if (text.charAt(index) == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        index++;
    }

    private void skipWhitespace() {
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else {
                break;
            }
        }
    }

    private ConfigurationException error(String message) {
        return new ConfigurationException("JSON inválido en la línea " + line + ", columna " + column + ": " + message);
    }
}

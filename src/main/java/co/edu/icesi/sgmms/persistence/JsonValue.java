package co.edu.icesi.sgmms.persistence;

import co.edu.icesi.sgmms.model.structures.HashTable;
import co.edu.icesi.sgmms.model.structures.LinkedList;

/**
 * A parsed JSON value. Objects are stored in the project's {@link HashTable} (plus a list that
 * keeps the key order) and arrays in its {@link LinkedList}, so the parser does not depend on
 * Java collections either.
 */
public final class JsonValue {

    /** The six JSON value kinds. */
    public enum Kind { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    private final Kind kind;
    private final Object value;
    private final LinkedList<String> keys;

    private JsonValue(Kind kind, Object value, LinkedList<String> keys) {
        this.kind = kind;
        this.value = value;
        this.keys = keys;
    }

    static JsonValue object(HashTable<String, JsonValue> members, LinkedList<String> order) {
        return new JsonValue(Kind.OBJECT, members, order);
    }

    static JsonValue array(LinkedList<JsonValue> elements) {
        return new JsonValue(Kind.ARRAY, elements, null);
    }

    static JsonValue string(String text) {
        return new JsonValue(Kind.STRING, text, null);
    }

    static JsonValue number(double number) {
        return new JsonValue(Kind.NUMBER, number, null);
    }

    static JsonValue bool(boolean flag) {
        return new JsonValue(Kind.BOOLEAN, flag, null);
    }

    static JsonValue nullValue() {
        return new JsonValue(Kind.NULL, null, null);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isObject() {
        return kind == Kind.OBJECT;
    }

    public boolean isArray() {
        return kind == Kind.ARRAY;
    }

    public boolean isString() {
        return kind == Kind.STRING;
    }

    public boolean isNumber() {
        return kind == Kind.NUMBER;
    }

    /** Whether the value is a number without fractional part. */
    public boolean isInteger() {
        return kind == Kind.NUMBER && (Double) value == Math.rint((Double) value);
    }

    public boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    public boolean isNull() {
        return kind == Kind.NULL;
    }

    /** Member of an object, or {@code null} if absent. Complexity — average O(1). */
    @SuppressWarnings("unchecked")
    public JsonValue get(String key) {
        requireKind(Kind.OBJECT);
        return ((HashTable<String, JsonValue>) value).get(key);
    }

    /** Whether an object has the member. Complexity — average O(1). */
    @SuppressWarnings("unchecked")
    public boolean has(String key) {
        requireKind(Kind.OBJECT);
        return ((HashTable<String, JsonValue>) value).containsKey(key);
    }

    /** Member names of an object in file order. */
    public LinkedList<String> keys() {
        requireKind(Kind.OBJECT);
        return keys;
    }

    /** Elements of an array. */
    @SuppressWarnings("unchecked")
    public LinkedList<JsonValue> elements() {
        requireKind(Kind.ARRAY);
        return (LinkedList<JsonValue>) value;
    }

    public String asString() {
        requireKind(Kind.STRING);
        return (String) value;
    }

    public double asDouble() {
        requireKind(Kind.NUMBER);
        return (Double) value;
    }

    public int asInt() {
        requireKind(Kind.NUMBER);
        return (int) Math.round((Double) value);
    }

    public boolean asBoolean() {
        requireKind(Kind.BOOLEAN);
        return (Boolean) value;
    }

    private void requireKind(Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException("Expected a JSON " + expected + " but found " + kind);
        }
    }

    @Override
    public String toString() {
        return kind + (value == null ? "" : ":" + value);
    }
}

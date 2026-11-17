package co.edu.icesi.sgmms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonParserTest {

    @Test
    void parsesNestedObjectsAndArraysKeepingKeyOrder() throws Exception {
        JsonValue root = JsonParser.parse("{\"b\": [1, 2, {\"c\": true}], \"a\": {\"x\": null}}");

        assertTrue(root.isObject());
        assertEquals("[b, a]", root.keys().toString());
        assertEquals(3, root.get("b").elements().size());
        assertTrue(root.get("b").elements().get(2).get("c").asBoolean());
        assertTrue(root.get("a").get("x").isNull());
    }

    @Test
    void parsesEveryNumberFormat() throws Exception {
        JsonValue numbers = JsonParser.parse("[0, -7, 3.25, 1e3, 2.5E-1]");

        assertEquals(0, numbers.elements().get(0).asInt());
        assertEquals(-7, numbers.elements().get(1).asInt());
        assertEquals(3.25, numbers.elements().get(2).asDouble(), 1e-12);
        assertEquals(1000, numbers.elements().get(3).asDouble(), 1e-12);
        assertEquals(0.25, numbers.elements().get(4).asDouble(), 1e-12);
        assertTrue(numbers.elements().get(1).isInteger());
        assertFalse(numbers.elements().get(2).isInteger());
    }

    @Test
    void decodesEscapeSequencesInStrings() throws Exception {
        JsonValue text = JsonParser.parse("\"Cami\\u00f3n \\\"B-01\\\"\\nlínea\"");

        assertEquals("Camión \"B-01\"\nlínea", text.asString());
    }

    @Test
    void malformedDocumentsReportLineAndColumn() {
        ConfigurationException missingComma = assertThrows(ConfigurationException.class,
                () -> JsonParser.parse("{\n  \"a\": 1\n  \"b\": 2\n}"));
        assertTrue(missingComma.getMessage().contains("línea 3"), missingComma.getMessage());

        assertThrows(ConfigurationException.class, () -> JsonParser.parse("[1, 2"));
        assertThrows(ConfigurationException.class, () -> JsonParser.parse("{\"a\": tru}"));
        assertThrows(ConfigurationException.class, () -> JsonParser.parse("\"unterminated"));
        assertThrows(ConfigurationException.class, () -> JsonParser.parse("{} extra"));
    }

    @Test
    void duplicateMembersAreRejected() {
        assertThrows(ConfigurationException.class, () -> JsonParser.parse("{\"id\": 1, \"id\": 2}"));
    }
}

package tools.jackson.dataformat.javaprop;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.JsonToken;
import tools.jackson.core.io.SerializedString;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.javaprop.io.JPropWriteContext;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleStreamingTest extends ModuleTestBase
{
    private final ObjectMapper MAPPER = newPropertiesMapper();

    @Test
    public void testParsing() throws Exception
    {
        JsonParser p = MAPPER.createParser("foo = bar");
        Object src = p.streamReadInputSource();
        assertTrue(src instanceof Reader);
        _verifyGetNumberTypeFail(p, "null");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertNull(p.getEmbeddedObject());
        assertNotNull(p.currentLocation()); // N/A
        assertNotNull(p.currentTokenLocation()); // N/A
        _verifyGetNumberTypeFail(p, "START_OBJECT");
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        assertEquals("foo", p.currentName());
        assertEquals("foo", p.getString());
        assertEquals("foo", p.getValueAsString());
        assertEquals("foo", p.getValueAsString("x"));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        StringWriter sw = new StringWriter();
        assertEquals(3, p.getString(sw));
        assertEquals("bar", sw.toString());
        try {
            // try just one arbitrary one; all should fail similarly
            p.getLongValue();
            fail("Should not pass");
        } catch (StreamReadException e) {
            _verifyNonNumberTypeException(e, "VALUE_STRING");
        }

        p.close();
        assertTrue(p.isClosed());
        _verifyGetNumberTypeFail(p, "null");

        // and then some other accessors
        p = MAPPER.createParser("foo = bar");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("foo", p.nextName());
        assertEquals("bar", p.nextStringValue());
        assertNull(p.nextName());
        p.close();

        // one more thing, verify handling of non-binary
        p = MAPPER.createParser("foo = bar");
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
        try {
            p.getBinaryValue();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "cannot access as binary");
        }
        try {
            p.getDoubleValue();
            fail("Should not pass");
        } catch (StreamReadException e) {
            _verifyNonNumberTypeException(e, "PROPERTY_NAME");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertFalse(p.isClosed());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    public void testStreamingGeneration() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(strw);

        Object target = gen.streamWriteOutputTarget();
        assertTrue(target instanceof Writer);
        
        gen.writeStartObject();
        gen.writeBooleanProperty("flagTrue", true);
        gen.writeBooleanProperty("flagFalse", false);
        gen.writeNullProperty("null");
        gen.writeNumberProperty("long", 10L);
        gen.writeNumberProperty("int", 10);
        gen.writeNumberProperty("double", 0.25);
        gen.writeNumberProperty("float", 0.5f);
        gen.writeNumberProperty("decimal", BigDecimal.valueOf(0.125));
        gen.writeName(new SerializedString("bigInt"));
        gen.writeNumber(BigInteger.valueOf(123));
        gen.writeName("numString");
        gen.writeNumber("123.0");
        gen.writeName("charString");
        gen.writeString(new char[] { 'a', 'b', 'c' }, 1, 2);

        gen.writeName("arr");
        gen.writeStartArray();

        TokenStreamContext ctxt = gen.streamWriteContext();
        String path = ctxt.toString();
        assertTrue(ctxt instanceof JPropWriteContext);
        // Note: this context gives full path, unlike many others
        assertEquals("/arr/0", path);
        
        gen.writeEndArray();

        gen.writeEndObject();
        assertFalse(gen.isClosed());
        gen.flush();
        gen.close();

        String props = strw.toString();

        // Plus read back for fun
        Map<?,?> stuff = MAPPER.readValue(props, Map.class);
        assertEquals(11, stuff.size());
        assertEquals("10", stuff.get("long"));
    }

    @Test
    public void testStreamingGenerationRaw() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(strw);

        String COMMENT = "# comment!\n";
        gen.writeRaw(COMMENT);
        gen.writeRaw(new SerializedString(COMMENT));
        gen.writeRaw(COMMENT, 0, COMMENT.length());
        gen.writeRaw('#');
        gen.writeRaw('\n');

        gen.writeStartObject();
        gen.writeBooleanProperty("enabled", true);
        gen.writeEndObject();
        
        gen.close();

        assertEquals(COMMENT + COMMENT + COMMENT
                + "#\nenabled=true\n", strw.toString());

        // Plus read back for fun
        Map<?,?> stuff = MAPPER.readValue(strw.toString(), Map.class);
        assertEquals(1, stuff.size());
        assertEquals("true", stuff.get("enabled"));
    }        

    @Test
    public void testStreamingLongRaw() throws Exception
    {
        StringWriter strw = new StringWriter();
        JsonGenerator gen = MAPPER.createGenerator(strw);

        StringBuilder sb = new StringBuilder();
        sb.append("# ");
        for (int i = 0; i < 12000; ++i) {
            sb.append('a');
        }
        gen.writeRaw(sb.toString());
        gen.close();

        assertEquals(sb.toString(), strw.toString());
    }

    // In Jackson 3.x, non-Number token should return null for parser.getNumberType()
    // (no exception)
    private void _verifyGetNumberTypeFail(JsonParser p, String token) throws Exception
    {
        assertNull(p.getNumberType());
    }

    private void _verifyNonNumberTypeException(Exception e, String token) throws Exception
    {
        verifyException(e, "Current token ("+token+") not numeric, cannot use numeric");
    }
}

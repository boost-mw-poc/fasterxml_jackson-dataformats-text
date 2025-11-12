package com.fasterxml.jackson.dataformat.yaml.tofix;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.dataformat.yaml.*;
import com.fasterxml.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

//!!! 11-Nov-2025, tatu: Closed as "Not a Bug", test left for reference
public class SimpleGeneration366NotaBugTest extends ModuleTestBase
{
    // [dataformats-text#366]: multi-line literal block with trailing spaces does not work
    @JacksonTestFailureExpected
    @Test
    public void testLiteralBlockStyleMultilineWithTrailingSpace() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld ");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World ", yaml);
    }

    // [dataformats-text#366]: multi-line literal block without trailing spaces actually works
    @Test
    public void testLiteralBlockStyleMultiline() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

        YAMLMapper mapper = YAMLMapper.builder()
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .build();

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("text", "Hello\nWorld");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "text: |-\n  Hello\n  World", yaml);
    }
}

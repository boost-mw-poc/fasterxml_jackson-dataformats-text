package com.fasterxml.jackson.dataformat.csv.limits;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.fasterxml.jackson.core.StreamReadConstraints;

import com.fasterxml.jackson.databind.JsonMappingException;

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// Tests copied from databind "JDKNumberDeserTest" (only a small subset)
public class CSVLargeNumberReadLimitTest extends ModuleTestBase
{
    // [databind#2784]
    static class BigDecimalHolder2784 {
        public BigDecimal value;
    }

    static class NestedBigDecimalHolder2784 {
        @JsonUnwrapped
        public BigDecimalHolder2784 holder;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final CsvMapper MAPPER = newObjectMapper();

    @Test
    public void testVeryBigDecimalUnwrapped() throws Exception
    {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        CsvSchema schema = MAPPER.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n" + value + "\n";
        try {
            MAPPER.readerFor(NestedBigDecimalHolder2784.class)
                    .with(schema)
                    .readValue(DOC);
            fail("expected JsonMappingException");
        } catch (JsonMappingException e) {
            // ^^^ unfortunately StreamConstraintsException gets wrapped
            verifyException(e, "Number value length (1200) exceeds the maximum allowed");
        }
    }

    @Test
    public void testVeryBigDecimalUnwrappedWithNumLenUnlimited() throws Exception
    {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        final String value = sb.toString();
        CsvFactory factory = CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        CsvMapper mapper = CsvMapper.builder(factory).build();
        CsvSchema schema = mapper.schemaFor(NestedBigDecimalHolder2784.class).withHeader()
                .withStrictHeaders(true);
        final String DOC = "value\n" + value + "\n";
        NestedBigDecimalHolder2784 result = mapper.readerFor(NestedBigDecimalHolder2784.class)
                .with(schema)
                .readValue(DOC);
        assertEquals(new BigDecimal(value), result.holder.value);
    }
}

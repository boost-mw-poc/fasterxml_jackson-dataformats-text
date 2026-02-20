package tools.jackson.dataformat.csv.limits;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.dataformat.csv.CsvFactory;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.fail;

// Tests for StreamReadConstraints.maxDocumentLength() enforcement in CSV parser
public class CSVLargeDocReadLimitsTest extends ModuleTestBase
{
    private static final int DOC_LEN_LIMIT = 10_000;

    private CsvFactory csvFactoryWithDocLenLimit(long limit) {
        return CsvFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxDocumentLength(limit)
                        .build())
                .build();
    }

    @Test
    public void testDocumentExceedingLimitFails() throws Exception
    {
        final String doc = generateCsv(DOC_LEN_LIMIT + 5_000);
        CsvFactory f = csvFactoryWithDocLenLimit(DOC_LEN_LIMIT);
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), doc)) {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Document length");
            verifyException(e, "exceeds the maximum allowed (");
        }
    }

    @Test
    public void testDocumentWithinLimitSucceeds() throws Exception
    {
        final String doc = generateCsv(1_000);
        CsvFactory f = csvFactoryWithDocLenLimit(DOC_LEN_LIMIT);
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private String generateCsv(int targetLen) {
        // "value1,value2,value3\n" = 22 chars per row
        final String row = "value1,value2,value3\n";
        final StringBuilder sb = new StringBuilder(targetLen + row.length());
        while (sb.length() < targetLen) {
            sb.append(row);
        }
        return sb.toString();
    }
}

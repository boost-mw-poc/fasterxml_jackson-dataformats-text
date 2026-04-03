package tools.jackson.dataformat.yaml.tofix;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.*;
import tools.jackson.dataformat.yaml.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test for [dataformats-text#292]: YAML anchor/alias with
 * base type + builder causes UnresolvedForwardReference.
 *<p>
 * NOTE: this is actually a jackson-databind issue (same failure with JSON),
 * not YAML-specific. Object identity does not work when combining
 * base type + {@code @JsonDeserialize(builder=...)} + polymorphism.
 */
public class ObjectAndTypeId292Test extends ModuleTestBase
{
    // Interface base type + builder
    static class ContainerWithBuilder {
        @JsonProperty
        public List<BaseWithBuilder> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedWithBuilder", value = DerivedWithBuilder.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    interface BaseWithBuilder {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedWithBuilder.DerivedBuilder.class)
    static class DerivedWithBuilder implements BaseWithBuilder {
        @JsonProperty
        public String a;

        DerivedWithBuilder(String a) {
            this.a = a;
        }

        @tools.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
        public static class DerivedBuilder {
            private String a;

            @JsonProperty
            public DerivedBuilder a(String a) {
                this.a = a;
                return this;
            }

            public DerivedWithBuilder build() {
                return new DerivedWithBuilder(this.a);
            }
        }
    }

    // Class base type + builder
    static class ContainerClassWithBuilder {
        @JsonProperty
        public List<BaseClassWithBuilder> list;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(name = "DerivedClassBuilder", value = DerivedClassWithBuilder.class)})
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class,
            resolver = SimpleObjectIdResolver.class)
    static class BaseClassWithBuilder {
    }

    @tools.jackson.databind.annotation.JsonDeserialize(builder = DerivedClassWithBuilder.DerivedBuilder.class)
    static class DerivedClassWithBuilder extends BaseClassWithBuilder {
        @JsonProperty
        public String a;

        DerivedClassWithBuilder(String a) {
            this.a = a;
        }

        @tools.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
        public static class DerivedBuilder {
            private String a;

            @JsonProperty
            public DerivedBuilder a(String a) {
                this.a = a;
                return this;
            }

            public DerivedClassWithBuilder build() {
                return new DerivedClassWithBuilder(this.a);
            }
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // [dataformats-text#292]: interface base type + builder
    @JacksonTestFailureExpected
    @Test
    public void testAliasWithInterfaceAndBuilder() throws Exception
    {
        String yaml = "list:\n"
                + "    - !DerivedWithBuilder &id1\n"
                + "        a: foo\n"
                + "    - *id1\n";
        ContainerWithBuilder container = MAPPER.readValue(yaml, ContainerWithBuilder.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseWithBuilder first = container.list.get(0);
        assertEquals(DerivedWithBuilder.class, first.getClass());
        assertEquals("foo", ((DerivedWithBuilder) first).a);

        BaseWithBuilder second = container.list.get(1);
        assertSame(first, second);
    }

    // [dataformats-text#292]: class base type + builder
    @JacksonTestFailureExpected
    @Test
    public void testAliasWithClassAndBuilder() throws Exception
    {
        String yaml = "list:\n"
                + "    - !DerivedClassBuilder &id1\n"
                + "        a: foo\n"
                + "    - *id1\n";
        ContainerClassWithBuilder container = MAPPER.readValue(yaml, ContainerClassWithBuilder.class);
        assertNotNull(container);
        assertNotNull(container.list);
        assertEquals(2, container.list.size());

        BaseClassWithBuilder first = container.list.get(0);
        assertEquals(DerivedClassWithBuilder.class, first.getClass());
        assertEquals("foo", ((DerivedClassWithBuilder) first).a);

        BaseClassWithBuilder second = container.list.get(1);
        assertSame(first, second);
    }
}

package tools.jackson.dataformat.yaml.misc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// [dataformats-text#22] YAML anchors don't seem to work with @JsonCreator
public class ObjectIdWithCreator22Test extends ModuleTestBase
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class Student {
        private String name;
        private int age;

        @JsonCreator
        public Student(@JsonProperty("age") int age,
                       @JsonProperty("name") String name) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    // Same as Student but with no-arg constructor (works fine)
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class StudentNoCreator {
        public String name;
        public int age;
    }

    // More complex: with @JsonCreator and circular references
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
    @JsonPropertyOrder({ "name", "next" })
    static class NodeWithCreator {
        public String name;
        public NodeWithCreator next;

        @JsonCreator
        public NodeWithCreator(@JsonProperty("name") String name,
                               @JsonProperty("next") NodeWithCreator next) {
            this.name = name;
            this.next = next;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    @Test
    public void testNativeAnchorWithJsonCreator() throws Exception
    {
        // The YAML from the issue: anchor on the mapping object
        String yaml = "---\n"
                + "&1 name: \"billy\"\n"
                + "age: 5\n";

        Student student = MAPPER.readValue(yaml, Student.class);
        assertNotNull(student);
        assertEquals("billy", student.getName());
        assertEquals(5, student.getAge());
    }

    @Test
    public void testNonNativeIdWithJsonCreator() throws Exception
    {
        // Workaround from the issue: explicit @id property
        String yaml = "---\n"
                + "'@id': 1\n"
                + "name: \"billy\"\n"
                + "age: 5\n";

        Student student = MAPPER.readValue(yaml, Student.class);
        assertNotNull(student);
        assertEquals("billy", student.getName());
        assertEquals(5, student.getAge());
    }

    @Test
    public void testNativeAnchorWithNoArgConstructor() throws Exception
    {
        // Same YAML but with no-arg constructor class
        String yaml = "---\n"
                + "&1 name: \"billy\"\n"
                + "age: 5\n";

        StudentNoCreator student = MAPPER.readValue(yaml, StudentNoCreator.class);
        assertNotNull(student);
        assertEquals("billy", student.name);
        assertEquals(5, student.age);
    }

    @Test
    public void testCircularRefWithJsonCreator() throws Exception
    {
        String yaml = "---\n"
                + "&1 name: \"first\"\n"
                + "next:\n"
                + "  &2 name: \"second\"\n"
                + "  next: *1\n";

        NodeWithCreator first = MAPPER.readValue(yaml, NodeWithCreator.class);
        assertNotNull(first);
        assertEquals("first", first.name);
        assertNotNull(first.next);
        assertEquals("second", first.next.name);
        assertNotNull(first.next.next);
        assertSame(first, first.next.next);
    }
}

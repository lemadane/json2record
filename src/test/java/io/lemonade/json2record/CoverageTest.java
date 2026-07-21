package io.lemonade.json2record;

import io.lemonade.json2record.convert.DefaultValueProvider;
import io.lemonade.json2record.convert.TypeConverter;
import io.lemonade.json2record.json.JSON;
import io.lemonade.json2record.json.internal.JsonParser;
import io.lemonade.json2record.json.internal.JsonRecordReader;
import io.lemonade.json2record.json.internal.JsonRecordWriter;
import io.lemonade.json2record.reflect.RecordFactory;
import io.lemonade.json2record.reflect.RecordIntrospector;
import io.lemonade.json2record.reflect.RecordMetadata;
import io.lemonade.json2record.xml.XML;
import io.lemonade.json2record.xml.internal.XmlRecordReader;
import io.lemonade.json2record.xml.internal.XmlRecordWriter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoverageTest {

    public enum DummyEnum { A, B }

    public record TestOpt(Optional<String> optStr, Optional<Integer> optInt) {}
    public record TestPrimitives(byte b, short s, int i, long l, float f, double d, char c, boolean bool) {}
    public record TestBoxed(Byte b, Short s, Integer i, Long l, Float f, Double d, Character c, Boolean bool, BigInteger bi, BigDecimal bd, DummyEnum de) {}
    public record Nested(String name) {}
    public record RootWithNestedList(List<Nested> items) {}
    public record RootWithPrimList(List<Integer> numbers) {}
    public record XmlStaticTest(String val) {
        public static String attr1;
        public static String attr2 = null;
    }

    @Test
    void testExceptionCauseConstructors() {
        Throwable cause = new RuntimeException("cause");
        assertThat(new MissingDataException("msg", cause)).hasCause(cause);
        assertThat(new ExcessDataException("msg", cause)).hasCause(cause);
        assertThat(new TypeConversionException("msg", cause)).hasCause(cause);
        assertThat(new JsonMappingException("msg", cause)).hasCause(cause);
        assertThat(new XmlMappingException("msg", cause)).hasCause(cause);
        assertThat(new DataMappingException("msg", cause)).hasCause(cause);
        assertThat(new NameEncodingException("msg", cause)).hasCause(cause);
        assertThat(new RecordConstructionException("msg", cause)).hasCause(cause);

        // Non-record inspect test
        assertThatThrownBy(() -> RecordIntrospector.inspect(OrdinaryClass.class))
                .isInstanceOf(RecordConstructionException.class);
    }

    public static class OrdinaryClass {}

    @Test
    void testTypeConverterExhaustive() {
        assertThat(TypeConverter.convert(String.class, "hello", "path")).isEqualTo("hello");
        assertThat(TypeConverter.convert(String.class, 123, "path")).isEqualTo("123");

        // Numbers to primitives/wrappers
        assertThat(TypeConverter.convert(byte.class, (byte) 5, "path")).isEqualTo((byte) 5);
        assertThat(TypeConverter.convert(Byte.class, "10", "path")).isEqualTo((byte) 10);
        assertThat(TypeConverter.convert(short.class, (short) 100, "path")).isEqualTo((short) 100);
        assertThat(TypeConverter.convert(Short.class, "200", "path")).isEqualTo((short) 200);
        assertThat(TypeConverter.convert(int.class, 1000, "path")).isEqualTo(1000);
        assertThat(TypeConverter.convert(Integer.class, "2000", "path")).isEqualTo(2000);
        assertThat(TypeConverter.convert(long.class, 10000L, "path")).isEqualTo(10000L);
        assertThat(TypeConverter.convert(Long.class, "20000", "path")).isEqualTo(20000L);
        assertThat(TypeConverter.convert(float.class, 1.5f, "path")).isEqualTo(1.5f);
        assertThat(TypeConverter.convert(Float.class, "2.5", "path")).isEqualTo(2.5f);
        assertThat(TypeConverter.convert(double.class, 10.5d, "path")).isEqualTo(10.5d);
        assertThat(TypeConverter.convert(Double.class, "20.5", "path")).isEqualTo(20.5d);
        assertThat(TypeConverter.convert(boolean.class, true, "path")).isEqualTo(true);
        assertThat(TypeConverter.convert(Boolean.class, "FALSE", "path")).isEqualTo(false);
        assertThat(TypeConverter.convert(char.class, 'X', "path")).isEqualTo('X');
        assertThat(TypeConverter.convert(Character.class, "Y", "path")).isEqualTo('Y');
        assertThat(TypeConverter.convert(BigInteger.class, "12345", "path")).isEqualTo(new BigInteger("12345"));
        assertThat(TypeConverter.convert(BigDecimal.class, "123.45", "path")).isEqualTo(new BigDecimal("123.45"));
        assertThat(TypeConverter.convert(DummyEnum.class, "B", "path")).isEqualTo(DummyEnum.B);

        // Conversion errors
        assertThatThrownBy(() -> TypeConverter.convert(byte.class, null, "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(boolean.class, "notbool", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(byte.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(short.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(int.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(long.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(float.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(double.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(char.class, "multi", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(BigInteger.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(BigDecimal.class, "abc", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(DummyEnum.class, "C", "path")).isInstanceOf(TypeConversionException.class);
        assertThatThrownBy(() -> TypeConverter.convert(java.util.Date.class, "val", "path")).isInstanceOf(TypeConversionException.class);
    }

    @Test
    void testDefaultValueProviderExhaustive() {
        RecordMetadata meta = RecordIntrospector.inspect(TestPrimitives.class);
        for (RecordMetadata.ComponentMetadata comp : meta.components()) {
            Object def = DefaultValueProvider.getDefaultValue(comp);
            assertThat(def).isNotNull();
        }
    }

    @Test
    void testJsonParserEscapesAndNumbers() {
        String jsonStr = "{\"msg\":\"slash \\/ backslash \\\\ quote \\\" b \\b f \\f n \\n r \\r t \\t\", \"n1\": -10.5, \"n2\": 1e5, \"n3\": 2.5E-2}";
        record MsgRec(String msg, double n1, double n2, double n3) {}

        MsgRec rec = JSON.parse(MsgRec.class, jsonStr);
        assertThat(rec.msg()).contains("/");
        assertThat(rec.n1()).isEqualTo(-10.5);
        assertThat(rec.n2()).isEqualTo(100000.0);

        assertThatThrownBy(() -> JsonParser.parse(""))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JsonParser.parse("   "))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JsonParser.parse("{ \"a\" "))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JsonParser.parse("[ 1, "))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JsonParser.parse("{ \"a\": 1, }"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JsonParser.parse("undefined"))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void testJsonRecordWriterEdgeCases() {
        TestOpt optEmpty = new TestOpt(Optional.empty(), Optional.of(42));
        String jsonOpt = JSON.stringify(optEmpty);
        assertThat(jsonOpt).contains("\"optStr\":null");
        assertThat(jsonOpt).contains("\"optInt\":42");

        TestBoxed boxed = new TestBoxed((byte) 1, (short) 2, 3, 4L, 5.5f, 6.6d, 'Z', true, BigInteger.TEN, BigDecimal.ONE, DummyEnum.A);
        String jsonBoxed = JSON.stringify(boxed);
        assertThat(jsonBoxed).contains("\"de\":\"A\"");

        // Float / Double Infinity & NaN
        record FloatRec(float f) {}
        assertThatThrownBy(() -> JSON.stringify(new FloatRec(Float.POSITIVE_INFINITY)))
                .isInstanceOf(JsonMappingException.class);

        record DoubleRec(double d) {}
        assertThatThrownBy(() -> JSON.stringify(new DoubleRec(Double.NEGATIVE_INFINITY)))
                .isInstanceOf(JsonMappingException.class);

        // Control char escaping in writer
        record ControlRec(String text) {}
        ControlRec ctrl = new ControlRec("line1\nline2\t\u0001");
        String jsonCtrl = JSON.stringify(ctrl);
        assertThat(jsonCtrl).contains("\\n");
        assertThat(jsonCtrl).contains("\\t");
        assertThat(jsonCtrl).contains("\\u0001");
    }

    @Test
    void testJsonRecordReaderEdgeCases() {
        record PrimRec(int count) {}
        assertThatThrownBy(() -> JSON.parse(PrimRec.class, "{\"count\": null}"))
                .isInstanceOf(TypeConversionException.class);

        String jsonListNested = "{\"items\":[{\"name\":\"A\"},{\"name\":\"B\"}]}";
        RootWithNestedList rootList = JSON.parse(RootWithNestedList.class, jsonListNested);
        assertThat(rootList.items()).hasSize(2);
        assertThat(rootList.items().get(0).name()).isEqualTo("A");

        String jsonPrimList = "{\"numbers\":[1, 2, 3]}";
        RootWithPrimList rootPrimList = JSON.parse(RootWithPrimList.class, jsonPrimList);
        assertThat(rootPrimList.numbers()).containsExactly(1, 2, 3);
    }

    @Test
    void testXmlWriterAndReaderEdgeCases() {
        TestOpt optRec = new TestOpt(Optional.empty(), Optional.of(99));
        String xmlOpt = XML.stringify(optRec);
        assertThat(xmlOpt).contains("<optInt>99</optInt>");
        assertThat(xmlOpt).doesNotContain("optStr");

        XmlStaticTest.attr1 = "A1";
        XmlStaticTest.attr2 = null;
        XmlStaticTest staticRec = new XmlStaticTest("test");
        String xmlStatic = XML.stringify(staticRec);
        assertThat(xmlStatic).contains("attr1=\"A1\"");

        // XML empty elements mapping
        record EmptyRecordTest(String str, Optional<String> opt, TestOpt record) {}
        String xmlEmpty = "<EmptyRecordTest><str/><opt/><record/></EmptyRecordTest>";
        EmptyRecordTest parsedEmpty = XML.partialParse(EmptyRecordTest.class, xmlEmpty);
        assertThat(parsedEmpty.str()).isEqualTo("");
        assertThat(parsedEmpty.opt()).isEmpty();
        assertThat(parsedEmpty.record()).isNotNull();
    }

    @Test
    void testRecordMetadataMethods() {
        RecordMetadata meta = RecordIntrospector.inspect(TestOpt.class);
        assertThat(meta.recordSimpleName()).isEqualTo("TestOpt");
        assertThat(meta.decodedRecordName()).isEqualTo("TestOpt");
        assertThat(meta.canonicalConstructor()).isNotNull();
        assertThat(meta.components()).hasSize(2);
        assertThat(meta.findComponentByDecodedName("optStr")).isNotNull();
        assertThat(meta.findComponentByRawName("optStr")).isNotNull();
        assertThat(meta.findComponentByRawName("nonExistent")).isNull();

        RecordMetadata.ComponentMetadata comp = meta.components().get(0);
        assertThat(comp.name()).isEqualTo("optStr");
        assertThat(comp.decodedName()).isEqualTo("optStr");
        assertThat(comp.type()).isEqualTo(Optional.class);
        assertThat(comp.genericType()).isNotNull();
        assertThat(comp.accessor()).isNotNull();
        assertThat(comp.isList()).isFalse();
        assertThat(comp.listElementType()).isNull();
        assertThat(comp.isOptional()).isTrue();
        assertThat(comp.optionalValueType()).isEqualTo(String.class);
        assertThat(comp.isNestedRecord()).isFalse();
    }

    @Test
    void testJsonRecordReaderNonObjectRootsAndTypes() {
        assertThatThrownBy(() -> JSON.parse(Nested.class, "123"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("JSON number");

        assertThatThrownBy(() -> JSON.parse(Nested.class, "true"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("JSON boolean");

        assertThatThrownBy(() -> JSON.parse(Nested.class, "[\"a\"]"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("JSON array");

        assertThatThrownBy(() -> JSON.parse(Nested.class, "\"str\""))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("JSON string");

        assertThatThrownBy(() -> JSON.parse(Nested.class, "null"))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("JSON null");

        record ListReq(List<String> items) {}
        assertThatThrownBy(() -> JSON.parse(ListReq.class, "{\"items\": \"not_an_array\"}"))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageContaining("A JSON array does not match");

        record NestedReq(Nested item) {}
        assertThatThrownBy(() -> JSON.parse(NestedReq.class, "{\"item\": 123}"))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageContaining("Expected JSON object for nested record");
    }

    @Test
    void testJsonRecordWriterCollectionCycleAndEscaping() {
        record ListCycle(List<Object> items) {}
        List<Object> cycleList = new ArrayList<>();
        cycleList.add(cycleList);
        ListCycle cycleRec = new ListCycle(cycleList);

        assertThatThrownBy(() -> JSON.stringify(cycleRec))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Cycle detected");

        record StringEscapes(String text) {}
        StringEscapes esc = new StringEscapes("a\"b\\c/d\be\ff\ng\rh\ti\u0005");
        String jsonStr = JSON.stringify(esc);
        assertThat(jsonStr).contains("\\\"").contains("\\\\").contains("\\b").contains("\\f").contains("\\n").contains("\\r").contains("\\t").contains("\\u0005");
    }

    @Test
    void testXmlReaderEmptyPrimitiveInPartialMode() {
        String xml = "<TestPrimitives><b/></TestPrimitives>";
        assertThatThrownBy(() -> XML.partialParse(TestPrimitives.class, xml))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageContaining("Empty XML element <b> cannot be mapped to primitive type byte");
    }

    @Test
    void testJsonParserSyntaxErrorsExhaustive() {
        record Dummy(String a, boolean b, String c, double d) {}

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"a\": \"unclosed}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"a\": \"\\k\"}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"a\": \"\n\"}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"a\": \"\\uGGGG\"}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"b\": truX}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"b\": falX}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"c\": nulX}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"d\": -}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"d\": 1.}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"d\": 1e}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{\"a\": 1 \"b\": 2}"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "{ \"a\" }"))
                .isInstanceOf(JsonMappingException.class);

        assertThatThrownBy(() -> JSON.parse(Dummy.class, "!"))
                .isInstanceOf(JsonMappingException.class);
    }
}

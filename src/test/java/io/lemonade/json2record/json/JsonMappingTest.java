package io.lemonade.json2record.json;

import io.lemonade.json2record.exceptions.ExcessDataException;
import io.lemonade.json2record.JSON;
import io.lemonade.json2record.exceptions.JsonMappingException;
import io.lemonade.json2record.exceptions.MissingDataException;
import io.lemonade.json2record.exceptions.TypeConversionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonMappingTest {

    enum Status { ACTIVE, INACTIVE }

    public record VanLoadScan(
            String facilityID,
            String scanType
    ) {}

    public record FacilityInboundEvent(
            int eventMessageCount,
            List<VanLoadScan> vanLoadScans
    ) {}

    public record ScalarTypesRecord(
            String text,
            boolean flag,
            byte b,
            short s,
            int i,
            long l,
            float f,
            double d,
            char c,
            BigInteger bigInt,
            BigDecimal bigDec,
            Status status,
            Optional<String> optVal,
            String nullVal
    ) {}

    public record IgnoreStaticRecord(String name) {
        private static String version = "1.0";

        public static String version() {
            return version;
        }
    }

    public record SpecialCharRecord(
            String Facility$hyphen$ID,
            String Facility$dot$ID,
            String display$space$name,
            String price$dollar$value
    ) {}

    public record PartialJsonRecord(
            String id,
            int count,
            Optional<String> comment,
            List<String> items
    ) {}

    // --- Tests ---

    @DisplayName("Strict Parse Flat And Nested Record")
    @Test
    void testStrictParseFlatAndNestedRecord() {
        String json = """
                {
                  "eventMessageCount": 2,
                  "vanLoadScans": [
                    {
                      "facilityID": "0603",
                      "scanType": "V3"
                    },
                    {
                      "facilityID": "0417",
                      "scanType": "I3"
                    }
                  ]
                }
                """;

        FacilityInboundEvent event = JSON.parse(FacilityInboundEvent.class, json);
        assertThat(event.eventMessageCount()).isEqualTo(2);
        assertThat(event.vanLoadScans()).hasSize(2);
        assertThat(event.vanLoadScans().get(0).facilityID()).isEqualTo("0603");
        assertThat(event.vanLoadScans().get(1).facilityID()).isEqualTo("0417");
    }

    @DisplayName("Scalar Types And Precision")
    @Test
    void testScalarTypesAndPrecision() {
        String json = """
                {
                  "text": "Hello",
                  "flag": true,
                  "b": 12,
                  "s": 1234,
                  "i": 123456,
                  "l": 9876543210,
                  "f": 1.23,
                  "d": 4.56789,
                  "c": "A",
                  "bigInt": 123456789012345678901234567890,
                  "bigDec": 123456789012345678901234567890.123456789,
                  "status": "ACTIVE",
                  "optVal": "Present",
                  "nullVal": null
                }
                """;

        ScalarTypesRecord rec = JSON.parse(ScalarTypesRecord.class, json);
        assertThat(rec.text()).isEqualTo("Hello");
        assertThat(rec.flag()).isTrue();
        assertThat(rec.b()).isEqualTo((byte) 12);
        assertThat(rec.s()).isEqualTo((short) 1234);
        assertThat(rec.i()).isEqualTo(123456);
        assertThat(rec.l()).isEqualTo(9876543210L);
        assertThat(rec.f()).isEqualTo(1.23f);
        assertThat(rec.d()).isEqualTo(4.56789d);
        assertThat(rec.c()).isEqualTo('A');
        assertThat(rec.bigInt()).isEqualTo(new BigInteger("123456789012345678901234567890"));
        assertThat(rec.bigDec()).isEqualTo(new BigDecimal("123456789012345678901234567890.123456789"));
        assertThat(rec.status()).isEqualTo(Status.ACTIVE);
        assertThat(rec.optVal()).contains("Present");
        assertThat(rec.nullVal()).isNull();
    }

    @DisplayName("Duplicate Keys Rejection")
    @Test
    void testDuplicateKeysRejection() {
        String json = """
                {
                  "facilityID": "0603",
                  "facilityID": "0417"
                }
                """;

        assertThatThrownBy(() -> JSON.parse(VanLoadScan.class, json))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Duplicate JSON key \"facilityID\"");
    }

    @DisplayName("Trailing Content Rejection")
    @Test
    void testTrailingContentRejection() {
        String json = """
                { "facilityID": "0603", "scanType": "V3" } extra
                """;

        assertThatThrownBy(() -> JSON.parse(VanLoadScan.class, json))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Trailing non-whitespace content");
    }

    @DisplayName("Missing Property In Strict Parse Throws")
    @Test
    void testMissingPropertyInStrictParseThrows() {
        String json = """
                { "facilityID": "0603" }
                """;

        assertThatThrownBy(() -> JSON.parse(VanLoadScan.class, json))
                .isInstanceOf(MissingDataException.class)
                .hasMessageContaining("Missing JSON property \"scanType\"");
    }

    @DisplayName("Excess Property In Strict Parse Throws")
    @Test
    void testExcessPropertyInStrictParseThrows() {
        String json = """
                { "facilityID": "0603", "scanType": "V3", "extraProp": 123 }
                """;

        assertThatThrownBy(() -> JSON.parse(VanLoadScan.class, json))
                .isInstanceOf(ExcessDataException.class)
                .hasMessageContaining("Excess JSON property \"extraProp\"");
    }

    @DisplayName("Static Fields Ignored In Json")
    @Test
    void testStaticFieldsIgnoredInJson() {
        String json = """
                {
                  "name": "Test",
                  "version": "9.9"
                }
                """;

        // In strict mode, "version" is an excess property because static field is ignored!
        assertThatThrownBy(() -> JSON.parse(IgnoreStaticRecord.class, json))
                .isInstanceOf(ExcessDataException.class)
                .hasMessageContaining("Excess JSON property \"version\"");

        String validJson = "{ \"name\": \"Test\" }";
        IgnoreStaticRecord rec = JSON.parse(IgnoreStaticRecord.class, validJson);
        assertThat(rec.name()).isEqualTo("Test");
        assertThat(IgnoreStaticRecord.version()).isEqualTo("1.0");

        String stringified = JSON.stringify(rec);
        assertThat(stringified).isEqualTo("{\"name\":\"Test\"}");
    }

    @DisplayName("Special Character Escaping And Key Codec")
    @Test
    void testSpecialCharacterEscapingAndKeyCodec() {
        String json = """
                {
                  "Facility-ID": "F-100",
                  "Facility.ID": "F.200",
                  "display name": "Main Facility",
                  "price$value": "$50"
                }
                """;

        SpecialCharRecord rec = JSON.parse(SpecialCharRecord.class, json);
        assertThat(rec.Facility$hyphen$ID()).isEqualTo("F-100");
        assertThat(rec.Facility$dot$ID()).isEqualTo("F.200");
        assertThat(rec.display$space$name()).isEqualTo("Main Facility");
        assertThat(rec.price$dollar$value()).isEqualTo("$50");

        String outputJson = JSON.stringify(rec);
        assertThat(outputJson).contains("\"Facility-ID\":\"F-100\"");
        assertThat(outputJson).contains("\"Facility.ID\":\"F.200\"");
        assertThat(outputJson).contains("\"display name\":\"Main Facility\"");
        assertThat(outputJson).contains("\"price$value\":\"$50\"");
    }

    @DisplayName("Unicode Escapes And Surrogate Pairs")
    @Test
    void testUnicodeEscapesAndSurrogatePairs() {
        record TextRecord(String message) {}
        String json = "{ \"message\": \"Hello \\u0047\\u006f\\u006f\\u0064\\u0062\\u0079\\u0065 \\ud83d\\ude00\" }";

        TextRecord rec = JSON.parse(TextRecord.class, json);
        assertThat(rec.message()).isEqualTo("Hello Goodbye 😀");
    }

    @DisplayName("Partial Parsing Missing Defaults")
    @Test
    void testPartialParsingMissingDefaults() {
        String json = "{ \"id\": \"P100\" }";
        PartialJsonRecord rec = JSON.partialParse(PartialJsonRecord.class, json);

        assertThat(rec.id()).isEqualTo("P100");
        assertThat(rec.count()).isEqualTo(0);
        assertThat(rec.comment()).isEmpty();
        assertThat(rec.items()).isEmpty();
    }

    @DisplayName("Non Finite Number Rejection")
    @Test
    void testNonFiniteNumberRejection() {
        record DoubleRecord(double val) {}
        DoubleRecord rec = new DoubleRecord(Double.NaN);

        assertThatThrownBy(() -> JSON.stringify(rec))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Non-finite number value");
    }

    @DisplayName("Cycle Detection In Json Stringify")
    @Test
    void testCycleDetectionInJsonStringify() {
        record CycleNode(String name, List<Object> children) {}

        List<Object> children = new ArrayList<>();
        CycleNode parent = new CycleNode("Parent", children);
        children.add(parent); // Creates self cycle

        assertThatThrownBy(() -> JSON.stringify(parent))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("Cycle detected");
    }

    @DisplayName("Round Trip")
    @Test
    void testRoundTrip() {
        VanLoadScan scan = new VanLoadScan("0603", "V3");
        String json = JSON.stringify(scan);
        VanLoadScan roundTripped = JSON.parse(VanLoadScan.class, json);
        assertThat(roundTripped).isEqualTo(scan);
    }
}

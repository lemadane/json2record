package io.lemonade.json2record.naming;

import io.lemonade.json2record.NameEncodingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataNameCodecTest {

    @Test
    void testColonEncoding() {
        String encoded = DataNameCodec.encode("fxg:FacilityInboundEvent");
        assertThat(encoded).isEqualTo("fxg$FacilityInboundEvent");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("fxg:FacilityInboundEvent");
    }

    @Test
    void testXmlnsPrefixEncoding() {
        String encoded = DataNameCodec.encode("xmlns:fxg");
        assertThat(encoded).isEqualTo("xmlns$fxg");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("xmlns:fxg");
    }

    @Test
    void testHyphenEncoding() {
        String encoded = DataNameCodec.encode("Facility-ID");
        assertThat(encoded).isEqualTo("Facility$hyphen$ID");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("Facility-ID");
    }

    @Test
    void testDotEncoding() {
        String encoded = DataNameCodec.encode("Facility.ID");
        assertThat(encoded).isEqualTo("Facility$dot$ID");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("Facility.ID");
    }

    @Test
    void testSpaceEncoding() {
        String encoded = DataNameCodec.encode("display name");
        assertThat(encoded).isEqualTo("display$space$name");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("display name");
    }

    @Test
    void testDollarEncoding() {
        String encoded = DataNameCodec.encode("price$value");
        assertThat(encoded).isEqualTo("price$dollar$value");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("price$value");
    }

    @Test
    void testUnicodeEscapeEncoding() {
        String input = "emoji_😀_test";
        String encoded = DataNameCodec.encode(input);
        assertThat(encoded).contains("$u1F600$");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo(input);
    }

    @Test
    void testDigitAtStartEncoding() {
        String encoded = DataNameCodec.encode("0603");
        assertThat(encoded).startsWith("$u0030$");
        assertThat(DataNameCodec.decode(encoded)).isEqualTo("0603");
    }

    @Test
    void testEmptyString() {
        assertThat(DataNameCodec.encode("")).isEqualTo("");
        assertThat(DataNameCodec.decode("")).isEqualTo("");
    }

    @Test
    void testInvalidUnicodeEscapeThrows() {
        assertThatThrownBy(() -> DataNameCodec.decode("$uXYZ$"))
                .isInstanceOf(NameEncodingException.class);

        assertThatThrownBy(() -> DataNameCodec.decode("$u123"))
                .isInstanceOf(NameEncodingException.class);
    }

    @Test
    void testCollisionResistance() {
        String enc1 = DataNameCodec.encode("a-b");
        String enc2 = DataNameCodec.encode("a.b");
        String enc3 = DataNameCodec.encode("a:b");
        String enc4 = DataNameCodec.encode("a b");
        String enc5 = DataNameCodec.encode("a$b");

        assertThat(SetOf(enc1, enc2, enc3, enc4, enc5)).hasSize(5);
    }

    private static java.util.Set<String> SetOf(String... items) {
        return java.util.Set.of(items);
    }
}

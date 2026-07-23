package io.lemadane.json2record;

import io.lemadane.json2record.exceptions.DataMappingException;
import io.lemadane.json2record.exceptions.TypeConversionException;
import io.lemadane.json2record.exceptions.RecordConstructionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedAndEdgeCasesTest {

    public static class OrdinaryClass {
        private String name;
    }

    public record ValidRecord(String name) {}

    public record FailingConstructorRecord(String val) {
        public FailingConstructorRecord {
            if ("invalid".equals(val)) {
                throw new IllegalArgumentException("Validation failed in constructor");
            }
        }
    }

    @DisplayName("Xml Null Arguments Rejected")
    @Test
    void testXmlNullArgumentsRejected() {
        assertThatThrownBy(() -> XML.parse(null, "<xml/>"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> XML.parse(ValidRecord.class, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> XML.partialParse(null, "<xml/>"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> XML.partialParse(ValidRecord.class, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> XML.stringify(null))
                .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("Json Null Arguments Rejected")
    @Test
    void testJsonNullArgumentsRejected() {
        assertThatThrownBy(() -> JSON.parse(null, "{}"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> JSON.parse(ValidRecord.class, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> JSON.partialParse(null, "{}"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> JSON.partialParse(ValidRecord.class, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> JSON.stringify(null))
                .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("Non Record Target Class Rejected")
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testNonRecordTargetClassRejected() {
        Class clazz = OrdinaryClass.class;

        assertThatThrownBy(() -> XML.parse(clazz, "<OrdinaryClass/>"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("Target type is not a Java record class");

        assertThatThrownBy(() -> XML.partialParse(clazz, "<OrdinaryClass/>"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("Target type is not a Java record class");

        assertThatThrownBy(() -> JSON.parse(clazz, "{}"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("Target type is not a Java record class");

        assertThatThrownBy(() -> JSON.partialParse(clazz, "{}"))
                .isInstanceOf(DataMappingException.class)
                .hasMessageContaining("Target type is not a Java record class");
    }

    @DisplayName("Record Constructor Failure Reporting")
    @Test
    void testRecordConstructorFailureReporting() {
        String xml = "<FailingConstructorRecord><val>invalid</val></FailingConstructorRecord>";
        assertThatThrownBy(() -> XML.parse(FailingConstructorRecord.class, xml))
                .isInstanceOf(RecordConstructionException.class)
                .hasMessageContaining("Validation failed in constructor");

        String json = "{\"val\":\"invalid\"}";
        assertThatThrownBy(() -> JSON.parse(FailingConstructorRecord.class, json))
                .isInstanceOf(RecordConstructionException.class)
                .hasMessageContaining("Validation failed in constructor");
    }

    @DisplayName("Concurrent Metadata Caching")
    @Test
    void testConcurrentMetadataCaching() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        ValidRecord rec = JSON.parse(ValidRecord.class, "{\"name\":\"test\"}");
                        assertThat(rec.name()).isEqualTo("test");
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threads);
    }

    @DisplayName("Exception Path Rich Messages")
    @Test
    void testExceptionPathRichMessages() {
        record Sub(int count) {}
        record Root(Sub sub) {}

        String xml = "<Root><sub><count>invalid_int</count></sub></Root>";
        assertThatThrownBy(() -> XML.parse(Root.class, xml))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageContaining("/Root/sub/count");

        String json = "{\"sub\":{\"count\":\"invalid_int\"}}";
        assertThatThrownBy(() -> JSON.parse(Root.class, json))
                .isInstanceOf(TypeConversionException.class)
                .hasMessageContaining("$.sub.count");
    }
}

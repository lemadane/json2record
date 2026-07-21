package io.lemonade.json2record.json;

import io.lemonade.json2record.RecordConstructionException;
import io.lemonade.json2record.json.internal.JsonRecordReader;
import io.lemonade.json2record.json.internal.JsonRecordWriter;

import java.util.Objects;

/**
 * Utility class for parsing JSON documents into Java records and stringifying Java records into JSON documents.
 */
public final class JSON {

    private JSON() {
    }

    /**
     * Performs a strict, complete mapping of JSON object properties to Java record components.
     *
     * @param recordType the target record class.
     * @param json the JSON input string.
     * @param <T> the record type.
     * @return the parsed record instance.
     * @throws NullPointerException if any argument is null.
     * @throws RecordConstructionException if recordType is not a Java record class.
     * @throws io.lemonade.json2record.JsonMappingException for structural, conversion, or mapping errors.
     */
    public static <T extends Record> T parse(Class<T> recordType, String json) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        Objects.requireNonNull(json, "json must not be null");
        if (!recordType.isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + recordType.getName());
        }
        return JsonRecordReader.parse(recordType, json, true);
    }

    /**
     * Performs a selective JSON mapping into a target Java record, ignoring undeclared JSON properties.
     *
     * @param recordType the target record class.
     * @param json the JSON input string.
     * @param <T> the record type.
     * @return the parsed record instance.
     * @throws NullPointerException if any argument is null.
     * @throws RecordConstructionException if recordType is not a Java record class.
     * @throws io.lemonade.json2record.JsonMappingException for conversion or mapping errors.
     */
    public static <T extends Record> T partialParse(Class<T> recordType, String json) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        Objects.requireNonNull(json, "json must not be null");
        if (!recordType.isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + recordType.getName());
        }
        return JsonRecordReader.parse(recordType, json, false);
    }

    /**
     * Converts a record hierarchy into a compact JSON string.
     *
     * @param jsonRecord the record hierarchy to convert.
     * @return compact JSON representation.
     * @throws NullPointerException if jsonRecord is null.
     * @throws io.lemonade.json2record.JsonMappingException if stringification fails.
     */
    public static String stringify(Record jsonRecord) {
        Objects.requireNonNull(jsonRecord, "jsonRecord must not be null");
        if (!jsonRecord.getClass().isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + jsonRecord.getClass().getName());
        }
        return JsonRecordWriter.stringify(jsonRecord);
    }
}

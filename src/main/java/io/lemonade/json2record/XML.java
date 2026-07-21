package io.lemonade.json2record;

import io.lemonade.json2record.exceptions.XmlMappingException;

import io.lemonade.json2record.exceptions.RecordConstructionException;
import io.lemonade.json2record.xml.internal.XmlRecordReader;
import io.lemonade.json2record.xml.internal.XmlRecordWriter;

import java.util.Objects;

/**
 * Utility class for parsing XML documents into Java records and stringifying Java records into XML documents.
 */
public final class XML {

    private XML() {
    }

    /**
     * Performs a strict, complete mapping of XML child elements to Java record components.
     *
     * @param recordType the target record class.
     * @param xml the XML input string.
     * @param <T> the record type.
     * @return the parsed record instance.
     * @throws NullPointerException if any argument is null.
     * @throws RecordConstructionException if recordType is not a Java record class.
     * @throws io.lemonade.json2record.exceptions.XmlMappingException for structural, conversion, or mapping errors.
     */
    public static <T extends Record> T parse(Class<T> recordType, String xml) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        Objects.requireNonNull(xml, "xml must not be null");
        if (!recordType.isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + recordType.getName());
        }
        return XmlRecordReader.parse(recordType, xml, true);
    }

    /**
     * Performs a selective XML mapping into a target Java record, ignoring undeclared XML child elements.
     *
     * @param recordType the target record class.
     * @param xml the XML input string.
     * @param <T> the record type.
     * @return the parsed record instance.
     * @throws NullPointerException if any argument is null.
     * @throws RecordConstructionException if recordType is not a Java record class.
     * @throws io.lemonade.json2record.exceptions.XmlMappingException for conversion or mapping errors.
     */
    public static <T extends Record> T partialParse(Class<T> recordType, String xml) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        Objects.requireNonNull(xml, "xml must not be null");
        if (!recordType.isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + recordType.getName());
        }
        return XmlRecordReader.parse(recordType, xml, false);
    }

    /**
     * Converts a record hierarchy into a well-formed compact XML document string.
     *
     * @param xmlRecord the record hierarchy to convert.
     * @return compact XML representation.
     * @throws NullPointerException if xmlRecord is null.
     * @throws io.lemonade.json2record.exceptions.XmlMappingException if stringification fails.
     */
    public static String stringify(Record xmlRecord) {
        Objects.requireNonNull(xmlRecord, "xmlRecord must not be null");
        if (!xmlRecord.getClass().isRecord()) {
            throw new RecordConstructionException("Target type is not a Java record class: " + xmlRecord.getClass().getName());
        }
        return XmlRecordWriter.stringify(xmlRecord);
    }
}

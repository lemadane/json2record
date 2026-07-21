package io.lemonade.json2record.xml.internal;

import io.lemonade.json2record.exceptions.XmlMappingException;
import io.lemonade.json2record.convert.TypeConverter;
import io.lemonade.json2record.naming.DataNameCodec;
import io.lemonade.json2record.reflect.RecordIntrospector;
import io.lemonade.json2record.reflect.RecordMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal XML writer that converts Java records into XML strings.
 */
public final class XmlRecordWriter {

    private XmlRecordWriter() {
    }

    /**
     * Stringifies a record into an XML string.
     *
     * @param record the record instance.
     * @return compact XML string.
     */
    public static String stringify(Record record) {
        Objects.requireNonNull(record, "xmlRecord must not be null");
        RecordMetadata metadata = RecordIntrospector.inspect(record.getClass());
        StringBuilder sb = new StringBuilder();
        writeRecordElement(metadata, record, metadata.decodedRecordName(), sb);
        return sb.toString();
    }

    private static void writeRecordElement(RecordMetadata metadata, Record record, String elementName, StringBuilder sb) {
        sb.append('<').append(elementName);

        // Append static fields as XML attributes
        for (Map.Entry<String, Field> entry : metadata.staticFieldsByDecodedName().entrySet()) {
            Field field = entry.getValue();
            if (Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                try {
                    Object val = field.get(null);
                    if (val != null) {
                        String attrName = DataNameCodec.decode(field.getName());
                        sb.append(' ').append(attrName).append("=\"").append(escapeAttribute(val.toString())).append('"');
                    }
                } catch (IllegalAccessException e) {
                    throw new XmlMappingException("Failed to access static field " + field.getName() + " on " + metadata.recordType().getName(), e);
                }
            }
        }

        List<RecordMetadata.ComponentMetadata> components = metadata.components();
        if (components.isEmpty()) {
            sb.append("/>");
            return;
        }

        sb.append('>');

        for (RecordMetadata.ComponentMetadata comp : components) {
            String childElementName = comp.decodedName();
            Method accessor = comp.accessor();
            Object value;
            try {
                value = accessor.invoke(record);
            } catch (Exception e) {
                throw new XmlMappingException("Failed to access record component " + comp.name() + " on " + metadata.recordType().getName(), e);
            }

            if (value == null) {
                continue;
            }

            if (comp.isOptional()) {
                Optional<?> opt = (Optional<?>) value;
                if (opt.isPresent()) {
                    writeSingleComponent(comp.optionalValueType(), comp.genericType(), childElementName, opt.get(), sb);
                }
            } else if (comp.isList()) {
                Collection<?> list = (Collection<?>) value;
                for (Object item : list) {
                    if (item != null) {
                        writeSingleComponent(comp.listElementType(), comp.genericType(), childElementName, item, sb);
                    }
                }
            } else {
                writeSingleComponent(comp.type(), comp.genericType(), childElementName, value, sb);
            }
        }

        sb.append("</").append(elementName).append('>');
    }

    private static void writeSingleComponent(Class<?> type, java.lang.reflect.Type genericType, String elementName, Object value, StringBuilder sb) {
        if (value instanceof Record childRecord) {
            RecordMetadata childMeta = RecordIntrospector.inspect(childRecord.getClass());
            writeRecordElement(childMeta, childRecord, elementName, sb);
        } else {
            sb.append('<').append(elementName).append('>');
            sb.append(escapeText(value.toString()));
            sb.append("</").append(elementName).append('>');
        }
    }

    private static String escapeText(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeAttribute(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("\"", "&quot;");
    }
}

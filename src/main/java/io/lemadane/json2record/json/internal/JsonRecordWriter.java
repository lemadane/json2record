package io.lemadane.json2record.json.internal;

import io.lemadane.json2record.exceptions.JsonMappingException;
import io.lemadane.json2record.naming.DataNameCodec;
import io.lemadane.json2record.reflect.RecordIntrospector;
import io.lemadane.json2record.reflect.RecordMetadata;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Internal JSON writer that converts Java records into compact JSON strings.
 */
public final class JsonRecordWriter {

    private JsonRecordWriter() {
    }

    /**
     * Stringifies a record into a compact JSON string.
     *
     * @param record the record instance.
     * @return compact JSON string.
     */
    public static String stringify(Record record) {
        Objects.requireNonNull(record, "jsonRecord must not be null");
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        StringBuilder sb = new StringBuilder();
        writeRecord(record, visited, sb);
        return sb.toString();
    }

    private static void writeRecord(Record record, Set<Object> visited, StringBuilder sb) {
        if (!visited.add(record)) {
            throw new JsonMappingException("Cycle detected in object graph for record instance of " + record.getClass().getName());
        }

        try {
            RecordMetadata metadata = RecordIntrospector.inspect(record.getClass());
            sb.append('{');
            List<RecordMetadata.ComponentMetadata> components = metadata.components();
            boolean first = true;

            for (RecordMetadata.ComponentMetadata comp : components) {
                Method accessor = comp.accessor();
                Object val;
                try {
                    val = accessor.invoke(record);
                } catch (Exception e) {
                    throw new JsonMappingException("Failed to access record component " + comp.name() + " on " + metadata.recordType().getName(), e);
                }

                if (!first) {
                    sb.append(',');
                }
                first = false;

                String propName = comp.decodedName();
                sb.append('"').append(escapeString(propName)).append("\":");

                writeValue(val, visited, sb);
            }

            sb.append('}');
        } finally {
            visited.remove(record);
        }
    }

    private static void writeValue(Object val, Set<Object> visited, StringBuilder sb) {
        if (val == null) {
            sb.append("null");
            return;
        }

        if (val instanceof Optional<?> opt) {
            if (opt.isPresent()) {
                writeValue(opt.get(), visited, sb);
            } else {
                sb.append("null");
            }
            return;
        }

        if (val instanceof Record childRecord) {
            writeRecord(childRecord, visited, sb);
            return;
        }

        if (val instanceof Collection<?> collection) {
            if (!visited.add(val)) {
                throw new JsonMappingException("Cycle detected in object graph for collection instance");
            }
            try {
                sb.append('[');
                boolean first = true;
                for (Object item : collection) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    writeValue(item, visited, sb);
                }
                sb.append(']');
            } finally {
                visited.remove(val);
            }
            return;
        }

        if (val instanceof String || val instanceof Character) {
            sb.append('"').append(escapeString(val.toString())).append('"');
            return;
        }

        if (val instanceof Boolean) {
            sb.append(val);
            return;
        }

        if (val instanceof Number num) {
            if (num instanceof Float f) {
                if (f.isNaN() || f.isInfinite()) {
                    throw new JsonMappingException("Non-finite number value " + f + " is not supported in JSON");
                }
            } else if (num instanceof Double d) {
                if (d.isNaN() || d.isInfinite()) {
                    throw new JsonMappingException("Non-finite number value " + d + " is not supported in JSON");
                }
            }
            sb.append(num.toString());
            return;
        }

        if (val instanceof Enum<?> e) {
            sb.append('"').append(escapeString(e.name())).append('"');
            return;
        }

        sb.append('"').append(escapeString(val.toString())).append('"');
    }

    private static String escapeString(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u").append("0".repeat(4 - hex.length())).append(hex);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}

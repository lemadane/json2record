package io.lemonade.json2record.reflect;

import io.lemonade.json2record.DataMappingException;
import io.lemonade.json2record.RecordConstructionException;
import io.lemonade.json2record.naming.DataNameCodec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe introspector and metadata cache for Java record classes.
 */
public final class RecordIntrospector {

    private static final Map<Class<?>, RecordMetadata> CACHE = new ConcurrentHashMap<>();

    private RecordIntrospector() {
    }

    /**
     * Inspects a target record class and returns its metadata.
     *
     * @param recordType the class to inspect.
     * @return the record metadata.
     * @throws DataMappingException if the class is not a record or cannot be inspected.
     */
    public static RecordMetadata inspect(Class<?> recordType) {
        Objects.requireNonNull(recordType, "recordType must not be null");
        if (!recordType.isRecord()) {
            throw new RecordConstructionException("Target class is not a Java record: " + recordType.getName());
        }
        return CACHE.computeIfAbsent(recordType, RecordIntrospector::buildMetadata);
    }

    private static RecordMetadata buildMetadata(Class<?> clazz) {
        RecordComponent[] recordComponents = clazz.getRecordComponents();
        if (recordComponents == null) {
            recordComponents = new RecordComponent[0];
        }

        List<RecordMetadata.ComponentMetadata> components = new ArrayList<>(recordComponents.length);
        Map<String, RecordMetadata.ComponentMetadata> componentsByDecodedName = new LinkedHashMap<>();
        Class<?>[] paramTypes = new Class<?>[recordComponents.length];

        for (int i = 0; i < recordComponents.length; i++) {
            RecordComponent rc = recordComponents[i];
            String rawName = rc.getName();
            String decodedName = DataNameCodec.decode(rawName);
            Class<?> type = rc.getType();
            paramTypes[i] = type;

            Method accessor = rc.getAccessor();
            accessor.trySetAccessible();

            RecordMetadata.ComponentMetadata meta = new RecordMetadata.ComponentMetadata(
                    rawName,
                    decodedName,
                    type,
                    rc.getGenericType(),
                    accessor
            );
            components.add(meta);
            componentsByDecodedName.put(decodedName, meta);
            // Also map by raw component name if different
            if (!rawName.equals(decodedName)) {
                componentsByDecodedName.putIfAbsent(rawName, meta);
            }
        }

        Constructor<?> canonicalConstructor;
        try {
            canonicalConstructor = clazz.getDeclaredConstructor(paramTypes);
            canonicalConstructor.trySetAccessible();
        } catch (NoSuchMethodException e) {
            throw new RecordConstructionException("Could not find canonical constructor for record: " + clazz.getName(), e);
        }

        // Locate static fields declared directly on the record (for XML attribute mapping)
        Map<String, Field> staticFieldsByDecodedName = new LinkedHashMap<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field f : declaredFields) {
            if (Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                String rawFieldName = f.getName();
                String decodedFieldName = DataNameCodec.decode(rawFieldName);
                f.trySetAccessible();
                staticFieldsByDecodedName.put(decodedFieldName, f);
                if (!rawFieldName.equals(decodedFieldName)) {
                    staticFieldsByDecodedName.putIfAbsent(rawFieldName, f);
                }
            }
        }

        String simpleName = clazz.getSimpleName();
        String decodedSimpleName = DataNameCodec.decode(simpleName);

        return new RecordMetadata(
                clazz,
                simpleName,
                decodedSimpleName,
                canonicalConstructor,
                components,
                componentsByDecodedName,
                staticFieldsByDecodedName
        );
    }
}

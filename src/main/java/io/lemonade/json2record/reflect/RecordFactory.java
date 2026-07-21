package io.lemonade.json2record.reflect;

import io.lemonade.json2record.exceptions.RecordConstructionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory for reflective record instantiation via canonical constructor.
 */
public final class RecordFactory {

    private RecordFactory() {
    }

    /**
     * Instantiates a record using its metadata and canonical constructor arguments.
     *
     * @param metadata the record metadata.
     * @param args the constructor arguments in exact record component order.
     * @param <T> the record type.
     * @return the instantiated record.
     * @throws RecordConstructionException if instantiation fails.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Record> T createRecord(RecordMetadata metadata, Object[] args) {
        Constructor<?> constructor = metadata.canonicalConstructor();
        try {
            return (T) constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RecordConstructionException(
                    "Canonical constructor for " + metadata.recordType().getName() + " threw an exception: " + cause.getMessage(),
                    cause
            );
        } catch (Exception e) {
            throw new RecordConstructionException(
                    "Failed to instantiate record " + metadata.recordType().getName() + ": " + e.getMessage(),
                    e
            );
        }
    }
}

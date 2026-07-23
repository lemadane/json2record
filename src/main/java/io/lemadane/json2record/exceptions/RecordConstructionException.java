package io.lemadane.json2record.exceptions;

/**
 * Exception thrown when reflective record instantiation fails.
 */
public class RecordConstructionException extends DataMappingException {

    /**
     * Constructs a new RecordConstructionException with the specified detail message.
     *
     * @param message the detail message.
     */
    public RecordConstructionException(String message) {
        super(message);
    }

    /**
     * Constructs a new RecordConstructionException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public RecordConstructionException(String message, Throwable cause) {
        super(message, cause);
    }
}

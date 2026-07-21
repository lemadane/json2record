package io.lemonade.json2record;

/**
 * Base unchecked exception thrown for any data mapping or conversion failure.
 */
public class DataMappingException extends RuntimeException {

    /**
     * Constructs a new DataMappingException with the specified detail message.
     *
     * @param message the detail message explaining the mapping error.
     */
    public DataMappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataMappingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the mapping error.
     * @param cause the underlying cause of the failure.
     */
    public DataMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}

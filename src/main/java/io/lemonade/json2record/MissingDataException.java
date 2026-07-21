package io.lemonade.json2record;

/**
 * Exception thrown when a required record component has no matching XML element or JSON property in strict mode.
 */
public class MissingDataException extends DataMappingException {

    /**
     * Constructs a new MissingDataException with the specified detail message.
     *
     * @param message the detail message.
     */
    public MissingDataException(String message) {
        super(message);
    }

    /**
     * Constructs a new MissingDataException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public MissingDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

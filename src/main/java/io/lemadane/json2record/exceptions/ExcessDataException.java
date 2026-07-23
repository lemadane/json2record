package io.lemadane.json2record.exceptions;

/**
 * Exception thrown when an undeclared XML element or JSON property is encountered in strict mode.
 */
public class ExcessDataException extends DataMappingException {

    /**
     * Constructs a new ExcessDataException with the specified detail message.
     *
     * @param message the detail message.
     */
    public ExcessDataException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExcessDataException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ExcessDataException(String message, Throwable cause) {
        super(message, cause);
    }
}

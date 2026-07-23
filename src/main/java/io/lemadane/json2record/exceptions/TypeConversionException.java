package io.lemadane.json2record.exceptions;

/**
 * Exception thrown when a value cannot be converted to the target Java record component type.
 */
public class TypeConversionException extends DataMappingException {

    /**
     * Constructs a new TypeConversionException with the specified detail message.
     *
     * @param message the detail message.
     */
    public TypeConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new TypeConversionException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}

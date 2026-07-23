package io.lemadane.json2record.exceptions;

/**
 * Exception thrown for XML specific parsing or stringification mapping errors.
 */
public class XmlMappingException extends DataMappingException {

    /**
     * Constructs a new XmlMappingException with the specified detail message.
     *
     * @param message the detail message.
     */
    public XmlMappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new XmlMappingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public XmlMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}

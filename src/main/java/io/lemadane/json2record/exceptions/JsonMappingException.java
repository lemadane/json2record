package io.lemadane.json2record.exceptions;

/**
 * Exception thrown for JSON specific parsing or stringification mapping errors.
 */
public class JsonMappingException extends DataMappingException {

    /**
     * Constructs a new JsonMappingException with the specified detail message.
     *
     * @param message the detail message.
     */
    public JsonMappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonMappingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}

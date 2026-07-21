package io.lemonade.json2record;

/**
 * Exception thrown when identifier encoding or decoding fails.
 */
public class NameEncodingException extends DataMappingException {

    /**
     * Constructs a new NameEncodingException with the specified detail message.
     *
     * @param message the detail message.
     */
    public NameEncodingException(String message) {
        super(message);
    }

    /**
     * Constructs a new NameEncodingException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public NameEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
